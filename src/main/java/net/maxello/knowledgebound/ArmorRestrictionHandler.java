package net.maxello.knowledgebound;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class ArmorRestrictionHandler {

    private ArmorRestrictionHandler() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                checkPlayerArmor(player);
            }
        });
    }

    private static void checkPlayerArmor(ServerPlayerEntity player) {
        // Combat knowledge = best of Melee + Ranged Combat
        int meleeTier  = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.MELEE_COMBAT_ID);
        int rangedTier = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.RANGED_COMBAT_ID);
        int combatTier = Math.max(meleeTier, rangedTier);

        // Check all armor slots
        checkSlot(player, EquipmentSlot.HEAD,  combatTier);
        checkSlot(player, EquipmentSlot.CHEST, combatTier);
        checkSlot(player, EquipmentSlot.LEGS,  combatTier);
        checkSlot(player, EquipmentSlot.FEET,  combatTier);
    }

    private static void checkSlot(ServerPlayerEntity player, EquipmentSlot slot, int combatTier) {
        ItemStack stack = player.getEquippedStack(slot);
        if (stack.isEmpty()) return;
        if (!(stack.getItem() instanceof ArmorItem armorItem)) return;

        int requiredTier = getRequiredArmourTier(armorItem, stack);
        if (requiredTier < 0) {
            // Unknown / unrestricted armor → allowed
            return;
        }

        if (combatTier < requiredTier) {
            String tierName = getTierName(requiredTier);

            Text msg = Text.literal("You need " + tierName + " Combat Knowledge to wear this armor!")
                    .formatted(Formatting.RED);
            // Action bar message
            player.sendMessage(msg, true);

            // Remove from armor slot
            ItemStack copy = stack.copy();
            player.equipStack(slot, ItemStack.EMPTY);

            // Try to put it back into inventory; if full, drop it
            if (!player.getInventory().insertStack(copy)) {
                player.dropItem(copy, false);
            }
        }
    }

    /**
     * Determine required tier for this armor, using config first, then vanilla material mapping.
     */
    private static int getRequiredArmourTier(ArmorItem armorItem, ItemStack stack) {
        KnowledgeBoundConfig.ArmorTierConfig cfg = KnowledgeBoundConfig.INSTANCE.armorTiers;

        // 1) Check per-item override from config.extraItemTiers
        Identifier itemId = Registries.ITEM.getId(armorItem);
        if (itemId != null) {
            Integer override = cfg.extraItemTiers.get(itemId.toString());
            if (override != null) {
                return override;
            }
        }

        // 2) Fallback: use material-based defaults (also configurable)
        ArmorMaterial mat = armorItem.getMaterial();

        if (mat == ArmorMaterials.LEATHER) {
            return cfg.leatherTier;
        } else if (mat == ArmorMaterials.CHAIN) {
            return cfg.chainTier;
        } else if (mat == ArmorMaterials.IRON) {
            return cfg.ironTier;
        } else if (mat == ArmorMaterials.GOLD) {
            return cfg.goldTier;
        } else if (mat == ArmorMaterials.DIAMOND) {
            return cfg.diamondTier;
        } else if (mat == ArmorMaterials.NETHERITE) {
            return cfg.netheriteTier;
        }

        // 3) Unknown / modded material with no override: unrestricted by default
        return -1;
    }

    /**
     * Pretty name for the tier shown in the message.
     */
    private static String getTierName(int tier) {
        return switch (tier) {
            case 0 -> "Leather";
            case 1 -> "Chainmail";
            case 2 -> "Iron";
            case 3 -> "Gold";
            case 4 -> "Diamond";
            case 5 -> "Netherite";
            default -> "higher";
        };
    }
}
