package net.maxello.knowledgebound;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Random;

/**
 * Defines how a particular set of recipes behaves under knowledge:
 * - chance to fail completely
 * - chance to produce a poor-quality (low durability) item
 * - chance to produce a normal item
 *
 * Chances are based on the DIFFERENCE between the player's knowledge tier
 * and the item's required tier (looked up from CraftingRuleRegistry).
 */
public class CraftingKnowledgeRule {

    private final Identifier id;
    private final Identifier knowledgeId;
    private final double poorDurabilityFraction;
    private final Random random = new Random();

    public CraftingKnowledgeRule(Identifier id,
                                 Identifier knowledgeId,
                                 double poorDurabilityFraction) {
        this.id = id;
        this.knowledgeId = knowledgeId;
        this.poorDurabilityFraction = poorDurabilityFraction;
    }

    public Identifier getId() {
        return id;
    }

    public Identifier getKnowledgeId() {
        return knowledgeId;
    }

    /**
     * Apply this rule to the crafted stack.
     *
     * @param player        the crafter
     * @param itemId        ID of the crafted item
     * @param originalStack vanilla output
     * @param knowledgeTier player's tier in the relevant knowledge
     * @return modified stack, original stack, or EMPTY on full failure
     */
    public ItemStack apply(ServerPlayerEntity player,
                           Identifier itemId,
                           ItemStack originalStack,
                           int knowledgeTier) {

        // get required tier
        int itemTier = CraftingRuleRegistry.getItemTier(itemId);
        int diff = knowledgeTier - itemTier;

        // chances based on diff
        KnowledgeBoundConfig.CraftingTierChances tc =
                KnowledgeBoundConfig.INSTANCE.getCraftingChancesForDiff(diff);
        tc.normalize();

        double roll = random.nextDouble();

        double failChance = Math.max(0.0, tc.failChance);
        double poorChance = Math.max(0.0, tc.poorChance);

        if (roll < failChance) {
            // rip item
            player.sendMessage(
                    KnowledgeBoundTextFormatter.craftingFailSmithing(),
                    true
            );
            return ItemStack.EMPTY;
        }

        if (roll < failChance + poorChance) {
            // scuffed craft
            ItemStack poor = originalStack.copy();
            int maxDmg = poor.getMaxDamage();

            if (maxDmg > 0) {
                int remaining = Math.max(1, (int) Math.round(maxDmg * poorDurabilityFraction));
                int damage = maxDmg - remaining;
                poor.setDamage(damage);
            }

            // send poor quality actionbar
            player.sendMessage(
                    KnowledgeBoundTextFormatter.craftingQualitySmithing("poor"),
                    true
            );
            return poor;
        }

        // normal craft
        return originalStack;
    }
}
