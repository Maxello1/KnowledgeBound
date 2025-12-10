package net.maxello.knowledgebound;

import net.minecraft.util.Identifier;

import java.util.*;

public class KnowledgeRegistry {

    private static final Map<Identifier, KnowledgeDefinition> REGISTRY = new HashMap<>();

    // Public IDs for material/profession knowledges
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
    public static final Identifier WEAPONSMITHING_ID =
            new Identifier(KnowledgeBound.MOD_ID, "weaponsmithing");
    public static final Identifier ARMOURING_ID =
            new Identifier(KnowledgeBound.MOD_ID, "armouring");

    public static final Identifier RANGED_COMBAT_ID =
            new Identifier(KnowledgeBound.MOD_ID, "ranged_combat");
    public static final Identifier FISHING_ID =
            new Identifier(KnowledgeBound.MOD_ID, "fishing");

    public static final Identifier MELEE_COMBAT_ID =
            new Identifier(KnowledgeBound.MOD_ID, "melee_combat");

    public static void init() {
        KnowledgeBound.LOGGER.info("[KnowledgeBound] Registering knowledges…");

        register(createForestryDefinition());
        register(createMiningDefinition());
        register(createDiggingDefinition());
        register(createFarmingDefinition());

        register(createToolsmithingDefinition());
        register(createWeaponsmithingDefinition());
        register(createArmouringDefinition());

        register(createRangedCombatDefinition());
        register(createFishingDefinition());
        register(createMeleeCombatDefinition());
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
    //  Shared helpers
    // --------------------------------------------------

    private static Map<Integer, Integer> defaultMinutesPerTier() {
        Map<Integer, Integer> minutesPerTier = new HashMap<>();

        double m = KnowledgeBoundConfig.INSTANCE.minutesMultiplier;
        int[] base = {60, 120, 240, 480, 960}; // tiers 1–5

        for (int i = 0; i < base.length; i++) {
            int tier = i + 1;
            int value = (int) Math.round(base[i] * m);
            minutesPerTier.put(tier, Math.max(1, value));
        }

        return minutesPerTier;
    }

    private static Map<Integer, Set<KnowledgeDefinition.ToolTier>> defaultMaterialTierProgression() {
        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.WOOD));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.STONE));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.COPPER));
        xpToolTiers.put(3, EnumSet.of(KnowledgeDefinition.ToolTier.IRON));
        xpToolTiers.put(4, EnumSet.of(KnowledgeDefinition.ToolTier.DIAMOND));
        return xpToolTiers;
    }

    // --------------------------------------------------
    //  Forestry
    // --------------------------------------------------

    private static KnowledgeDefinition createForestryDefinition() {
        Identifier id = FORESTRY_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.SKILL;
        int maxTier = 5;

        Map<Integer, Integer> minutesPerTier = defaultMinutesPerTier();
        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = defaultMaterialTierProgression();
        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id, type, maxTier, minutesPerTier, xpToolTiers, xpActions
        );
    }

    // --------------------------------------------------
    //  Mining
    // --------------------------------------------------

    private static KnowledgeDefinition createMiningDefinition() {
        Identifier id = MINING_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.SKILL;
        int maxTier = 5;

        Map<Integer, Integer> minutesPerTier = defaultMinutesPerTier();
        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = defaultMaterialTierProgression();
        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id, type, maxTier, minutesPerTier, xpToolTiers, xpActions
        );
    }

    // --------------------------------------------------
    //  Digging
    // --------------------------------------------------

    private static KnowledgeDefinition createDiggingDefinition() {
        Identifier id = DIGGING_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.SKILL;
        int maxTier = 5;

        Map<Integer, Integer> minutesPerTier = defaultMinutesPerTier();
        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = defaultMaterialTierProgression();
        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id, type, maxTier, minutesPerTier, xpToolTiers, xpActions
        );
    }

    // --------------------------------------------------
    //  Farming
    // --------------------------------------------------

    private static KnowledgeDefinition createFarmingDefinition() {
        Identifier id = FARMING_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.SKILL;
        int maxTier = 5;

        Map<Integer, Integer> minutesPerTier = defaultMinutesPerTier();

        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.FIST, KnowledgeDefinition.ToolTier.WOOD));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.WOOD, KnowledgeDefinition.ToolTier.STONE));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.STONE, KnowledgeDefinition.ToolTier.COPPER));
        xpToolTiers.put(3, EnumSet.of(KnowledgeDefinition.ToolTier.COPPER, KnowledgeDefinition.ToolTier.IRON));
        xpToolTiers.put(4, EnumSet.of(KnowledgeDefinition.ToolTier.IRON, KnowledgeDefinition.ToolTier.DIAMOND));

        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id, type, maxTier, minutesPerTier, xpToolTiers, xpActions
        );
    }

    // --------------------------------------------------
    //  Toolsmithing
    // --------------------------------------------------

    private static KnowledgeDefinition createToolsmithingDefinition() {
        Identifier id = TOOLSMITHING_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.SKILL;
        int maxTier = 5;

        Map<Integer, Integer> minutesPerTier = defaultMinutesPerTier();
        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = defaultMaterialTierProgression();
        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id, type, maxTier, minutesPerTier, xpToolTiers, xpActions
        );
    }

    // --------------------------------------------------
    //  Weaponsmithing
    // --------------------------------------------------

    private static KnowledgeDefinition createWeaponsmithingDefinition() {
        Identifier id = WEAPONSMITHING_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.SKILL;
        int maxTier = 5;

        Map<Integer, Integer> minutesPerTier = defaultMinutesPerTier();
        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = defaultMaterialTierProgression();
        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id, type, maxTier, minutesPerTier, xpToolTiers, xpActions
        );
    }

    // --------------------------------------------------
    //  Armouring
    // --------------------------------------------------

    private static KnowledgeDefinition createArmouringDefinition() {
        Identifier id = ARMOURING_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.SKILL;
        int maxTier = 5;

        Map<Integer, Integer> minutesPerTier = defaultMinutesPerTier();

        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.LEATHER));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.CHAINMAIL));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.COPPER));
        xpToolTiers.put(3, EnumSet.of(KnowledgeDefinition.ToolTier.IRON));
        xpToolTiers.put(4, EnumSet.of(KnowledgeDefinition.ToolTier.DIAMOND));

        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id, type, maxTier, minutesPerTier, xpToolTiers, xpActions
        );
    }

    // --------------------------------------------------
    //  Ranged Combat
    // --------------------------------------------------

    private static KnowledgeDefinition createRangedCombatDefinition() {
        Identifier id = RANGED_COMBAT_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.SKILL;
        int maxTier = 5;

        Map<Integer, Integer> minutesPerTier = defaultMinutesPerTier();

        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.BOW));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.BOW, KnowledgeDefinition.ToolTier.CROSSBOW));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.CROSSBOW));
        xpToolTiers.put(3, EnumSet.of(KnowledgeDefinition.ToolTier.CROSSBOW));
        xpToolTiers.put(4, EnumSet.of(KnowledgeDefinition.ToolTier.CROSSBOW));

        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id, type, maxTier, minutesPerTier, xpToolTiers, xpActions
        );
    }

    // --------------------------------------------------
    //  Fishing
    // --------------------------------------------------

    private static KnowledgeDefinition createFishingDefinition() {
        Identifier id = FISHING_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.PROFESSION;
        int maxTier = 3;

        Map<Integer, Integer> minutesPerTier = new HashMap<>();
        minutesPerTier.put(1, (int) Math.round(60 * KnowledgeBoundConfig.INSTANCE.minutesMultiplier));
        minutesPerTier.put(2, (int) Math.round(120 * KnowledgeBoundConfig.INSTANCE.minutesMultiplier));
        minutesPerTier.put(3, (int) Math.round(240 * KnowledgeBoundConfig.INSTANCE.minutesMultiplier));

        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.FISHING_ROD));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.FISHING_ROD));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.FISHING_ROD));

        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id, type, maxTier, minutesPerTier, xpToolTiers, xpActions
        );
    }

    // --------------------------------------------------
    //  Melee Combat
    // --------------------------------------------------

    private static KnowledgeDefinition createMeleeCombatDefinition() {
        Identifier id = MELEE_COMBAT_ID;
        KnowledgeDefinition.Type type = KnowledgeDefinition.Type.SKILL;
        int maxTier = 5;

        Map<Integer, Integer> minutesPerTier = defaultMinutesPerTier();
        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = defaultMaterialTierProgression();
        List<KnowledgeDefinition.XpAction> xpActions = List.of();

        return new KnowledgeDefinition(
                id, type, maxTier, minutesPerTier, xpToolTiers, xpActions
        );
    }
}
