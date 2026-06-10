package net.maxello.knowledgebound;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.Identifier;

/**
 * Represents a single knowledge, like Forestry, Mining, Toolsmithing, Carpentry...
 */
public class KnowledgeDefinition {

    public enum Type {
        SKILL,
        PROFESSION
    }

    /**
     * Material jobs have 5 tiers and cannot jump tiers when crafting.
     * Class jobs have 3 tiers and can jump tiers (with reduced success).
     */
    public enum JobCategory {
        MATERIAL_5_TIER,
        CLASS_3_TIER,
        GATHERING,
        COMBAT
    }

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

    public static class XpAction {
        public final List<Identifier> blocks;

        public XpAction(List<Identifier> blocks) {
            this.blocks = blocks;
        }
    }

    private final Identifier id;
    private final Type type;
    private final JobCategory jobCategory;
    private final int maxTier;

    // minutes needed per tier (from previous tier to this tier)
    private final Map<Integer, Integer> tierMinutes;

    // which tool tiers can grant XP when progressing from tier N to N+1
    private final Map<Integer, Set<ToolTier>> xpToolTiers;

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

    public int getMinutesForTier(int tier) {
        return tierMinutes.getOrDefault(tier, 0);
    }

    public Set<ToolTier> getXpToolTiersFor(int currentTier) {
        return xpToolTiers.getOrDefault(currentTier, Set.of());
    }

    public List<XpAction> getXpActions() {
        return xpActions;
    }
}
