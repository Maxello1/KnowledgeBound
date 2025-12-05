package net.maxello.knowledgebound;

import net.minecraft.util.Identifier;

import java.util.*;

public class KnowledgeRegistry {

    private static final Map<Identifier, KnowledgeDefinition> REGISTRY = new HashMap<>();

    // Public IDs for material knowledges
    public static final Identifier FORESTRY_ID =
            new Identifier(KnowledgeBound.MOD_ID, "forestry");
    public static final Identifier MINING_ID =
            new Identifier(KnowledgeBound.MOD_ID, "mining");
    public static final Identifier DIGGING_ID =
            new Identifier(KnowledgeBound.MOD_ID, "digging");
    public static final Identifier FARMING_ID =
            new Identifier(KnowledgeBound.MOD_ID, "farming");
    public static final Identifier TOOLSMITHING_ID =
            new Identifier(KnowledgeBound.MOD_ID, "toolsmithing");

    public static void init() {
        KnowledgeBound.LOGGER.info("[KnowledgeBound] Registering knowledges…");

        register(createForestryDefinition());
        register(createMiningDefinition());
        register(createDiggingDefinition());
        register(createFarmingDefinition());
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

    // --------------------------------------------------
    //  Forestry
    // --------------------------------------------------

    private static KnowledgeDefinition createForestryDefinition() {
        Identifier id = FORESTRY_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.SKILL;
        int maxTier = 5;

        Map<Integer, Integer> minutesPerTier = new HashMap<>();
        minutesPerTier.put(1, 60);
        minutesPerTier.put(2, 120);
        minutesPerTier.put(3, 240);
        minutesPerTier.put(4, 480);
        minutesPerTier.put(5, 960);

        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.WOOD));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.STONE));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.COPPER));
        xpToolTiers.put(3, EnumSet.of(KnowledgeDefinition.ToolTier.IRON));
        xpToolTiers.put(4, EnumSet.of(KnowledgeDefinition.ToolTier.DIAMOND));

        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id,
                type,
                maxTier,
                minutesPerTier,
                xpToolTiers,
                xpActions
        );
    }

    // --------------------------------------------------
    //  Mining
    // --------------------------------------------------

    private static KnowledgeDefinition createMiningDefinition() {
        Identifier id = MINING_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.SKILL;
        int maxTier = 5;

        Map<Integer, Integer> minutesPerTier = new HashMap<>();
        minutesPerTier.put(1, 60);
        minutesPerTier.put(2, 120);
        minutesPerTier.put(3, 240);
        minutesPerTier.put(4, 480);
        minutesPerTier.put(5, 960);

        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.WOOD));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.STONE));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.COPPER));
        xpToolTiers.put(3, EnumSet.of(KnowledgeDefinition.ToolTier.IRON));
        xpToolTiers.put(4, EnumSet.of(KnowledgeDefinition.ToolTier.DIAMOND));

        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id,
                type,
                maxTier,
                minutesPerTier,
                xpToolTiers,
                xpActions
        );
    }

    // --------------------------------------------------
    //  Digging (shovel blocks)
    // --------------------------------------------------

    private static KnowledgeDefinition createDiggingDefinition() {
        Identifier id = DIGGING_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.SKILL;
        int maxTier = 5;

        Map<Integer, Integer> minutesPerTier = new HashMap<>();
        minutesPerTier.put(1, 60);
        minutesPerTier.put(2, 120);
        minutesPerTier.put(3, 240);
        minutesPerTier.put(4, 480);
        minutesPerTier.put(5, 960);

        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        // Same tool progression as Mining
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.WOOD));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.STONE));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.COPPER));
        xpToolTiers.put(3, EnumSet.of(KnowledgeDefinition.ToolTier.IRON));
        xpToolTiers.put(4, EnumSet.of(KnowledgeDefinition.ToolTier.DIAMOND));

        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id,
                type,
                maxTier,
                minutesPerTier,
                xpToolTiers,
                xpActions
        );
    }

    // --------------------------------------------------
    //  Farming
    // --------------------------------------------------

    private static KnowledgeDefinition createFarmingDefinition() {
        Identifier id = FARMING_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.SKILL;
        int maxTier = 5;

        Map<Integer, Integer> minutesPerTier = new HashMap<>();
        minutesPerTier.put(1, 60);
        minutesPerTier.put(2, 120);
        minutesPerTier.put(3, 240);
        minutesPerTier.put(4, 480);
        minutesPerTier.put(5, 960);

        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.FIST, KnowledgeDefinition.ToolTier.WOOD));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.WOOD, KnowledgeDefinition.ToolTier.STONE));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.STONE, KnowledgeDefinition.ToolTier.COPPER));
        xpToolTiers.put(3, EnumSet.of(KnowledgeDefinition.ToolTier.COPPER, KnowledgeDefinition.ToolTier.IRON));
        xpToolTiers.put(4, EnumSet.of(KnowledgeDefinition.ToolTier.IRON, KnowledgeDefinition.ToolTier.DIAMOND));

        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id,
                type,
                maxTier,
                minutesPerTier,
                xpToolTiers,
                xpActions
        );
    }

    // --------------------------------------------------
    //  Toolsmithing
    // --------------------------------------------------

    private static KnowledgeDefinition createToolsmithingDefinition() {
        Identifier id = TOOLSMITHING_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.SKILL;
        int maxTier = 5;

        Map<Integer, Integer> minutesPerTier = new HashMap<>();
        minutesPerTier.put(1, 60);
        minutesPerTier.put(2, 120);
        minutesPerTier.put(3, 240);
        minutesPerTier.put(4, 480);
        minutesPerTier.put(5, 960);

        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.WOOD));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.STONE));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.COPPER));
        xpToolTiers.put(3, EnumSet.of(KnowledgeDefinition.ToolTier.IRON));
        xpToolTiers.put(4, EnumSet.of(KnowledgeDefinition.ToolTier.DIAMOND));

        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id,
                type,
                maxTier,
                minutesPerTier,
                xpToolTiers,
                xpActions
        );
    }
}
