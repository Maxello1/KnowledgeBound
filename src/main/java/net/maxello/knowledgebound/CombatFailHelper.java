package net.maxello.knowledgebound;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.Random;

/**
 * Shared utility class for combat fail logic.
 * Used by melee combat events and bow/crossbow mixins.
 */
public final class CombatFailHelper {

    private CombatFailHelper() {}

    private static final Random RANDOM = new Random();

    public enum CombatOutcome {
        FAIL, POOR, NORMAL
    }

    /**
     * Rolls a combat outcome using the shared tier-difference table.
     *
     * @param player      The attacking player
     * @param knowledgeId The relevant combat knowledge (melee or ranged)
     * @param weaponStack The weapon being used
     * @return The combat outcome
     */
    public static CombatOutcome rollCombatOutcome(ServerPlayerEntity player,
                                                   Identifier knowledgeId,
                                                   ItemStack weaponStack) {
        if (!KnowledgeBoundConfig.INSTANCE.weaponDropOnFail) {
            return CombatOutcome.NORMAL;
        }

        int playerTier = PlayerKnowledgeManager.getTier(player, knowledgeId);
        int weaponTier = getWeaponRequiredTier(weaponStack);
        int diff = playerTier - weaponTier;

        KnowledgeBoundConfig.CraftingTierChances tc =
                KnowledgeBoundConfig.INSTANCE.getCraftingChancesForDiff(diff);
        tc.normalize();

        double roll = RANDOM.nextDouble();
        double failChance = Math.max(0.0, tc.failChance);
        double poorChance = Math.max(0.0, tc.poorChance);

        if (roll < failChance) {
            return CombatOutcome.FAIL;
        }
        if (roll < failChance + poorChance) {
            return CombatOutcome.POOR;
        }
        return CombatOutcome.NORMAL;
    }

    /**
     * Gets the required combat tier for a weapon.
     * Checks combatWeaponTierOverrides first, then falls back to CraftingRuleRegistry.
     */
    public static int getWeaponRequiredTier(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        String itemIdStr = itemId.toString();

        // Check config overrides first
        Map<String, Integer> overrides = KnowledgeBoundConfig.INSTANCE.combatWeaponTierOverrides;
        if (overrides.containsKey(itemIdStr)) {
            return overrides.get(itemIdStr);
        }

        // Fall back to crafting tier registry
        return CraftingRuleRegistry.getItemTier(itemId);
    }

    /**
     * Drops the player's main hand weapon into the world.
     * Preserves all item data (durability, enchantments, components, etc.).
     */
    public static void dropWeapon(ServerPlayerEntity player) {
        ItemStack weapon = player.getMainHandStack();
        if (weapon.isEmpty()) return;

        // Remove from player's hand
        ItemStack dropped = weapon.copy();
        player.getMainHandStack().setCount(0);

        // Spawn as item entity at player's position
        Vec3d pos = player.getPos();
        ItemEntity itemEntity = new ItemEntity(
                player.getWorld(),
                pos.x, pos.y + 0.5, pos.z,
                dropped
        );
        // Small random velocity so it doesn't stack perfectly
        itemEntity.setVelocity(
                (RANDOM.nextDouble() - 0.5) * 0.2,
                0.2,
                (RANDOM.nextDouble() - 0.5) * 0.2
        );
        // Prevent immediate pickup
        itemEntity.setPickupDelay(40); // 2 seconds
        player.getWorld().spawnEntity(itemEntity);

        // Notify player
        player.sendMessage(
                KnowledgeBoundTextFormatter.formatSimple(
                        KnowledgeBoundConfig.INSTANCE.messages.weaponDropped),
                true
        );
    }

    /**
     * Calculates the extra divergence (inaccuracy) for a ranged weapon.
     *
     * @param player      The shooting player
     * @param isCrossbow  Whether this is a crossbow (vs bow)
     * @param isPoor      Whether this is a "poor" quality shot
     * @return Extra divergence to add to the projectile
     */
    public static float getAccuracyPenalty(ServerPlayerEntity player,
                                           boolean isCrossbow,
                                           boolean isPoor) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        int playerTier = PlayerKnowledgeManager.getTier(
                player, KnowledgeRegistry.RANGED_COMBAT_ID);

        // Determine weapon required tier
        int weaponTier = isCrossbow ? 2 : 1; // default: bow=1, crossbow=2
        // Check overrides
        String weaponId = isCrossbow ? "minecraft:crossbow" : "minecraft:bow";
        if (cfg.combatWeaponTierOverrides.containsKey(weaponId)) {
            weaponTier = cfg.combatWeaponTierOverrides.get(weaponId);
        }

        int diff = playerTier - weaponTier;

        // Map diff to spread array index: <= -3 -> 0, -2 -> 1, -1 -> 2, 0 -> 3, +1 -> 4, >= +2 -> 5
        int index;
        if (diff <= -3) index = 0;
        else if (diff == -2) index = 1;
        else if (diff == -1) index = 2;
        else if (diff == 0) index = 3;
        else if (diff == 1) index = 4;
        else index = 5;

        double[] spreadArray = isCrossbow
                ? cfg.crossbowSpreadPerDiff
                : cfg.bowSpreadPerDiff;

        double baseSpread = 0.0;
        if (index >= 0 && index < spreadArray.length) {
            baseSpread = spreadArray[index];
        }

        // Apply poor modifier
        if (isPoor) {
            baseSpread += cfg.poorSpreadModifier;
        }

        return (float) baseSpread;
    }
}
