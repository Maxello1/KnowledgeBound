package net.maxello.knowledgebound;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class KnowledgeEvents {

    public static void init() {
        KnowledgeBound.LOGGER.info("[KnowledgeBound] Registering events…");

        registerBlockBreakXp();
    }

    private static void registerBlockBreakXp() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                Block block = state.getBlock();
                Identifier blockId = Registries.BLOCK.getId(block);

                if (isForestryBlock(blockId)) {
                    KnowledgeDefinition def = KnowledgeRegistry.get(KnowledgeRegistry.FORESTRY_ID);
                    if (def != null) {
                        KnowledgeDefinition.ToolTier toolTier =
                                ToolTierHelper.fromItem(serverPlayer.getMainHandStack());
                        grantXpIfValidTool(serverPlayer, def, toolTier);
                    }
                }
            }
        });
    }

    private static boolean isForestryBlock(Identifier blockId) {
        String path = blockId.getPath();
        return path.endsWith("_log") || path.endsWith("_wood");
    }

    private static void grantXpIfValidTool(ServerPlayerEntity player,
                                           KnowledgeDefinition def,
                                           KnowledgeDefinition.ToolTier toolTier) {
        int currentTier = PlayerKnowledgeManager.getTier(player, def.getId());
        if (def.getXpToolTiersFor(currentTier).contains(toolTier)) {
            PlayerKnowledgeManager.grantMinuteIfAllowed(player, def.getId());
        }
    }

    /**
     * Called from our crafting mixin.
     * @param player        crafting player
     * @param itemId        ID of the crafted item
     * @param originalStack vanilla output stack
     * @return modified result (normal / poor / fail)
     */
    public static ItemStack handleCrafting(ServerPlayerEntity player,
                                           Identifier itemId,
                                           ItemStack originalStack) {
        CraftingKnowledgeRule rule = CraftingRuleRegistry.getForItem(itemId);
        if (rule == null) {
            KnowledgeBound.LOGGER.debug("[KB] No rule for item {}", itemId);
            return originalStack;
        }

        int tier = PlayerKnowledgeManager.getTier(player, rule.getKnowledgeId());
        KnowledgeBound.LOGGER.debug("[KB] Applying rule for item {} at tier {}", itemId, tier);
        return rule.apply(player, itemId, originalStack, tier);
    }


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
