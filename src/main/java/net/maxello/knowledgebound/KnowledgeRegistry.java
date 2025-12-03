package net.maxello.knowledgebound;

import net.maxello.knowledgebound.KnowledgeBound;
import net.minecraft.util.Identifier;

import java.util.*;

public class KnowledgeRegistry {

    private static final Map<Identifier, KnowledgeDefinition> REGISTRY = new HashMap<>();

    public static final Identifier FORESTRY_ID = new Identifier(KnowledgeBound.MOD_ID, "forestry");
    public static final Identifier TOOLSMITHING_ID = new Identifier(KnowledgeBound.MOD_ID, "toolsmithing");
    // TODO: add mining, farming, etc.

    public static void init() {
        KnowledgeBound.LOGGER.info("[KnowledgeBound] Registering knowledges…");

        register(createForestryDefinition());
        register(createToolsmithingDefinition());
    }

    private static void register(KnowledgeDefinition def) {
        REGISTRY.put(def.getId(), def);
    }

    public static KnowledgeDefinition get(Identifier id) {
        return REGISTRY.get(id);
    }

    public static Collection<KnowledgeDefinition> all() {
        return REGISTRY.values();
    }

    private static KnowledgeDefinition createForestryDefinition() {
        Map<Integer, Integer> tierMinutes = new HashMap<>();
        // These are example values based on your wiki data
        tierMinutes.put(1, 60);   // Tier 0 -> 1}
        tierMinutes.put(2, 120);  // Tier 1 -> 2
        tierMinutes.put(3, 180);  // guessed
        tierMinutes.put(4, 240);  // guessed

        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, Set.of(KnowledgeDefinition.ToolTier.WOOD));
        xpToolTiers.put(1, Set.of(KnowledgeDefinition.ToolTier.STONE));
        xpToolTiers.put(2, Set.of(KnowledgeDefinition.ToolTier.COPPER));
        xpToolTiers.put(3, Set.of(KnowledgeDefinition.ToolTier.IRON));
        xpToolTiers.put(4, Set.of(KnowledgeDefinition.ToolTier.DIAMOND));

        List<KnowledgeDefinition.XpAction> xpActions = List.of(
                new KnowledgeDefinition.XpAction(List.of(
                        new Identifier("minecraft", "oak_log"),
                        new Identifier("minecraft", "spruce_log"),
                        new Identifier("minecraft", "birch_log")
                        // TODO: add more logs
                ))
        );

        return new KnowledgeDefinition(
                FORESTRY_ID,
                KnowledgeDefinition.Type.SKILL,
                4,
                tierMinutes,
                xpToolTiers,
                xpActions
        );
    }

    private static KnowledgeDefinition createToolsmithingDefinition() {
        Map<Integer, Integer> tierMinutes = new HashMap<>();
        tierMinutes.put(1, 60);
        tierMinutes.put(2, 120);
        tierMinutes.put(3, 180);
        tierMinutes.put(4, 240);

        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, Set.of(KnowledgeDefinition.ToolTier.WOOD));
        xpToolTiers.put(1, Set.of(KnowledgeDefinition.ToolTier.STONE));
        xpToolTiers.put(2, Set.of(KnowledgeDefinition.ToolTier.COPPER));
        xpToolTiers.put(3, Set.of(KnowledgeDefinition.ToolTier.IRON));
        xpToolTiers.put(4, Set.of(KnowledgeDefinition.ToolTier.DIAMOND));

        List<KnowledgeDefinition.XpAction> xpActions = List.of(
                // For toolsmithing you might later tie this to anvils / smithing tables
                new KnowledgeDefinition.XpAction(List.of())
        );

        return new KnowledgeDefinition(
                TOOLSMITHING_ID,
                KnowledgeDefinition.Type.SKILL,
                4,
                tierMinutes,
                xpToolTiers,
                xpActions
        );
    }
}
