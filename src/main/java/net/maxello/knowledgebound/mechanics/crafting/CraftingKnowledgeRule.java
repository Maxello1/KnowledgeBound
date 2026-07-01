package net.maxello.knowledgebound.mechanics.crafting;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.util.KnowledgeBoundTextFormatter;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.KnowledgeRegistry;
import net.maxello.knowledgebound.core.KnowledgeDefinition;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Random;

/**
 * Handles what actually happens when a player takes an item out of the crafting grid.
 * 
 * We check if their tier is high enough. If it's a core material job (like blacksmithing),
 * they are strictly forbidden from crafting things above their tier. If it's a class job
 * (like Carpentry), they can try to craft it, but they might completely ruin the materials
 * or produce a "poor quality" version.
 */
public class CraftingKnowledgeRule {

    private final Identifier id;
    private final Identifier knowledgeId;
    
    // We don't actually use this variable currently because we pull from the config,
    // but it's here for legacy support in case we want specific rules to have custom damage ratios.
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
     * Intercepts the crafted item right before the player gets it.
     * Returns an empty stack if the craft fails, or a damaged stack if it's poor quality.
     */
    public ItemStack apply(ServerPlayerEntity player,
                           Identifier itemId,
                           ItemStack originalStack,
                           int knowledgeTier) {

        // Find out what tier this item actually is.
        int itemTier = CraftingRuleRegistry.getItemTier(itemId);
        
        // This is the most important calculation. 
        // If it's negative, the player is trying to craft above their weight class.
        int diff = knowledgeTier - itemTier;

        // Is this a strict material job? (Toolsmithing, Armouring, Weaponsmithing)
        KnowledgeDefinition def = KnowledgeRegistry.get(knowledgeId);
        boolean isMaterialJob = def != null &&
                def.getJobCategory() == KnowledgeDefinition.JobCategory.MATERIAL_5_TIER;

        // Material jobs absolutely cannot jump tiers. If you are a tier 1 smith,
        // you cannot craft a diamond sword. Period.
        if (isMaterialJob && diff < 0) {
            player.sendMessage(
                    KnowledgeBoundTextFormatter.craftingLevelTooLow(knowledgeId),
                    true // action bar
            );
            return ItemStack.EMPTY;
        }

        // For class jobs (like Carpentry), we look up the chances in the config.
        // It maps the tier difference to a failure/poor chance.
        KnowledgeBoundConfig.CraftingTierChances tc =
                KnowledgeBoundConfig.INSTANCE.getCraftingChancesForDiff(diff);
        tc.normalize(); // Ensure the math doesn't result in >100% chance

        double roll = random.nextDouble();

        double failChance = Math.max(0.0, tc.failChance);
        double poorChance = Math.max(0.0, tc.poorChance);

        // First we check if it's a total failure.
        if (roll < failChance) {
            // They messed up so badly that the materials are wasted and they get nothing.
            player.sendMessage(
                    KnowledgeBoundTextFormatter.craftingFail(knowledgeId),
                    true // action bar
            );
            return ItemStack.EMPTY;
        }

        // If it didn't fail completely, did they do a bad job?
        if (roll < failChance + poorChance) {
            // A "poor quality" craft means the item comes out heavily damaged.
            // But this obviously only works on items that actually HAVE durability (tools/armor).
            ItemStack poor = originalStack.copy();
            int maxDmg = poor.getMaxDamage();

            // If it's a damageable item...
            if (maxDmg > 0) {
                // Calculate how much durability they should be left with based on the config percentage.
                int remaining = Math.max(1, (int) Math.round(maxDmg * KnowledgeBoundConfig.INSTANCE.poorDurabilityFraction));
                
                // Damage the item. (Note: setDamage sets how much damage it has TAKEN, not how much is left)
                int damage = maxDmg - remaining;
                poor.setDamage(damage);

                player.sendMessage(
                        KnowledgeBoundTextFormatter.craftingQuality(knowledgeId, "poor"),
                        true // action bar
                );
                return poor;
            }
            // If it's not damageable (like crafting a bed or a bookshelf), it can't really
            // be "poor quality". We just let it succeed normally.
        }

        // The craft succeeded perfectly!
        player.sendMessage(
                KnowledgeBoundTextFormatter.craftingQuality(knowledgeId, "normal"),
                true // action bar
        );
        return originalStack;
    }
}



