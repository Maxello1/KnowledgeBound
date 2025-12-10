package net.maxello.knowledgebound;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.Identifier;

/**
 * Represents a single material-based knowledge, like Forestry, Mining, Toolsmithing...
 */
public class KnowledgeDefinition {

    public enum Type {
        SKILL,
        PROFESSION
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
        // For now we keep it simple: blocks that count for XP in this knowledge.
        public final List<Identifier> blocks;

        public XpAction(List<Identifier> blocks) {
            this.blocks = blocks;
        }
    }

    private final Identifier id;
    private final Type type;
    private final int maxTier;

    // minutes needed per tier (from previous tier → this tier)
    private final Map<Integer, Integer> tierMinutes;

    // which tool tiers can grant XP when progressing from tier N to N+1
    private final Map<Integer, Set<ToolTier>> xpToolTiers;

    private final List<XpAction> xpActions;

    public KnowledgeDefinition(
            Identifier id,
            Type type,
            int maxTier,
            Map<Integer, Integer> tierMinutes,
            Map<Integer, Set<ToolTier>> xpToolTiers,
            List<XpAction> xpActions
    ) {
        this.id = id;
        this.type = type;
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
