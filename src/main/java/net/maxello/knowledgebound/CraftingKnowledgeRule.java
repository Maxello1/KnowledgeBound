package net.maxello.knowledgebound;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;

public class CraftingKnowledgeRule {

    public static class TierChance {
        public final double success;
        public final double poor;

        public TierChance(double success, double poor) {
            this.success = success;
            this.poor = poor;
        }
    }

    private final Identifier id;
    private final Identifier knowledgeId;
    private final double poorDurabilityFraction;
    private final Map<Integer, TierChance> tierChances;

    public CraftingKnowledgeRule(
            Identifier id,
            Identifier knowledgeId,
            double poorDurabilityFraction,
            Map<Integer, TierChance> tierChances
    ) {
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

    public TierChance getTierChance(int tier) {
        return tierChances.getOrDefault(tier, new TierChance(0.0, 0.0));
    }

    public double getPoorDurabilityFraction() {
        return poorDurabilityFraction;
    }

    /**
     * Decide if the craft succeeds, is poor quality, or fails.
     */
    public ItemStack apply(ServerPlayerEntity player,
                           Identifier recipeId,
                           ItemStack originalOutput,
                           int knowledgeTier) {

        TierChance tc = getTierChance(knowledgeTier);
        double r = player.getRandom().nextDouble();

        if (r < tc.success) {
            // normal craft
            return originalOutput;
        } else if (r < tc.success + tc.poor) {
            // poor-quality craft
            ItemStack poor = originalOutput.copy();
            int maxDur = poor.getMaxDamage();
            if (maxDur > 0) {
                int usable = Math.max(1, (int) Math.ceil(maxDur * poorDurabilityFraction));
                poor.setDamage(maxDur - usable);
            }
            return poor;
        } else {
            // full failure – no item
            return ItemStack.EMPTY;
        }
    }
}
