package net.maxello.knowledgebound.mechanics.combat;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.util.KbIdHelper;
import net.maxello.knowledgebound.util.KnowledgeBoundTextFormatter;
import net.maxello.knowledgebound.mechanics.crafting.CraftingRuleRegistry;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.Random;

/**
 * The brains behind what happens when a player tries to fight with a weapon
 * they are way too underleveled for.
 * 
 * Used by our melee events and our bow/crossbow mixins to calculate whether
 * an attack completely fumbles (FAIL), goes somewhat wrong (POOR), or works
 * perfectly fine (NORMAL).
 */
public final class CombatFailHelper {

    private CombatFailHelper() {}

    private static final Random RANDOM = new Random();

    /**
     * Represents the result of an attack roll.
     */
    public enum CombatOutcome {
        FAIL, POOR, NORMAL
    }

    /**
     * Rolls the dice to see if the player actually manages to swing/shoot their weapon properly.
     * We use the exact same logic table that crafting uses.
     *
     * @param player      The attacking player
     * @param knowledgeId The relevant combat knowledge (melee or ranged)
     * @param weaponStack The weapon being used (diamond sword, bow, etc.)
     * @return The combat outcome
     */
    public static CombatOutcome rollCombatOutcome(ServerPlayerEntity player,
                                                   Identifier knowledgeId,
                                                   ItemStack weaponStack) {
        // If the server owner disabled this feature, just always succeed.
        if (!KnowledgeBoundConfig.INSTANCE.weaponDropOnFail) {
            return CombatOutcome.NORMAL;
        }

        int playerTier = PlayerKnowledgeManager.getTier(player, knowledgeId);
        int weaponTier = getWeaponRequiredTier(weaponStack);
        
        // This is the core number. If it's negative, the player is underleveled.
        int diff = playerTier - weaponTier;

        // Fetch the failure chances from the config based on how far below the tier they are.
        KnowledgeBoundConfig.CraftingTierChances tc =
                KnowledgeBoundConfig.INSTANCE.getCraftingChancesForDiff(diff);
        tc.normalize();

        double roll = RANDOM.nextDouble();
        double failChance = Math.max(0.0, tc.failChance);
        double poorChance = Math.max(0.0, tc.poorChance);

        // A total FAIL usually means dropping the weapon.
        if (roll < failChance) {
            return CombatOutcome.FAIL;
        }
        // A POOR hit usually means half damage or wild arrow spread.
        if (roll < failChance + poorChance) {
            return CombatOutcome.POOR;
        }
        
        // They managed to handle the weapon just fine.
        return CombatOutcome.NORMAL;
    }

    /**
     * Figures out what tier a specific weapon actually is.
     * E.g. Iron Sword = Tier 2. Diamond Sword = Tier 3.
     */
    public static int getWeaponRequiredTier(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        String itemIdStr = KbIdHelper.getKbId(stack);

        // We check the combat config overrides first. This lets admins assign custom tiers
        // to modded weapons without having to mess with the crafting recipes.
        Map<String, Integer> overrides = KnowledgeBoundConfig.INSTANCE.combatWeaponTierOverrides;
        if (overrides.containsKey(itemIdStr)) {
            return overrides.get(itemIdStr);
        }

        // If there's no combat-specific override, we just default to whatever tier
        // it requires to craft the item.
        return CraftingRuleRegistry.getItemTier(Identifier.of(itemIdStr));
    }

    /**
     * The punishment for a total FAIL. We literally throw the weapon out of their hands.
     * We have to be very careful to preserve all enchantments, durability, and custom
     * data components when we spawn the dropped item entity.
     */
    public static void dropWeapon(ServerPlayerEntity player) {
        ItemStack weapon = player.getMainHandStack();
        if (weapon.isEmpty()) return;

        // Copy the weapon exactly as it is, then delete it from their inventory.
        ItemStack dropped = weapon.copy();
        player.getMainHandStack().setCount(0);

        // Spawn a physical item entity in the world where they are standing.
        Vec3d pos = player.getPos();
        ItemEntity itemEntity = new ItemEntity(
                player.getWorld(),
                pos.x, pos.y + 0.5, pos.z,
                dropped
        );
        
        // Give it a tiny bit of random velocity so it scatters visually instead of just
        // falling perfectly straight down into their feet.
        itemEntity.setVelocity(
                (RANDOM.nextDouble() - 0.5) * 0.2,
                0.2,
                (RANDOM.nextDouble() - 0.5) * 0.2
        );
        
        // Put a 2-second pickup delay on it. Otherwise, the server might just instantly
        // vacuum it right back into their inventory before they even realize it dropped.
        itemEntity.setPickupDelay(40); 
        player.getWorld().spawnEntity(itemEntity);

        // Let them know why their sword suddenly disappeared.
        player.sendMessage(
                KnowledgeBoundTextFormatter.formatSimple(
                        KnowledgeBoundConfig.INSTANCE.messages.weaponDropped),
                true // action bar
        );
    }

    /**
     * Calculates how wildly inaccurate an arrow should be based on the player's skill.
     * If you're a level 0 archer firing a heavy crossbow, that bolt is going sideways.
     */
    public static float getAccuracyPenalty(ServerPlayerEntity player,
                                           boolean isCrossbow,
                                           boolean isPoor) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        int playerTier = PlayerKnowledgeManager.getTier(
                player, KnowledgeRegistry.RANGED_COMBAT_ID);

        // Bows are easier to use than crossbows by default.
        int weaponTier = isCrossbow ? 2 : 1; 
        
        // Check if the server admin overrode the requirements.
        String weaponId = isCrossbow ? "minecraft:crossbow" : "minecraft:bow";
        if (cfg.combatWeaponTierOverrides.containsKey(weaponId)) {
            weaponTier = cfg.combatWeaponTierOverrides.get(weaponId);
        }

        int diff = playerTier - weaponTier;

        // We map the tier difference to an array index. 
        // 0 means horribly underleveled (diff -3), 5 means you are overleveled (diff +2).
        int index;
        if (diff <= -3) index = 0;
        else if (diff == -2) index = 1;
        else if (diff == -1) index = 2;
        else if (diff == 0) index = 3; // Exactly the right level
        else if (diff == 1) index = 4;
        else index = 5;

        // Grab the appropriate spread array from the config.
        double[] spreadArray = isCrossbow
                ? cfg.crossbowSpreadPerDiff
                : cfg.bowSpreadPerDiff;

        double baseSpread = 0.0;
        if (index >= 0 && index < spreadArray.length) {
            baseSpread = spreadArray[index];
        }

        // If the RNG gods rolled a "POOR" outcome for this shot, we add even MORE spread.
        if (isPoor) {
            baseSpread += cfg.poorSpreadModifier;
        }

        return (float) baseSpread;
    }
}



