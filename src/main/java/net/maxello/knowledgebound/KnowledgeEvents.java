package net.maxello.knowledgebound;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Handles block-break XP for material knowledges and the crafting hook
 * used by the CraftingResultSlot mixin.
 */
public class KnowledgeEvents {

    public static void init() {
        KnowledgeBound.LOGGER.info("[KnowledgeBound] Registering events…");
        registerBlockBreakXp();
    }

    // ----------------------------------------------------------------------
    // Block break XP (Forestry, Mining, Digging, Farming)
    // ----------------------------------------------------------------------

    private static void registerBlockBreakXp() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

            Block block = state.getBlock();
            Identifier blockId = Registries.BLOCK.getId(block);

            // --- Forestry: logs & wood ---
            if (isForestryBlock(blockId)) {
                KnowledgeDefinition def = KnowledgeRegistry.get(KnowledgeRegistry.FORESTRY_ID);
                if (def != null) {
                    KnowledgeDefinition.ToolTier toolTier =
                            ToolTierHelper.fromItem(serverPlayer.getMainHandStack());
                    grantXpIfValidTool(serverPlayer, def, toolTier);
                }
            }

            // --- Mining: stone, ores, deepslate, etc. ---
            if (isMiningBlock(blockId)) {
                KnowledgeDefinition def = KnowledgeRegistry.get(KnowledgeRegistry.MINING_ID);
                if (def != null) {
                    KnowledgeDefinition.ToolTier toolTier =
                            ToolTierHelper.fromItem(serverPlayer.getMainHandStack());
                    grantXpIfValidTool(serverPlayer, def, toolTier);
                }
            }

            // --- Digging: shovel blocks (dirt, sand, gravel, etc.) ---
            if (isDiggingBlock(blockId)) {
                KnowledgeDefinition def = KnowledgeRegistry.get(KnowledgeRegistry.DIGGING_ID);
                if (def != null) {
                    KnowledgeDefinition.ToolTier toolTier =
                            ToolTierHelper.fromItem(serverPlayer.getMainHandStack());
                    grantXpIfValidTool(serverPlayer, def, toolTier);
                }
            }

            // --- Farming: crops ---
            if (isFarmingBlock(blockId)) {
                KnowledgeDefinition def = KnowledgeRegistry.get(KnowledgeRegistry.FARMING_ID);
                if (def != null) {
                    KnowledgeDefinition.ToolTier toolTier =
                            ToolTierHelper.fromItem(serverPlayer.getMainHandStack());
                    grantXpIfValidTool(serverPlayer, def, toolTier);
                }
            }
        });
    }

    private static boolean isForestryBlock(Identifier blockId) {
        String path = blockId.getPath();
        return path.endsWith("_log")
                || path.endsWith("_wood")
                || path.endsWith("_stem")
                || path.endsWith("_hyphae");
    }

    private static boolean isMiningBlock(Identifier blockId) {
        String path = blockId.getPath();

        if (path.equals("stone")
                || path.equals("deepslate")
                || path.equals("netherrack")
                || path.equals("blackstone")
                || path.equals("tuff")) {
            return true;
        }

        return path.endsWith("_ore")
                || path.equals("gilded_blackstone");
    }

    private static boolean isDiggingBlock(Identifier blockId) {
        // Classic shovel blocks
        return blockId.equals(Registries.BLOCK.getId(Blocks.DIRT))
                || blockId.equals(Registries.BLOCK.getId(Blocks.COARSE_DIRT))
                || blockId.equals(Registries.BLOCK.getId(Blocks.ROOTED_DIRT))
                || blockId.equals(Registries.BLOCK.getId(Blocks.GRASS_BLOCK))
                || blockId.equals(Registries.BLOCK.getId(Blocks.PODZOL))
                || blockId.equals(Registries.BLOCK.getId(Blocks.MYCELIUM))
                || blockId.equals(Registries.BLOCK.getId(Blocks.MUD))
                || blockId.equals(Registries.BLOCK.getId(Blocks.MUDDY_MANGROVE_ROOTS))
                || blockId.equals(Registries.BLOCK.getId(Blocks.SAND))
                || blockId.equals(Registries.BLOCK.getId(Blocks.RED_SAND))
                || blockId.equals(Registries.BLOCK.getId(Blocks.GRAVEL))
                || blockId.equals(Registries.BLOCK.getId(Blocks.CLAY))
                || blockId.equals(Registries.BLOCK.getId(Blocks.SNOW))
                || blockId.equals(Registries.BLOCK.getId(Blocks.SNOW_BLOCK))
                || blockId.equals(Registries.BLOCK.getId(Blocks.POWDER_SNOW))
                || blockId.equals(Registries.BLOCK.getId(Blocks.SOUL_SAND))
                || blockId.equals(Registries.BLOCK.getId(Blocks.SOUL_SOIL));
    }

    private static boolean isFarmingBlock(Identifier blockId) {
        return blockId.equals(Registries.BLOCK.getId(Blocks.WHEAT))
                || blockId.equals(Registries.BLOCK.getId(Blocks.CARROTS))
                || blockId.equals(Registries.BLOCK.getId(Blocks.POTATOES))
                || blockId.equals(Registries.BLOCK.getId(Blocks.BEETROOTS))
                || blockId.equals(Registries.BLOCK.getId(Blocks.MELON_STEM))
                || blockId.equals(Registries.BLOCK.getId(Blocks.PUMPKIN_STEM));
    }

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

        CraftingKnowledgeRule rule = CraftingRuleRegistry.getForItem(itemId);
        if (rule == null) {
            return originalStack;
        }

        int tier = PlayerKnowledgeManager.getTier(player, rule.getKnowledgeId());
        return rule.apply(player, itemId, originalStack, tier);
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
