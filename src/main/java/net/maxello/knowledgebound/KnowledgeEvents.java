package net.maxello.knowledgebound;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;

import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Handles block-break XP/failure for material knowledges,
 * ranged combat XP, and the crafting hook used by the mixin.
 */
public class KnowledgeEvents {

    private static final Random RANDOM = new Random();

    public static void init() {
        KnowledgeBound.LOGGER.info("[KnowledgeBound] Registering events…");
        registerBlockBreakXpAndFailure();
        registerRangedCombatXp();
        registerMeleeCombatXp();
    }

    // ----------------------------------------------------------------------
    // Block break XP + failure (Forestry, Mining, Digging, Farming)
    // ----------------------------------------------------------------------

    private static void registerBlockBreakXpAndFailure() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;

            Block block = state.getBlock();
            Identifier blockId = Registries.BLOCK.getId(block);

            if (isForestryBlock(blockId)) {
                KnowledgeDefinition def = KnowledgeRegistry.get(KnowledgeRegistry.FORESTRY_ID);
                if (def != null) {
                    return handleGatherBlock(world, serverPlayer, pos, state, def);
                }
            } else if (isMiningBlock(blockId)) {
                KnowledgeDefinition def = KnowledgeRegistry.get(KnowledgeRegistry.MINING_ID);
                if (def != null) {
                    return handleGatherBlock(world, serverPlayer, pos, state, def);
                }
            } else if (isDiggingBlock(blockId)) {
                KnowledgeDefinition def = KnowledgeRegistry.get(KnowledgeRegistry.DIGGING_ID);
                if (def != null) {
                    return handleGatherBlock(world, serverPlayer, pos, state, def);
                }
            } else if (isFarmingBlock(blockId)) {
                KnowledgeDefinition def = KnowledgeRegistry.get(KnowledgeRegistry.FARMING_ID);
                if (def != null) {
                    return handleGatherBlock(world, serverPlayer, pos, state, def);
                }
            }

            // Not one of our knowledge blocks → vanilla behaviour.
            return true;
        });
    }

    private static boolean handleGatherBlock(World world,
                                             ServerPlayerEntity player,
                                             BlockPos pos,
                                             BlockState state,
                                             KnowledgeDefinition def) {

        int tier = PlayerKnowledgeManager.getTier(player, def.getId());
        double failChance = getGatherFailChance(def, tier);
        boolean fail = RANDOM.nextDouble() < failChance;

        if (fail) {
            // Scuffed gather: block breaks, but no drops and no XP.
            if (!world.isClient()) {
                world.breakBlock(pos, false, player); // false -> no item drops

                player.sendMessage(
                        KnowledgeBoundTextFormatter.gatheringFail(def.getId()),
                        true
                );
            }
            return false; // cancel vanilla breaking, already handled it
        }

        // Success: let vanilla handle breaking + drops, and grant XP
        KnowledgeDefinition.ToolTier toolTier =
                ToolTierHelper.fromItem(player.getMainHandStack());
        grantXpIfValidTool(player, def, toolTier);

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

    private static boolean isFarmingBlock(Identifier blockId) {
        boolean vanilla =
                blockId.equals(Registries.BLOCK.getId(Blocks.WHEAT)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.CARROTS)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.POTATOES)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.BEETROOTS)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.MELON_STEM)) ||
                        blockId.equals(Registries.BLOCK.getId(Blocks.PUMPKIN_STEM));

        return vanilla || matchesExtraBlock(blockId, KnowledgeBoundConfig.INSTANCE.extraFarmingBlocks);
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
            // Ignore non-positive damage
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

            // Only count sword hits for now
            ItemStack held = player.getMainHandStack();
            if (!isSwordItem(held)) {
                return true;
            }

            // Map the sword material to WOOD / STONE / IRON / DIAMOND, etc.
            KnowledgeDefinition.ToolTier toolTier =
                    ToolTierHelper.fromItem(held);

            // Grant XP if this tool tier is valid for current melee tier
            grantXpIfValidTool(player, meleeDef, toolTier);

            return true; // never cancel damage
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

            // What is the player holding? (bow / crossbow)
            KnowledgeDefinition.ToolTier toolTier =
                    ToolTierHelper.fromItem(player.getMainHandStack());

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

        // 1) Apply crafting rule (poor / fail / normal) if one exists
        CraftingKnowledgeRule rule = CraftingRuleRegistry.getForItem(itemId);
        ItemStack result = originalStack;

        if (rule != null) {
            int tier = PlayerKnowledgeManager.getTier(player, rule.getKnowledgeId());
            result = rule.apply(player, itemId, originalStack, tier);
        }

        // 2) Grant smithing XP (only if something was actually crafted)
        if (!result.isEmpty()) {
            grantSmithingXp(player, itemId);
        }

        return result;
    }

    // ----------------------------------------------------------------------
    // Smithing XP helpers (tool / weapon / armour crafting)
    // ----------------------------------------------------------------------

    private static void grantSmithingXp(ServerPlayerEntity player, Identifier itemId) {
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
        if (stack.isEmpty()) return false;
        String path = stack.getItem().toString();
        return path.endsWith("_sword");
    }

    private static boolean isArmorItem(String path) {
        return path.endsWith("_helmet")
                || path.endsWith("_chestplate")
                || path.endsWith("_leggings")
                || path.endsWith("_boots")
                || path.equals("turtle_helmet");
    }

    // ----------------------------------------------------------------------
    // Tool tier helper
    // ----------------------------------------------------------------------

    public static class ToolTierHelper {
        public static KnowledgeDefinition.ToolTier fromItem(ItemStack stack) {
            if (stack.isEmpty()) {
                return KnowledgeDefinition.ToolTier.FIST;
            }
            String path = stack.getItem().toString();
            if (path.contains("wooden_"))  return KnowledgeDefinition.ToolTier.WOOD;
            if (path.contains("stone_"))   return KnowledgeDefinition.ToolTier.STONE;
            if (path.contains("copper_"))  return KnowledgeDefinition.ToolTier.COPPER;
            if (path.contains("iron_"))    return KnowledgeDefinition.ToolTier.IRON;
            if (path.contains("diamond_")) return KnowledgeDefinition.ToolTier.DIAMOND;
            if (path.contains("leather_"))   return KnowledgeDefinition.ToolTier.LEATHER;
            if (path.contains("chainmail_")) return KnowledgeDefinition.ToolTier.CHAINMAIL;
            if (path.contains("bow"))        return KnowledgeDefinition.ToolTier.BOW;
            if (path.contains("crossbow"))   return KnowledgeDefinition.ToolTier.CROSSBOW;
            if (path.contains("fishing_rod")) return KnowledgeDefinition.ToolTier.FISHING_ROD;

            return KnowledgeDefinition.ToolTier.UNKNOWN;
        }
    }
}
