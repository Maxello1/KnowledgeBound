package net.maxello.knowledgebound;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Random;

/**
 * Handles crafting outcomes based on the diff between player tier and item tier.
 * Material jobs hard-block tier jumps (100% fail if diff < 0).
 * Class jobs can jump tiers using the standard diff table.
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
     */
    public ItemStack apply(ServerPlayerEntity player,
                           Identifier itemId,
                           ItemStack originalStack,
                           int knowledgeTier) {

        // get required tier
        int itemTier = CraftingRuleRegistry.getItemTier(itemId);
        int diff = knowledgeTier - itemTier;

        // check if this is a material job
        KnowledgeDefinition def = KnowledgeRegistry.get(knowledgeId);
        boolean isMaterialJob = def != null &&
                def.getJobCategory() == KnowledgeDefinition.JobCategory.MATERIAL_5_TIER;

        // material jobs can't jump tiers at all
        if (isMaterialJob && diff < 0) {
            player.sendMessage(
                    KnowledgeBoundTextFormatter.craftingLevelTooLow(knowledgeId),
                    true
            );
            return ItemStack.EMPTY;
        }

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
                    KnowledgeBoundTextFormatter.craftingFail(knowledgeId),
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
                    KnowledgeBoundTextFormatter.craftingQuality(knowledgeId, "poor"),
                    true
            );
            return poor;
        }

        // normal craft
        return originalStack;
    }
}
