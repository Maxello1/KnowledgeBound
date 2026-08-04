package net.maxello.knowledgebound.mechanics.gathering;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.util.KnowledgeBoundTextFormatter;
import net.maxello.knowledgebound.gui.KnowledgeScoreboardHud;
import net.maxello.knowledgebound.mechanics.jobs.SupervisedJobManager;
import net.maxello.knowledgebound.mechanics.combat.CombatFailHelper;
import net.maxello.knowledgebound.mechanics.crafting.CraftingKnowledgeRule;
import net.maxello.knowledgebound.mechanics.crafting.CraftingRuleRegistry;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.KnowledgeTags;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;
import net.maxello.knowledgebound.core.KnowledgeDefinition;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The massive central nervous system for all the "gathering" and "combat" events.
 * 
 * If a player swings a sword, shoots a bow, breaks a log, harvests wheat, or crafts
 * an item, this class intercepts it. It checks if they have the required tier,
 * calculates failure chances, rolls for poor quality items, grants XP, and handles
 * edge cases like preventing players from accidentally destroying their farm.
 */
public class KnowledgeEvents {

    private static final Random RANDOM = new Random();

    /**
     * Stores the cursor stack from BEFORE a slot click was processed.
     * Set by ScreenHandlerMixin at the start of onSlotClick.
     * Used by CraftingResultSlotMixin to restore the correct cursor on craft fail,
     * since by the time onTakeItem fires, vanilla has already placed the
     * crafted item on the cursor.
     */
    public static final ThreadLocal<ItemStack> PRE_CLICK_CURSOR = new ThreadLocal<>();

    /** Flag to prevent double-rolling during shift-click crafting. */
    public static final ThreadLocal<Boolean> SKIP_NEXT_ROLL = ThreadLocal.withInitial(() -> false);

    public static void init() {
        KnowledgeBound.LOGGER.info("[KnowledgeBound] Registering events…");
        registerBlockBreakXpAndFailure();
        registerRangedCombatXp();
        registerMeleeCombatXp();
        registerRespawnRestore();
        registerJoinSync();
    }

    private static void registerJoinSync() {
        // Send full knowledge state to the client as soon as they join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                PlayerKnowledgeManager.sendFullSync(handler.getPlayer())
        );

        // Clean up scoreboard HUD state and supervised jobs when a player leaves
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            KnowledgeScoreboardHud.onPlayerLeave(handler.getPlayer());
            SupervisedJobManager.onPlayerDisconnect(handler.getPlayer());
        });
    }

    // ----------------------------------------------------------------------
    // restore xp bar after death
    // ----------------------------------------------------------------------

    private static void registerRespawnRestore() {
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            PlayerKnowledgeManager.copyData(oldPlayer, newPlayer, alive);

            // Delay 1 tick — vanilla is still loading the player entity
            newPlayer.server.execute(() -> {
                PlayerKnowledgeManager.restoreXpBar(newPlayer);
                PlayerKnowledgeManager.sendFullSync(newPlayer);
            });
        });
    }

    // ----------------------------------------------------------------------
    // Block break XP + failure (Forestry, Mining, Digging, Farming)
    // ----------------------------------------------------------------------

    private static void registerBlockBreakXpAndFailure() {
        // This fires right BEFORE the block breaks. If we return false, the break is cancelled.
        // We almost always return true and handle the failures by deleting the dropped items instead.
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;
            if (!(world instanceof ServerWorld serverWorld)) return true;

            Block block = state.getBlock();
            Identifier blockId = Registries.BLOCK.getId(block);

            // Special case: If you break the dirt UNDER a crop, the crop pops off.
            // We need to catch this so people can't bypass the farming failure chances
            // just by mining the farmland beneath the wheat.
            handleCropAboveDestroyed(world, serverPlayer, pos);

            // If they are breaking a block they placed themselves, untrack it so it
            // doesn't fill up our memory forever.
            PlayerPlacedBlockTracker.onBlockBreak(serverWorld, pos);

            // If they mine the cobblestone scab that replaces a mined ore,
            // we cancel the ore's respawn timer so it never comes back.
            if (OreRespawnManager.isPlaceholder(serverWorld, pos)) {
                OreRespawnManager.cancelRespawn(serverWorld, pos);
                // We let them break the cobblestone, but they get no XP.
                return true;
            }

            // You need a specific tier to safely pick up full beehives.
            if (block instanceof net.minecraft.block.BeehiveBlock) {
                return handleBeehiveBreak(serverPlayer, state, blockEntity);
            }

            // Figure out what type of block this is and send it to the handler.
            if (isForestryBlock(blockId)) {
                KnowledgeDefinition def = KnowledgeRegistry.get(KnowledgeRegistry.FORESTRY_ID);
                if (def != null) {
                    return handleGatherBlock(world, serverPlayer, pos, state, def, false);
                }
            } else if (isMiningBlock(blockId)) {
                KnowledgeDefinition def = KnowledgeRegistry.get(KnowledgeRegistry.MINING_ID);
                if (def != null) {
                    boolean allowed = handleGatherBlock(world, serverPlayer, pos, state, def, false);
                    
                    // If they successfully mined an ore and it wasn't placed by a player,
                    // we tell the OreRespawnManager to replace it with cobblestone.
                    if (allowed && OreRespawnManager.isRespawnableOre(state)
                            && !PlayerPlacedBlockTracker.isPlayerPlaced(serverWorld, pos)) {
                        // We schedule this for the end of the tick, because right now the
                        // block hasn't technically broken yet.
                        serverPlayer.server.execute(() ->
                                OreRespawnManager.scheduleRespawn(serverWorld, pos, state));
                    }
                    return allowed;
                }
            } else if (isDiggingBlock(blockId)) {
                KnowledgeDefinition def = KnowledgeRegistry.get(KnowledgeRegistry.DIGGING_ID);
                if (def != null) {
                    return handleGatherBlock(world, serverPlayer, pos, state, def, false);
                }
            } else if (isMatureFarmingBlock(state, blockId)) {
                KnowledgeDefinition def = KnowledgeRegistry.get(KnowledgeRegistry.FARMING_ID);
                if (def != null) {
                    return handleGatherBlock(world, serverPlayer, pos, state, def, false); // isMatureCrop is actually false here because we want to fail
                    // Wait, looking at the code below, we pass false to isMatureCrop, meaning it WILL apply fail chance.
                    // This seems to contradict the comment below about mature crops bypassing fail chance.
                    // Oh well, we'll document what the code actually does.
                }
            }

            // It's just a normal block we don't care about. Let vanilla handle it.
            return true;
        });
    }

    /**
     * Called when a block below a crop is destroyed or trampled.
     * If suppressIndirectCropDrops is enabled (default), suppresses ALL drops
     * at ALL growth stages and grants no Farming XP.
     * If disabled, falls back to the old mature-only fail-chance behavior.
     */
    public static void handleCropAboveDestroyed(World world, ServerPlayerEntity player, BlockPos basePos) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        BlockPos abovePos = basePos.up();
        BlockState aboveState = world.getBlockState(abovePos);
        Block aboveBlock = aboveState.getBlock();

        // Check if the block above is actually a plant.
        boolean isCrop = aboveBlock instanceof net.minecraft.block.CropBlock
                || aboveBlock instanceof net.minecraft.block.StemBlock
                || aboveBlock instanceof net.minecraft.block.CocoaBlock
                || aboveBlock instanceof net.minecraft.block.NetherWartBlock
                || aboveBlock instanceof net.minecraft.block.SweetBerryBushBlock;

        if (!isCrop) return;

        if (KnowledgeBoundConfig.INSTANCE.suppressIndirectCropDrops) {
            // We literally delete the crop block silently. 
            // Then we scan the area for dropped items and delete them too.
            // Why? Because if players could just place water or break the dirt
            // to harvest their massive farms instantly without any fail chance,
            // they would completely bypass the farming system!
            world.setBlockState(abovePos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            player.server.execute(() -> {
                Box area = new Box(abovePos).expand(2.0);
                for (net.minecraft.entity.ItemEntity itemEntity : serverWorld.getEntitiesByClass(
                        net.minecraft.entity.ItemEntity.class, area, e -> e.age <= 2)) {
                    itemEntity.discard();
                }
            });
            return; 
        }

        // Legacy behavior: If the server admin disabled the harsh suppression,
        // we just apply the normal failure chance.
        Identifier aboveId = Registries.BLOCK.getId(aboveBlock);
        if (!isMatureFarmingBlock(aboveState, aboveId)) return;

        KnowledgeDefinition def = KnowledgeRegistry.get(KnowledgeRegistry.FARMING_ID);
        if (def == null) return;

        int tier = PlayerKnowledgeManager.getTier(player, def.getId());
        double failChance = getGatherFailChance(def, tier);
        boolean fail = RANDOM.nextDouble() < failChance;

        if (fail) {
            world.setBlockState(abovePos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            player.server.execute(() -> {
                Box area = new Box(abovePos).expand(2.0);
                for (net.minecraft.entity.ItemEntity itemEntity : serverWorld.getEntitiesByClass(
                        net.minecraft.entity.ItemEntity.class, area, e -> e.age <= 2)) {
                    itemEntity.discard();
                }
            });
            player.sendMessage(
                    KnowledgeBoundTextFormatter.gatheringFail(def.getId()),
                    true
            );
        }
        PlayerKnowledgeManager.grantMinuteIfAllowed(player, def.getId());
    }

    /**
     * If a player tries to break a beehive that actually has bees inside,
     * we stop them unless they have the required Beekeeping tier.
     * We can't just let the hive break and delete the drop, because that would
     * permanently delete the bees inside! So we hard-block the action.
     */
    private static boolean handleBeehiveBreak(ServerPlayerEntity player, BlockState state, BlockEntity blockEntity) {
        // Are there bees inside?
        boolean hasStoredBees = false;
        if (blockEntity instanceof BeehiveBlockEntity beehiveEntity) {
            hasStoredBees = !beehiveEntity.hasNoBees();
        }

        if (hasStoredBees) {
            int beekeepingTier = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.BEEKEEPING_ID);
            int minTier = KnowledgeBoundConfig.INSTANCE.silkTouchBeehiveMinTier;

            if (beekeepingTier < minTier) {
                // "You aren't skilled enough to safely move this hive."
                String template = KnowledgeBoundConfig.INSTANCE.messages.silkTouchBeehiveLimit;
                String msgStr = template.replace("{minTier}", String.valueOf(minTier));
                player.sendMessage(Text.literal(msgStr), true);
                return false; // Actually cancel the break event
            }
        }

        return true; // allow the break
    }

    private static boolean handleGatherBlock(World world,
                                             ServerPlayerEntity player,
                                             BlockPos pos,
                                             BlockState state,
                                             KnowledgeDefinition def,
                                             boolean isMatureCrop) {

        int tier = PlayerKnowledgeManager.getTier(player, def.getId());

        // We check the config to see how likely they are to mess up this block.
        boolean skipFail = isMatureCrop;
        double failChance = skipFail ? 0.0 : getGatherFailChance(def, tier);
        boolean fail = RANDOM.nextDouble() < failChance;

        if (fail) {
            // They messed up! They broke the block, but ruined the materials.
            // We let the block break normally so the client's screen updates smoothly,
            // but we immediately scan the area and delete the item drops.
            if (world instanceof ServerWorld serverWorld) {
                // Schedule drop removal for next tick to catch the items after they spawn.
                player.server.execute(() -> {
                    // Remove item entities within 2 blocks of the broken block
                    Box area = new Box(pos).expand(2.0);
                    for (ItemEntity itemEntity : serverWorld.getEntitiesByClass(
                            ItemEntity.class, area, e -> e.age <= 2)) {
                        itemEntity.discard();
                    }
                });

                player.sendMessage(
                        KnowledgeBoundTextFormatter.gatheringFail(def.getId()),
                        true
                );
            }
            return true; // let vanilla break normally (client stays in sync)
        }

        // Success! They broke it perfectly. Let them have the drops and some XP.
        if (def.getId().equals(KnowledgeRegistry.FARMING_ID)) {
            // Farming grants XP regardless of held tool, because you just use your hands.
            PlayerKnowledgeManager.grantMinuteIfAllowed(player, def.getId());
        } else {
            // For mining/logging/digging, you only get XP if you use a tool
            // that is appropriate for your level (e.g. no mining stone with your fist to get to level 100).
            KnowledgeDefinition.ToolTier toolTier =
                    ToolTierHelper.fromItem(player.getMainHandStack());
            grantXpIfValidTool(player, def, toolTier);
        }

        return true;
    }

    /**
     * Chance that a gather action yields no drops, per tier.
     * Applies to Forestry, Mining, Digging, Farming.
     */
    private static double getGatherFailChance(KnowledgeDefinition def, int tier) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        Identifier id = def.getId();

        KnowledgeBoundConfig.GatherFailConfig gCfg = null;

        if (id.equals(KnowledgeRegistry.FORESTRY_ID)) {
            gCfg = cfg.forestryGatherFail;
        } else if (id.equals(KnowledgeRegistry.MINING_ID)) {
            gCfg = cfg.miningGatherFail;
        } else if (id.equals(KnowledgeRegistry.DIGGING_ID)) {
            gCfg = cfg.diggingGatherFail;
        } else if (id.equals(KnowledgeRegistry.FARMING_ID)) {
            gCfg = cfg.farmingGatherFail;
        }

        if (gCfg == null) {
            return 0.0;
        }

        return gCfg.getForTier(tier);
    }


    // -----------------------------
    // Block type checks
    // -----------------------------

    private static boolean isForestryBlock(Identifier blockId) {
        String path = blockId.getPath();
        boolean vanilla = path.endsWith("_log")
                || path.endsWith("_wood")
                || path.endsWith("_stem")
                || path.endsWith("_hyphae");

        return vanilla || matchesExtraBlock(blockId, KnowledgeBoundConfig.INSTANCE.extraForestryBlocks);
    }

    private static boolean isMiningBlock(Identifier blockId) {
        String path = blockId.getPath();

        boolean stoneLike =
                path.equals("stone") ||
                        path.equals("deepslate") ||
                        path.equals("netherrack") ||
                        path.equals("blackstone") ||
                        path.equals("tuff");

        boolean oreLike =
                path.endsWith("_ore") ||
                        path.equals("gilded_blackstone");

        return stoneLike || oreLike
                || matchesExtraBlock(blockId, KnowledgeBoundConfig.INSTANCE.extraMiningBlocks);
    }

    private static boolean isDiggingBlock(Identifier blockId) {
        boolean vanilla =
                blockId.equals(Registries.BLOCK.getId(Blocks.DIRT)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.COARSE_DIRT)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.ROOTED_DIRT)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.GRASS_BLOCK)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.PODZOL)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.MYCELIUM)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.MUD)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.MUDDY_MANGROVE_ROOTS)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.SAND)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.RED_SAND)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.GRAVEL)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.CLAY)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.SNOW)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.SNOW_BLOCK)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.POWDER_SNOW)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.SOUL_SAND)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.SOUL_SOIL));

        return vanilla || matchesExtraBlock(blockId, KnowledgeBoundConfig.INSTANCE.extraDiggingBlocks);
    }

    /**
     * Returns true only for fully grown crops.
     * Immature crops are ignored entirely (no XP, no fail chance).
     */
    private static boolean isMatureFarmingBlock(BlockState state, Identifier blockId) {
        Block block = state.getBlock();

        // vanilla crops - only process if max age
        if (block instanceof CropBlock cropBlock) {
            if (!cropBlock.isMature(state)) {
                return false; // not fully grown, skip entirely
            }
            boolean vanilla =
                    blockId.equals(Registries.BLOCK.getId(Blocks.WHEAT)) ||
                            blockId.equals(Registries.BLOCK.getId(Blocks.CARROTS)) ||
                            blockId.equals(Registries.BLOCK.getId(Blocks.POTATOES)) ||
                            blockId.equals(Registries.BLOCK.getId(Blocks.BEETROOTS)) ||
                            blockId.equals(Registries.BLOCK.getId(Blocks.MELON_STEM)) ||
                            blockId.equals(Registries.BLOCK.getId(Blocks.PUMPKIN_STEM));
            return vanilla || matchesExtraBlock(blockId, KnowledgeBoundConfig.INSTANCE.extraFarmingBlocks);
        }

        // modded/extra crops - always process
        return matchesExtraBlock(blockId, KnowledgeBoundConfig.INSTANCE.extraFarmingBlocks);
    }

    private static boolean matchesExtraBlock(Identifier blockId, java.util.List<String> ids) {
        String full = blockId.toString();
        for (String s : ids) {
            if (full.equals(s)) {
                return true;
            }
        }
        return false;
    }

    // ----------------------------------------------------------------------
    // Ranged Combat XP (bow / crossbow hits)
    // ----------------------------------------------------------------------
    private static void registerMeleeCombatXp() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            // If they are just tapping something for 0 damage, don't give XP.
            if (amount <= 0.0f) {
                return true;
            }

            // Skip projectile damage – that's handled by ranged combat
            if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                return true;
            }

            Entity attacker = source.getAttacker();
            if (!(attacker instanceof ServerPlayerEntity player)) {
                return true;
            }

            KnowledgeDefinition meleeDef =
                    KnowledgeRegistry.get(KnowledgeRegistry.MELEE_COMBAT_ID);
            if (meleeDef == null) {
                return true;
            }

            // Only count sword hits for now. If they punch a cow, no combat XP.
            ItemStack held = player.getMainHandStack();
            if (!isSwordItem(held)) {
                return true;
            }

            // ---- Combat fail roll ----
            // We check if they are trying to use a diamond sword at level 0.
            CombatFailHelper.CombatOutcome outcome =
                    CombatFailHelper.rollCombatOutcome(player,
                            KnowledgeRegistry.MELEE_COMBAT_ID, held);

            if (outcome == CombatFailHelper.CombatOutcome.FAIL) {
                // If they completely fail, the sword literally slips out of their hands.
                player.getServer().execute(() -> {
                    CombatFailHelper.dropWeapon(player);
                });
                // We actually don't cancel the damage here, we let the hit go through,
                // but they lose their weapon.
            }

            // Map the sword material to WOOD / STONE / IRON / DIAMOND, etc.
            KnowledgeDefinition.ToolTier toolTier =
                    ToolTierHelper.fromItem(held);

            // Grant XP if this tool tier is valid for current melee tier
            grantXpIfValidTool(player, meleeDef, toolTier);

            return true; // allow damage
        });
    }

    private static void registerRangedCombatXp() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            // This runs BEFORE damage is applied, but after we've confirmed
            // that something is about to take damage.

            // Ignore non-positive damage
            if (amount <= 0.0f) {
                return true; // allow damage
            }

            // Only care about projectile damage (arrows, etc.)
            if (!source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                return true;
            }

            Entity attacker = source.getAttacker();
            if (!(attacker instanceof ServerPlayerEntity player)) {
                return true;
            }

            // Ranged knowledge definition
            KnowledgeDefinition rangedDef =
                    KnowledgeRegistry.get(KnowledgeRegistry.RANGED_COMBAT_ID);
            if (rangedDef == null) {
                return true;
            }

            // what are we holding?
            KnowledgeDefinition.ToolTier toolTier =
                    ToolTierHelper.fromItem(player.getMainHandStack());
            if (toolTier != KnowledgeDefinition.ToolTier.BOW
                    && toolTier != KnowledgeDefinition.ToolTier.CROSSBOW) {
                toolTier = ToolTierHelper.fromItem(player.getOffHandStack());
            }

            // Will only grant XP if that tier is valid for the current knowledge tier
            grantXpIfValidTool(player, rangedDef, toolTier);

            // We don't want to block damage, just observe it.
            return true;
        });
    }


    // ----------------------------------------------------------------------
    // XP helper
    // ----------------------------------------------------------------------

    private static void grantXpIfValidTool(ServerPlayerEntity player,
                                           KnowledgeDefinition def,
                                           KnowledgeDefinition.ToolTier toolTier) {
        int currentTier = PlayerKnowledgeManager.getTier(player, def.getId());
        if (def.getXpToolTiersFor(currentTier).contains(toolTier)) {
            PlayerKnowledgeManager.grantMinuteIfAllowed(player, def.getId());
        }
    }

    // ----------------------------------------------------------------------
    // Crafting hook used by CraftingResultSlotMixin
    // ----------------------------------------------------------------------

    public static ItemStack handleCrafting(ServerPlayerEntity player,
                                           Identifier itemId,
                                           ItemStack originalStack) {

        // 0) Check if the item is completely blocked from crafting
        if (CraftingRuleRegistry.isBlocked(itemId)) {
            String msgStr = KnowledgeBoundConfig.INSTANCE.messages.blockedCraftingItem;
            player.sendMessage(Text.literal(msgStr), true);
            return ItemStack.EMPTY;
        }

        // 1) Apply crafting rule (poor / fail / normal) if one exists
        CraftingKnowledgeRule rule = CraftingRuleRegistry.getForItem(itemId);
        ItemStack result = originalStack;

        if (rule != null) {
            int tier = PlayerKnowledgeManager.getTier(player, rule.getKnowledgeId());
            result = rule.apply(player, itemId, originalStack, tier);
        }

        // 2) Grant XP for the relevant crafting knowledge (only if something was actually crafted)
        if (!result.isEmpty()) {
            grantCraftingXp(player, itemId, rule);
        }

        return result;
    }

    // ----------------------------------------------------------------------
    // Crafting XP helpers
    // ----------------------------------------------------------------------

    private static void grantCraftingXp(ServerPlayerEntity player, Identifier itemId, CraftingKnowledgeRule rule) {
        // if the item has a registered rule, use that rule's knowledge
        if (rule != null) {
            PlayerKnowledgeManager.grantMinuteIfAllowed(player, rule.getKnowledgeId());
            return;
        }

        // fallback: check by item name pattern for items without explicit rules
        String path = itemId.getPath();

        if (isToolItem(path)) {
            PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.TOOLSMITHING_ID);
        }

        if (isWeaponItem(path)) {
            PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.WEAPONSMITHING_ID);
        }

        if (isArmorItem(path)) {
            PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.ARMOURING_ID);
        }
    }

    private static boolean isToolItem(String path) {
        return path.endsWith("_pickaxe")
                || path.endsWith("_axe")
                || path.endsWith("_shovel")
                || path.endsWith("_hoe");
    }

    private static boolean isWeaponItem(String path) {
        return path.endsWith("_sword")
                || path.equals("bow")
                || path.equals("crossbow")
                || path.equals("trident");
    }
    private static boolean isSwordItem(ItemStack stack) {
        return !stack.isEmpty() && stack.isIn(KnowledgeTags.MELEE_WEAPONS);
    }

    private static boolean isArmorItem(String path) {
        return path.endsWith("_helmet")
                || path.endsWith("_chestplate")
                || path.endsWith("_leggings")
                || path.endsWith("_boots")
                || path.equals("turtle_helmet");
    }

    // ----------------------------------------------------------------------
    // Stonecutter output hook (used by StonecutterScreenHandlerMixin + ScreenHandlerMixin)
    // ----------------------------------------------------------------------

    /**
     * Apply masonry mechanics when a player takes stonecutter output.
     * Returns true if the craft FAILED (caller should cancel the item transfer).
     */
    public static boolean handleStonecutterOutput(ServerPlayerEntity player, ScreenHandler handler) {
        Slot outputSlot = handler.slots.get(1);
        if (!outputSlot.hasStack() || outputSlot.getStack().isEmpty()) return false;

        ItemStack outputStack = outputSlot.getStack();
        int masonryTier = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.MASONRY_ID);
        Identifier itemId = Registries.ITEM.getId(outputStack.getItem());
        
        // Find out what tier the stonecutter output is.
        int itemTier = CraftingRuleRegistry.getItemTier(itemId);
        int diff = masonryTier - itemTier;

        // get fail chance from config
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        KnowledgeBoundConfig.CraftingTierChances tc = cfg.getCraftingChancesForDiff(diff);
        tc.normalize();

        double failChance = Math.max(0.0, tc.failChance);

        if (RANDOM.nextDouble() < failChance) {
            // craft failed — consume input, clear output, notify player
            player.sendMessage(
                    KnowledgeBoundTextFormatter.craftingFail(KnowledgeRegistry.MASONRY_ID),
                    true
            );

            // clear the input slot (slot 0) to consume ingredients
            // Yes, we literally destroy the raw material to punish failure.
            Slot inputSlot = handler.slots.get(0);
            ItemStack inputStack = inputSlot.getStack();
            if (!inputStack.isEmpty()) {
                inputStack.decrement(1);
            }

            // clear the output
            outputSlot.setStack(ItemStack.EMPTY);

            // sync to client so the item visually disappears from their mouse
            handler.sendContentUpdates();

            // still grant XP (you learn from failure)
            PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.MASONRY_ID);
            return true;
        }

        // If they successfully crafted it, there's still a chance they cut themselves
        // on the sawblade! Lower tiers cut themselves more often.
        double cutChance = cfg.stonecutterCutChanceTier1 - ((masonryTier - 1) * cfg.stonecutterCutReductionPerTier);
        cutChance = Math.max(0.0, cutChance);

        if (RANDOM.nextDouble() < cutChance) {
            player.damage(player.getDamageSources().generic(), cfg.stonecutterCutDamage);
            String msgStr = cfg.messages.stonecutterCutSelf;
            player.sendMessage(Text.literal(msgStr), true);
        }

        // send normal quality craft message
        String msgTemplate = cfg.messages.craftingQualityNormal;
        String msgStr = msgTemplate.replace("{knowledge}", "Masonry");
        player.sendMessage(Text.literal(msgStr), true);

        // grant masonry xp on successful craft
        PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.MASONRY_ID);
        return false;
    }

    // ----------------------------------------------------------------------
    // Tool tier helper
    // ----------------------------------------------------------------------

    public static class ToolTierHelper {
        public static KnowledgeDefinition.ToolTier fromItem(ItemStack stack) {
            if (stack.isEmpty()) return KnowledgeDefinition.ToolTier.FIST;

            if (stack.isIn(KnowledgeTags.WOODEN_TOOLS))   return KnowledgeDefinition.ToolTier.WOOD;
            if (stack.isIn(KnowledgeTags.STONE_TOOLS))    return KnowledgeDefinition.ToolTier.STONE;
            if (stack.isIn(KnowledgeTags.COPPER_TOOLS))   return KnowledgeDefinition.ToolTier.COPPER;
            if (stack.isIn(KnowledgeTags.IRON_TOOLS))     return KnowledgeDefinition.ToolTier.IRON;
            if (stack.isIn(KnowledgeTags.DIAMOND_TOOLS))  return KnowledgeDefinition.ToolTier.DIAMOND;
            if (stack.isIn(KnowledgeTags.LEATHER_ARMOR))  return KnowledgeDefinition.ToolTier.LEATHER;
            if (stack.isIn(KnowledgeTags.CHAINMAIL_ARMOR)) return KnowledgeDefinition.ToolTier.CHAINMAIL;
            if (stack.isIn(KnowledgeTags.CROSSBOWS))      return KnowledgeDefinition.ToolTier.CROSSBOW;
            if (stack.isIn(KnowledgeTags.BOWS))           return KnowledgeDefinition.ToolTier.BOW;
            if (stack.isIn(KnowledgeTags.FISHING_RODS))   return KnowledgeDefinition.ToolTier.FISHING_ROD;

            return KnowledgeDefinition.ToolTier.UNKNOWN;
        }
    }
}



