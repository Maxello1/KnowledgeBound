package net.maxello.knowledgebound;

import net.minecraft.util.Identifier;

import java.util.*;

public class KnowledgeRegistry {

    private static final Map<Identifier, KnowledgeDefinition> REGISTRY = new HashMap<>();

    // gathering
    public static final Identifier FORESTRY_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "forestry");
    public static final Identifier MINING_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "mining");
    public static final Identifier DIGGING_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "digging");
    public static final Identifier FARMING_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "farming");

    // material jobs (5-tier)
    public static final Identifier TOOLSMITHING_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "toolsmithing");
    public static final Identifier WEAPONSMITHING_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "weaponsmithing");
    public static final Identifier ARMOURING_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "armouring");

    // combat
    public static final Identifier RANGED_COMBAT_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "ranged_combat");
    public static final Identifier MELEE_COMBAT_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "melee_combat");

    // fishing
    public static final Identifier FISHING_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "fishing");

    // class jobs (3-tier)
    public static final Identifier CARPENTRY_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "carpentry");
    public static final Identifier MASONRY_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "masonry");
    public static final Identifier BEEKEEPING_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "beekeeping");

    // supervised jobs (3-tier)
    public static final Identifier SMELTING_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "smelting");
    public static final Identifier COOKING_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "cooking");

    // husbandry (3-tier)
    public static final Identifier HUSBANDRY_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "husbandry");

    // jeweller (3-tier)
    public static final Identifier JEWELLER_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "jeweller");

    // slaughtering (3-tier)
    public static final Identifier SLAUGHTERING_ID =
            Identifier.of(KnowledgeBound.MOD_ID, "slaughtering");

    public static void init() {
        KnowledgeBound.LOGGER.info("[KnowledgeBound] Registering knowledges…");

        // gathering
        register(createForestryDefinition());
        register(createMiningDefinition());
        register(createDiggingDefinition());
        register(createFarmingDefinition());

        // material jobs
        register(createToolsmithingDefinition());
        register(createWeaponsmithingDefinition());
        register(createArmouringDefinition());

        // combat
        register(createRangedCombatDefinition());
        register(createMeleeCombatDefinition());

        // fishing
        register(createFishingDefinition());

        // class jobs
        register(createCarpentryDefinition());
        register(createMasonryDefinition());
        register(createBeekeepingDefinition());

        // supervised jobs
        register(createSmeltingDefinition());
        register(createCookingDefinition());

        // husbandry
        register(createHusbandryDefinition());

        // jeweller
        register(createJewellerDefinition());

        // slaughtering
        register(createSlaughteringDefinition());
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
    //  helpers
    // --------------------------------------------------

    private static Map<Integer, Integer> defaultMinutesPerTier() {
        Map<Integer, Integer> minutesPerTier = new HashMap<>();

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        double m = cfg.minutesMultiplier;
        int[] base = cfg.baseMinutesPerTier;

        for (int i = 0; i < base.length; i++) {
            int tier = i + 1;
            int value = (int) Math.round(base[i] * m);
            minutesPerTier.put(tier, Math.max(1, value));
        }

        return minutesPerTier;
    }

    private static Map<Integer, Integer> classJobMinutesPerTier() {
        Map<Integer, Integer> minutesPerTier = new HashMap<>();

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        double m = cfg.minutesMultiplier;
        int[] base = cfg.classJobBaseMinutes;

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

    // empty tool tier map for class jobs (xp granted by crafting, not by tool)
    private static Map<Integer, Set<KnowledgeDefinition.ToolTier>> noToolTiers() {
        return new HashMap<>();
    }

    // --------------------------------------------------
    //  Forestry
    // --------------------------------------------------

    private static KnowledgeDefinition createForestryDefinition() {
        return new KnowledgeDefinition(
                FORESTRY_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.GATHERING,
                5,
                defaultMinutesPerTier(),
                defaultMaterialTierProgression(),
                List.of()
        );
    }

    // --------------------------------------------------
    //  Mining
    // --------------------------------------------------

    private static KnowledgeDefinition createMiningDefinition() {
        return new KnowledgeDefinition(
                MINING_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.GATHERING,
                5,
                defaultMinutesPerTier(),
                defaultMaterialTierProgression(),
                List.of()
        );
    }

    // --------------------------------------------------
    //  Digging
    // --------------------------------------------------

    private static KnowledgeDefinition createDiggingDefinition() {
        return new KnowledgeDefinition(
                DIGGING_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.GATHERING,
                5,
                defaultMinutesPerTier(),
                defaultMaterialTierProgression(),
                List.of()
        );
    }

    // --------------------------------------------------
    //  Farming
    // --------------------------------------------------

    private static KnowledgeDefinition createFarmingDefinition() {
        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.FIST, KnowledgeDefinition.ToolTier.WOOD));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.WOOD, KnowledgeDefinition.ToolTier.STONE));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.STONE, KnowledgeDefinition.ToolTier.COPPER));
        xpToolTiers.put(3, EnumSet.of(KnowledgeDefinition.ToolTier.COPPER, KnowledgeDefinition.ToolTier.IRON));
        xpToolTiers.put(4, EnumSet.of(KnowledgeDefinition.ToolTier.IRON, KnowledgeDefinition.ToolTier.DIAMOND));

        return new KnowledgeDefinition(
                FARMING_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.GATHERING,
                5,
                defaultMinutesPerTier(),
                xpToolTiers,
                List.of()
        );
    }

    // --------------------------------------------------
    //  Toolsmithing
    // --------------------------------------------------

    private static KnowledgeDefinition createToolsmithingDefinition() {
        return new KnowledgeDefinition(
                TOOLSMITHING_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.MATERIAL_5_TIER,
                5,
                defaultMinutesPerTier(),
                defaultMaterialTierProgression(),
                List.of()
        );
    }

    // --------------------------------------------------
    //  Weaponsmithing
    // --------------------------------------------------

    private static KnowledgeDefinition createWeaponsmithingDefinition() {
        return new KnowledgeDefinition(
                WEAPONSMITHING_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.MATERIAL_5_TIER,
                5,
                defaultMinutesPerTier(),
                defaultMaterialTierProgression(),
                List.of()
        );
    }

    // --------------------------------------------------
    //  Armouring
    // --------------------------------------------------

    private static KnowledgeDefinition createArmouringDefinition() {
        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.LEATHER));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.CHAINMAIL));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.COPPER));
        xpToolTiers.put(3, EnumSet.of(KnowledgeDefinition.ToolTier.IRON));
        xpToolTiers.put(4, EnumSet.of(KnowledgeDefinition.ToolTier.DIAMOND));

        return new KnowledgeDefinition(
                ARMOURING_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.MATERIAL_5_TIER,
                5,
                defaultMinutesPerTier(),
                xpToolTiers,
                List.of()
        );
    }

    // --------------------------------------------------
    //  Ranged Combat
    // --------------------------------------------------

    private static KnowledgeDefinition createRangedCombatDefinition() {
        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.BOW));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.BOW, KnowledgeDefinition.ToolTier.CROSSBOW));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.CROSSBOW));
        xpToolTiers.put(3, EnumSet.of(KnowledgeDefinition.ToolTier.CROSSBOW));
        xpToolTiers.put(4, EnumSet.of(KnowledgeDefinition.ToolTier.CROSSBOW));

        return new KnowledgeDefinition(
                RANGED_COMBAT_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.COMBAT,
                5,
                defaultMinutesPerTier(),
                xpToolTiers,
                List.of()
        );
    }

    // --------------------------------------------------
    //  Fishing
    // --------------------------------------------------

    private static KnowledgeDefinition createFishingDefinition() {
        Map<Integer, Integer> minutesPerTier = new HashMap<>();
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        int[] fishBase = cfg.fishingBaseMinutes;
        double m = cfg.minutesMultiplier;
        for (int i = 0; i < fishBase.length; i++) {
            minutesPerTier.put(i + 1, Math.max(1, (int) Math.round(fishBase[i] * m)));
        }

        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.FISHING_ROD));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.FISHING_ROD));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.FISHING_ROD));

        return new KnowledgeDefinition(
                FISHING_ID,
                KnowledgeDefinition.Type.PROFESSION,
                KnowledgeDefinition.JobCategory.GATHERING,
                3,
                minutesPerTier,
                xpToolTiers,
                List.of()
        );
    }

    // --------------------------------------------------
    //  Melee Combat
    // --------------------------------------------------

    private static KnowledgeDefinition createMeleeCombatDefinition() {
        return new KnowledgeDefinition(
                MELEE_COMBAT_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.COMBAT,
                5,
                defaultMinutesPerTier(),
                defaultMaterialTierProgression(),
                List.of()
        );
    }

    // --------------------------------------------------
    //  Carpentry (class job, 3 tiers)
    // --------------------------------------------------

    private static KnowledgeDefinition createCarpentryDefinition() {
        return new KnowledgeDefinition(
                CARPENTRY_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.CLASS_3_TIER,
                3,
                classJobMinutesPerTier(),
                noToolTiers(),
                List.of()
        );
    }

    // --------------------------------------------------
    //  Masonry (class job, 3 tiers)
    // --------------------------------------------------

    private static KnowledgeDefinition createMasonryDefinition() {
        return new KnowledgeDefinition(
                MASONRY_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.CLASS_3_TIER,
                3,
                classJobMinutesPerTier(),
                noToolTiers(),
                List.of()
        );
    }

    // --------------------------------------------------
    //  Beekeeping (class job, 3 tiers)
    // --------------------------------------------------

    private static KnowledgeDefinition createBeekeepingDefinition() {
        return new KnowledgeDefinition(
                BEEKEEPING_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.CLASS_3_TIER,
                3,
                classJobMinutesPerTier(),
                noToolTiers(),
                List.of()
        );
    }

    // --------------------------------------------------
    //  Smelting (supervised job, 3 tiers)
    // --------------------------------------------------

    private static KnowledgeDefinition createSmeltingDefinition() {
        Map<Integer, Integer> minutesPerTier = new HashMap<>();
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        double m = cfg.minutesMultiplier;
        int[] base = cfg.smeltingBaseMinutes;
        for (int i = 0; i < base.length; i++) {
            minutesPerTier.put(i + 1, Math.max(1, (int) Math.round(base[i] * m)));
        }

        return new KnowledgeDefinition(
                SMELTING_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.CLASS_3_TIER,
                3,
                minutesPerTier,
                noToolTiers(),
                List.of()
        );
    }

    // --------------------------------------------------
    //  Cooking (supervised job, 3 tiers)
    // --------------------------------------------------

    private static KnowledgeDefinition createCookingDefinition() {
        return new KnowledgeDefinition(
                COOKING_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.CLASS_3_TIER,
                3,
                classJobMinutesPerTier(),
                noToolTiers(),
                List.of()
        );
    }

    // --------------------------------------------------
    //  Husbandry (class job, 3 tiers)
    // --------------------------------------------------

    private static KnowledgeDefinition createHusbandryDefinition() {
        return new KnowledgeDefinition(
                HUSBANDRY_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.CLASS_3_TIER,
                3,
                classJobMinutesPerTier(),
                noToolTiers(),
                List.of()
        );
    }

    // --------------------------------------------------
    //  Jeweller (class job, 3 tiers)
    // --------------------------------------------------

    private static KnowledgeDefinition createJewellerDefinition() {
        return new KnowledgeDefinition(
                JEWELLER_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.CLASS_3_TIER,
                3,
                classJobMinutesPerTier(),
                noToolTiers(),
                List.of()
        );
    }

    // --------------------------------------------------
    //  Slaughtering (class job, 3 tiers)
    // --------------------------------------------------

    private static KnowledgeDefinition createSlaughteringDefinition() {
        Map<Integer, Integer> minutesPerTier = new HashMap<>();
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        double m = cfg.minutesMultiplier;
        int[] base = cfg.slaughteringBaseMinutes;
        for (int i = 0; i < base.length; i++) {
            minutesPerTier.put(i + 1, Math.max(1, (int) Math.round(base[i] * m)));
        }

        return new KnowledgeDefinition(
                SLAUGHTERING_ID,
                KnowledgeDefinition.Type.SKILL,
                KnowledgeDefinition.JobCategory.CLASS_3_TIER,
                3,
                minutesPerTier,
                noToolTiers(),
                List.of()
        );
    }
}
