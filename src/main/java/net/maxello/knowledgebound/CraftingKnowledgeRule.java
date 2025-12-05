package net.maxello.knowledgebound;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Random;

/**
 * Defines how a particular set of recipes behaves under knowledge:
 * - chance to fail completely
 * - chance to produce a poor-quality (low durability) item
 * - chance to produce a normal item
 */
public class CraftingKnowledgeRule {

    public static class TierChance {
        public final double goodChance;
        public final double poorChance;

        public TierChance(double goodChance, double poorChance) {
            this.goodChance = goodChance;
            this.poorChance = poorChance;
        }
    }

    private final Identifier id;
    private final Identifier knowledgeId;
    private final double poorDurabilityFraction;
    private final Map<Integer, TierChance> tierChances;
    private final Random random = new Random();

    public CraftingKnowledgeRule(Identifier id,
                                 Identifier knowledgeId,
                                 double poorDurabilityFraction,
                                 Map<Integer, TierChance> tierChances) {
        this.id = id;
        this.knowledgeId = knowledgeId;
        this.poorDurabilityFraction = poorDurabilityFraction;
        this.tierChances = tierChances;
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

        // Fallback to tier 0 chances if none are defined for this tier
        TierChance tc = tierChances.get(knowledgeTier);
        if (tc == null) {
            tc = tierChances.getOrDefault(0, new TierChance(1.0, 0.0));
        }

        double roll = random.nextDouble();

        double failChance = Math.max(0.0, 1.0 - tc.goodChance - tc.poorChance);
        double poorChance = Math.max(0.0, tc.poorChance);

        if (roll < failChance) {
            // Total failure: no item, red message
            player.sendMessage(
                    KnowledgeBoundTextFormatter.craftingFailSmithing(),
                    true
            );
            return ItemStack.EMPTY;
        }

        if (roll < failChance + poorChance) {
            // Poor quality item with reduced durability
            ItemStack poor = originalStack.copy();
            int maxDmg = poor.getMaxDamage();

            if (maxDmg > 0) {
                int remaining = Math.max(1, (int) Math.round(maxDmg * poorDurabilityFraction));
                int damage = maxDmg - remaining;
                poor.setDamage(damage);
            }

            // Cyan + purple "poor" quality line
            player.sendMessage(
                    KnowledgeBoundTextFormatter.craftingQualitySmithing("poor"),
                    true
            );
            return poor;
        }


        // Successful craft at full quality (no extra message)
        return originalStack;
    }
}
