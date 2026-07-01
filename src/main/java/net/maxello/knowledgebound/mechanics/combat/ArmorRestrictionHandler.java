package net.maxello.knowledgebound.mechanics.combat;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.util.KbIdHelper;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;
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

/**
 * Ensures players don't cheat the system by wearing armor they don't have the skill for.
 * 
 * We check a player's inventory on a slow tick. If they are wearing Diamond armor
 * but they don't have high enough Melee or Ranged combat tier, we rip the armor
 * right off their body and throw it into their inventory (or on the ground if full).
 */
public final class ArmorRestrictionHandler {

    // We don't need to check armor every single tick, that's way too heavy.
    // Checking once a second (every 20 ticks) is totally fine.
    private static int tickCounter = 0;

    private ArmorRestrictionHandler() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter < 20) return;
            tickCounter = 0;
            
            // Do a sweep over everyone online.
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                checkPlayerArmor(player);
            }
        });
    }

    private static void checkPlayerArmor(ServerPlayerEntity player) {
        // You only need ONE of the combat skills to be high enough to wear armor.
        // E.g. A master archer can wear diamond armor just as well as a master swordsman.
        int meleeTier  = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.MELEE_COMBAT_ID);
        int rangedTier = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.RANGED_COMBAT_ID);
        int combatTier = Math.max(meleeTier, rangedTier);

        // Check all four armor slots.
        checkSlot(player, EquipmentSlot.HEAD,  combatTier);
        checkSlot(player, EquipmentSlot.CHEST, combatTier);
        checkSlot(player, EquipmentSlot.LEGS,  combatTier);
        checkSlot(player, EquipmentSlot.FEET,  combatTier);
    }

    private static void checkSlot(ServerPlayerEntity player, EquipmentSlot slot, int combatTier) {
        ItemStack stack = player.getEquippedStack(slot);
        if (stack.isEmpty()) return;
        
        // If it's a pumpkin on their head or an elytra, we ignore it. We only care about actual ArmorItems.
        if (!(stack.getItem() instanceof ArmorItem armorItem)) return;

        int requiredTier = getRequiredArmourTier(armorItem, stack);
        
        // A required tier of -1 means this armor isn't restricted by our mod (e.g. unconfigured modded armor).
        if (requiredTier < 0) {
            return;
        }

        if (combatTier < requiredTier) {
            String tierName = getTierName(requiredTier);

            // Format the warning message based on the config.
            String template = KnowledgeBoundConfig.INSTANCE.messages.armorRestricted;
            String msgStr = template.replace("{tierName}", tierName);
            Text msg = Text.literal(msgStr);
            
            // Pop up an action bar message letting them know they're too weak to wear this.
            player.sendMessage(msg, true);

            // Yank the item off them.
            ItemStack copy = stack.copy();
            player.equipStack(slot, ItemStack.EMPTY);

            // Try to gently place it back in their inventory. 
            // If their inventory is completely full, just drop it at their feet.
            if (!player.getInventory().insertStack(copy)) {
                player.dropItem(copy, false);
            }
        }
    }

    /**
     * Determine what tier is required to wear this piece of armor.
     * We check the config overrides first (which lets admins tweak specific items),
     * and if there's no override, we fall back to standard vanilla material checks.
     */
    private static int getRequiredArmourTier(ArmorItem armorItem, ItemStack stack) {
        KnowledgeBoundConfig.ArmorTierConfig cfg = KnowledgeBoundConfig.INSTANCE.armorTiers;

        // 1) Config override. Very useful for modded items.
        String itemIdStr = KbIdHelper.getKbId(stack);
        if (!itemIdStr.isEmpty()) {
            Integer override = cfg.extraItemTiers.get(itemIdStr);
            if (override != null) {
                return override;
            }
        }

        // 2) Vanilla material fallback.
        net.minecraft.registry.entry.RegistryEntry<ArmorMaterial> mat = armorItem.getMaterial();

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

        // 3) Unknown / modded material with no override in the config. We just let them wear it.
        return -1;
    }

    /**
     * Converts an integer tier into a friendly word to put in the warning message.
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



