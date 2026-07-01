package net.maxello.knowledgebound.core;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * The big dictionary of all the specific knowledges in the mod.
 * 
 * We keep every KnowledgeDefinition here so we can easily look them up by ID
 * when a player mines a block, crafts an item, or levels up.
 * 
 * This class also serves as the hardcoded initialization point for all our base
 * knowledges (Forestry, Mining, Toolsmithing, etc.). In a perfect world, we might
 * data-drive this with JSON, but for now, they are defined in code.
 */
public class KnowledgeRegistry {

    // The actual map storing our definitions.
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

        // We manually create and register every single knowledge here.
        // gathering
        register(createForestryDefinition());
        register(createMiningDefinition());
        register(createDiggingDefinition());
        register(createFarmingDefinition());

        // material jobs (the 5-tier crafting ones)
        register(createToolsmithingDefinition());
        register(createWeaponsmithingDefinition());
        register(createArmouringDefinition());

        // combat (weapons and bows)
        register(createRangedCombatDefinition());
        register(createMeleeCombatDefinition());

        // fishing
        register(createFishingDefinition());

        // class jobs (the 3-tier specialty ones)
        register(createCarpentryDefinition());
        register(createMasonryDefinition());
        register(createBeekeepingDefinition());

        // supervised jobs (furnace/cooking stuff)
        register(createSmeltingDefinition());
        register(createCookingDefinition());

        // husbandry (animals and riding)
        register(createHusbandryDefinition());

        // jeweller (rings and baubles maybe?)
        register(createJewellerDefinition());

        // slaughtering (better drops and dissecting)
        register(createSlaughteringDefinition());
    }

    private static void register(KnowledgeDefinition def) {
        REGISTRY.put(def.getId(), def);
    }

    /** Look up a knowledge by its ID. Returns null if it doesn't exist. */
    public static KnowledgeDefinition get(Identifier id) {
        return REGISTRY.get(id);
    }

    /** Grab all of them. Useful for syncing data to the client or looping over everything. */
    public static Collection<KnowledgeDefinition> all() {
        return REGISTRY.values();
    }

    // --------------------------------------------------
    //  helpers
    // --------------------------------------------------

    /**
     * Builds the standard "minutes per tier" progression map used by most 5-tier jobs.
     * It pulls the base values from the config and multiplies them by the global multiplier.
     */
    private static Map<Integer, Integer> defaultMinutesPerTier() {
        Map<Integer, Integer> minutesPerTier = new HashMap<>();

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        double m = cfg.minutesMultiplier;
        int[] base = cfg.baseMinutesPerTier;

        for (int i = 0; i < base.length; i++) {
            int tier = i + 1;
            int value = (int) Math.round(base[i] * m);
            // We clamp to at least 1 minute. It doesn't make sense to require 0 minutes to level up.
            minutesPerTier.put(tier, Math.max(1, value));
        }

        return minutesPerTier;
    }

    /**
     * Same as defaultMinutesPerTier, but uses the base times specifically designated
     * for 3-tier class jobs (like Carpentry) from the config.
     */
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

    /**
     * The standard progression of tool tiers for most gathering and crafting jobs.
     * Basically: Tier 0 needs Wood. Tier 1 needs Stone. Tier 2 needs Copper, etc.
     */
    private static Map<Integer, Set<KnowledgeDefinition.ToolTier>> defaultMaterialTierProgression() {
        Map<Integer, Set<KnowledgeDefinition.ToolTier>> xpToolTiers = new HashMap<>();
        xpToolTiers.put(0, EnumSet.of(KnowledgeDefinition.ToolTier.WOOD));
        xpToolTiers.put(1, EnumSet.of(KnowledgeDefinition.ToolTier.STONE));
        xpToolTiers.put(2, EnumSet.of(KnowledgeDefinition.ToolTier.COPPER));
        xpToolTiers.put(3, EnumSet.of(KnowledgeDefinition.ToolTier.IRON));
        xpToolTiers.put(4, EnumSet.of(KnowledgeDefinition.ToolTier.DIAMOND));
        return xpToolTiers;
    }

    /**
     * Some jobs (like Class jobs) grant XP purely by the act of crafting a block,
     * not by using a specific tool. So we return an empty map for their tool tiers.
     */
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



