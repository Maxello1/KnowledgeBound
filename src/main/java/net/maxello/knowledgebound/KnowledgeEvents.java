package net.maxello.knowledgebound;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Handles block-break XP for material knowledges and the crafting hook
 * used by the CraftingResultSlot mixin.
 */
public class KnowledgeEvents {

    private static final Random RANDOM = new Random();

    public static void init() {
        KnowledgeBound.LOGGER.info("[KnowledgeBound] Registering events…");
        registerBlockBreakXpAndFailure();
    }

    // ----------------------------------------------------------------------
    // Block break XP + failure (Forestry, Mining, Digging, Farming)
    // ----------------------------------------------------------------------

    private static void registerBlockBreakXpAndFailure() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;

            Block block = state.getBlock();
            Identifier blockId = Registries.BLOCK.getId(block);

            // We handle at most one knowledge per block type.
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

    /**
     * Apply knowledge failure + XP for a single gatherable block.
     *
     * @return true  → allow vanilla to break & drop items
     *         false → we've already broken the block (no drops), cancel vanilla
     */
    private static boolean handleGatherBlock(World world,
                                             ServerPlayerEntity player,
                                             BlockPos pos,
                                             BlockState state,
                                             KnowledgeDefinition def) {

        // Determine current tier & failure chance
        int tier = PlayerKnowledgeManager.getTier(player, def.getId());
        double failChance = getGatherFailChance(def, tier);
        boolean fail = RANDOM.nextDouble() < failChance;

        if (fail) {
            // "Scuffed" gather: block breaks, but no drops and no XP.
            if (!world.isClient()) {
                world.breakBlock(pos, false, player); // false -> no item drops

                // Red message: "Your Forestry attempt failed to yield any resources."
                player.sendMessage(
                        KnowledgeBoundTextFormatter.gatheringFail(def.getId()),
                        true
                );
            }
            return false; // cancel vanilla breaking, we already did it
        }

        // Success: let vanilla handle breaking + drops, and grant XP.
        KnowledgeDefinition.ToolTier toolTier =
                ToolTierHelper.fromItem(player.getMainHandStack());
        grantXpIfValidTool(player, def, toolTier);

        return true;
    }


    // -----------------------------
    // Fail chance per tier
    // -----------------------------

    /**
     * Chance that a gather action yields no drops, per tier.
     * Applies to Forestry, Mining, Digging, Farming.
     *
     * Tier 0: 40% fail
     * Tier 1: 25% fail
     * Tier 2: 10% fail
     * Tier 3:  5% fail
     * Tier 4+: 2% fail
     */
    private static double getGatherFailChance(KnowledgeDefinition def, int tier) {
        Identifier id = def.getId();

        // Only our four gather knowledges use this mechanic.
        if (!(id.equals(KnowledgeRegistry.FORESTRY_ID)
                || id.equals(KnowledgeRegistry.MINING_ID)
                || id.equals(KnowledgeRegistry.DIGGING_ID)
                || id.equals(KnowledgeRegistry.FARMING_ID))) {
            return 0.0;
        }

        int clamped = Math.max(0, Math.min(tier, 4));
        return switch (clamped) {
            case 0 -> 0.40;
            case 1 -> 0.25;
            case 2 -> 0.10;
            case 3 -> 0.05;
            default -> 0.02;
        };
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
            if (path.contains("wooden_")) return KnowledgeDefinition.ToolTier.WOOD;
            if (path.contains("stone_")) return KnowledgeDefinition.ToolTier.STONE;
            if (path.contains("copper_")) return KnowledgeDefinition.ToolTier.COPPER;
            if (path.contains("iron_")) return KnowledgeDefinition.ToolTier.IRON;
            if (path.contains("diamond_")) return KnowledgeDefinition.ToolTier.DIAMOND;
            if (path.contains("leather_")) return KnowledgeDefinition.ToolTier.LEATHER;
            if (path.contains("chainmail_")) return KnowledgeDefinition.ToolTier.CHAINMAIL;
            if (path.contains("bow")) return KnowledgeDefinition.ToolTier.BOW;
            if (path.contains("crossbow")) return KnowledgeDefinition.ToolTier.CROSSBOW;

            return KnowledgeDefinition.ToolTier.UNKNOWN;
        }
    }
}
