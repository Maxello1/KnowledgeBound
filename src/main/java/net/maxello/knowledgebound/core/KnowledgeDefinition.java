package net.maxello.knowledgebound.core;

import net.maxello.knowledgebound.KnowledgeBound;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.Identifier;

/**
 * Represents the fundamental blueprint for a specific "Knowledge" in the mod.
 * 
 * Think of this as the master data container for a skill or profession like Forestry, 
 * Mining, Toolsmithing, Carpentry, etc. It defines what type of knowledge it is,
 * how many tiers it has, how much time/XP it takes to reach the next tier,
 * and what kinds of tools or actions actually grant experience.
 */
public class KnowledgeDefinition {

    /**
     * Basic categorization. Is this a core skill that everyone needs, 
     * or a specialized profession that you pick up later?
     */
    public enum Type {
        SKILL,
        PROFESSION
    }

    /**
     * Determines the overarching "ruleset" the knowledge follows for progression and caps.
     * 
     * - MATERIAL_5_TIER: Jobs focused on raw material processing (like Toolsmithing). 
     *   They have 5 distinct tiers. You absolutely cannot jump tiers when crafting.
     * - CLASS_3_TIER: More specialized class jobs (like Carpentry). 
     *   They have 3 tiers, and you actually CAN jump tiers, though with a big success penalty.
     * - GATHERING: Resource collection like Mining or Forestry.
     * - COMBAT: Pretty self-explanatory. Melee and Ranged.
     */
    public enum JobCategory {
        MATERIAL_5_TIER,
        CLASS_3_TIER,
        GATHERING,
        COMBAT
    }

    /**
     * Represents the logical "tier" of a tool or item used for an action.
     * We map this out so the system knows that you can't get XP for diamond
     * tools if you're only a level 1 miner, for instance.
     */
    public enum ToolTier {
        FIST,
        WOOD,
        STONE,
        COPPER,
        IRON,
        DIAMOND,
        LEATHER,
        CHAINMAIL,
        BOW,
        CROSSBOW,
        FISHING_ROD,
        UNKNOWN
    }

    /**
     * Maps an action that grants XP. Right now we use this for defining which specific
     * blocks provide XP when broken.
     */
    public static class XpAction {
        public final List<Identifier> blocks;

        public XpAction(List<Identifier> blocks) {
            this.blocks = blocks;
        }
    }

    // Basic identity of this knowledge
    private final Identifier id;
    private final Type type;
    private final JobCategory jobCategory;
    private final int maxTier;

    /**
     * The XP cost map. 
     * Key = The tier you are trying to reach.
     * Value = The number of "minutes" (XP) required to get from the previous tier to this one.
     */
    private final Map<Integer, Integer> tierMinutes;

    /**
     * Defines what tools are actually capable of granting XP at a given tier.
     * If you are Tier 1, maybe you can only get XP using Stone tools. 
     * If you use Diamond at Tier 1, no XP for you!
     */
    private final Map<Integer, Set<ToolTier>> xpToolTiers;

    /**
     * Specific actions (like block breaking) that grant XP for this knowledge.
     */
    private final List<XpAction> xpActions;

    public KnowledgeDefinition(
            Identifier id,
            Type type,
            JobCategory jobCategory,
            int maxTier,
            Map<Integer, Integer> tierMinutes,
            Map<Integer, Set<ToolTier>> xpToolTiers,
            List<XpAction> xpActions
    ) {
        this.id = id;
        this.type = type;
        this.jobCategory = jobCategory;
        this.maxTier = maxTier;
        this.tierMinutes = tierMinutes;
        this.xpToolTiers = xpToolTiers;
        this.xpActions = xpActions;
    }

    public Identifier getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public JobCategory getJobCategory() {
        return jobCategory;
    }

    public int getMaxTier() {
        return maxTier;
    }

    /**
     * How many minutes of effort are needed to hit the target tier?
     * Returns 0 if the tier doesn't exist.
     */
    public int getMinutesForTier(int tier) {
        return tierMinutes.getOrDefault(tier, 0);
    }

    /**
     * What tools can I use at my current tier to actually gain XP?
     * Returns an empty set if no tool restrictions apply, or if it's purely crafting based.
     */
    public Set<ToolTier> getXpToolTiersFor(int currentTier) {
        return xpToolTiers.getOrDefault(currentTier, Set.of());
    }

    public List<XpAction> getXpActions() {
        return xpActions;
    }
}


