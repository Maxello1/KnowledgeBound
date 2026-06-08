package net.maxello.knowledgebound;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KnowledgeBoundConfig {

    // Pretty JSON, but without HTML escaping so we don't get \u003d etc.
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static KnowledgeBoundConfig INSTANCE = new KnowledgeBoundConfig();

    // --------------------------------------------------
    // Top-level help text
    // --------------------------------------------------

    public List<String> _comment_header = List.of(
            "KnowledgeBound config file.",
            "You can change XP speed, fail chances, crafting quality and armor requirements here.",
            "All values are safe to edit. If you break something, delete this file and it will regenerate."
    );

    // --------------------------------------------------
    // Global XP tuning
    // --------------------------------------------------

    public List<String> _comment_xp = List.of(
            "XP progression settings:",
            "- baseMinutesPerTier: real-time minutes needed for each tier BEFORE applying minutesMultiplier.",
            "  Index 0 = Tier 1, index 1 = Tier 2, etc.",
            "- minutesMultiplier: scales all minutes. Example: 2.0 = twice as slow, 0.5 = twice as fast."
    );

    /**
     * Base minutes required per tier (before the multiplier).
     * Index 0 = Tier 1, index 1 = Tier 2, etc.
     * Default: [60, 120, 240, 480, 960]
     */
    public int[] baseMinutesPerTier = new int[] { 60, 120, 240, 480, 960 };

    /**
     * Multiplier for minutes required per tier
     * (1.0 = default, 2.0 = twice as slow, 0.5 = twice as fast).
     */
    public double minutesMultiplier = 1.0;

    // --------------------------------------------------
    // Combat damage scaling
    // --------------------------------------------------

    public List<String> _comment_damageScale = List.of(
            "Combat damage scaling per knowledge tier.",
            "Each index maps to a combat tier (0..5).",
            "Value 1.0 = full damage, 0.5 = half damage, etc.",
            "Applies to both melee and ranged combat."
    );

    /**
     * Damage multiplier per combat tier.
     * Index 0 = tier 0, index 5 = tier 5.
     */
    public double[] combatDamageScale = new double[] { 0.40, 0.55, 0.70, 0.85, 1.0, 1.0 };

    // --------------------------------------------------
    // Gather failure chances (Forestry, Mining, Digging, Farming)
    // --------------------------------------------------

    public List<String> _comment_gatherFail = List.of(
            "Gather fail chances:",
            "If a gather attempt fails, the block still breaks but drops NOTHING.",
            "Values are between 0.0 and 1.0 (0% to 100%).",
            "Each knowledge has a set of chances per tier:",
            "  tier0 = before you know anything",
            "  tier1, tier2, tier3, tier4 = higher knowledge tiers."
    );

    /**
     * Chance per tier that a gather action (block break) yields no drops.
     * Tier index 0..4 maps to knowledge tier 0..4.
     */
    public GatherFailConfig forestryGatherFail = new GatherFailConfig(0.40, 0.25, 0.10, 0.05, 0.02);
    public GatherFailConfig miningGatherFail   = new GatherFailConfig(0.40, 0.25, 0.10, 0.05, 0.02);
    public GatherFailConfig diggingGatherFail  = new GatherFailConfig(0.40, 0.25, 0.10, 0.05, 0.02);
    public GatherFailConfig farmingGatherFail  = new GatherFailConfig(0.30, 0.20, 0.10, 0.05, 0.02);

    public static class GatherFailConfig {
        /** Fail chance at tier 0,1,2,3,4 (0-1 range). */
        public double tier0;
        public double tier1;
        public double tier2;
        public double tier3;
        public double tier4;

        public GatherFailConfig() {
            // no-arg constructor for Gson
        }

        public GatherFailConfig(double t0, double t1, double t2, double t3, double t4) {
            this.tier0 = t0;
            this.tier1 = t1;
            this.tier2 = t2;
            this.tier3 = t3;
            this.tier4 = t4;
        }

        public double getForTier(int tier) {
            int clamped = Math.max(0, Math.min(tier, 4));
            return switch (clamped) {
                case 0 -> tier0;
                case 1 -> tier1;
                case 2 -> tier2;
                case 3 -> tier3;
                default -> tier4;
            };
        }
    }

    // --------------------------------------------------
    // Crafting chances (tier-difference based)
    // --------------------------------------------------

    public List<String> _comment_crafting = List.of(
            "Crafting chances are based on the DIFFERENCE between your knowledge tier and the item's tier.",
            "diff = playerKnowledgeTier - itemTier",
            "Array entries (6 total):",
            "  [0] = diff <= -3  (way out of your league)",
            "  [1] = diff  = -2  (very risky)",
            "  [2] = diff  = -1  (challenging)",
            "  [3] = diff  =  0  (at your level)",
            "  [4] = diff  = +1  (below your skill)",
            "  [5] = diff >= +2  (trivial)",
            "Inside each entry:",
            "  failChance   = chance to get NO item (ingredients lost)",
            "  poorChance   = chance to get a POOR quality item (10% durability)",
            "  normalChance = chance to get a NORMAL item (full durability)",
            "The values are automatically normalized if they don't sum to 1.0."
    );

    /**
     * Crafting chances indexed by tier difference.
     * Index 0 = diff <= -3, index 5 = diff >= +2.
     */
    public CraftingTierChances[] craftingDiffChances = defaultCraftingDiffChances();

    public static class CraftingTierChances {
        /** Chance the craft completely fails (no output item). */
        public double failChance;
        /** Chance the craft is "poor quality" (e.g. 10% durability). */
        public double poorChance;
        /** Chance the craft is normal (full durability). */
        public double normalChance;

        public CraftingTierChances() {
            // no-arg constructor for Gson
        }

        public CraftingTierChances(double fail, double poor, double normal) {
            this.failChance = fail;
            this.poorChance = poor;
            this.normalChance = normal;
        }

        /**
         * Normalize so fail+poor+normal == 1.0.
         * Called before using the chances, so you don't have to be perfect.
         */
        public void normalize() {
            double sum = failChance + poorChance + normalChance;
            if (sum <= 0.0) {
                failChance = 0.0;
                poorChance = 0.0;
                normalChance = 1.0;
            } else {
                failChance   /= sum;
                poorChance   /= sum;
                normalChance /= sum;
            }
        }
    }

    private static CraftingTierChances[] defaultCraftingDiffChances() {
        return new CraftingTierChances[] {
                new CraftingTierChances(1.00, 0.00, 0.00), // diff <= -3: impossible
                new CraftingTierChances(0.85, 0.12, 0.03), // diff  = -2: very risky
                new CraftingTierChances(0.45, 0.35, 0.20), // diff  = -1: challenging
                new CraftingTierChances(0.10, 0.15, 0.75), // diff  =  0: at your level
                new CraftingTierChances(0.00, 0.08, 0.92), // diff  = +1: below your skill
                new CraftingTierChances(0.00, 0.00, 1.00)  // diff >= +2: trivial
        };
    }

    /**
     * Looks up the crafting chances for a given tier difference.
     * diff = playerKnowledgeTier - itemTier
     */
    public CraftingTierChances getCraftingChancesForDiff(int diff) {
        // Map diff to array index: <= -3 -> 0, -2 -> 1, -1 -> 2, 0 -> 3, +1 -> 4, >= +2 -> 5
        int index;
        if (diff <= -3) index = 0;
        else if (diff == -2) index = 1;
        else if (diff == -1) index = 2;
        else if (diff == 0) index = 3;
        else if (diff == 1) index = 4;
        else index = 5; // diff >= +2

        if (index >= 0 && index < craftingDiffChances.length) {
            return craftingDiffChances[index];
        }
        // Fallback: guaranteed normal
        return new CraftingTierChances(0.0, 0.0, 1.0);
    }

    public List<String> _comment_itemTiers = List.of(
            "Per-item crafting tier overrides.",
            "Key: full item id (e.g. 'minecraft:iron_sword' or 'modid:custom_pickaxe').",
            "Value: the item's required crafting tier (0-4).",
            "Vanilla items have built-in defaults; use this for modded items."
    );

    /** Per-item tier overrides (e.g. "modid:custom_sword" -> 3). */
    public Map<String, Integer> itemCraftingTierOverrides = new HashMap<>();

    // --------------------------------------------------
    // Armor equip restrictions (tier per material / item)
    // --------------------------------------------------

    public List<String> _comment_armor = List.of(
            "Armor equip restrictions:",
            "- Your usable armor is based on your Combat Knowledge (highest of Melee + Ranged).",
            "- You can change which combat tier is required for each armor material.",
            "- You can also assign specific items (including modded armor) to a required tier."
    );

    public ArmorTierConfig armorTiers = new ArmorTierConfig();

    public static class ArmorTierConfig {

        public List<String> _comment_materials = List.of(
                "Base required combat tier per vanilla armor material.",
                "Typical progression:",
                "  Leather   -> tier 0",
                "  Chainmail -> tier 1",
                "  Iron      -> tier 2",
                "  Gold      -> tier 3",
                "  Diamond   -> tier 4",
                "  Netherite -> tier 5"
        );

        /**
         * Base required combat tier per vanilla armor material.
         */
        public int leatherTier   = 0;
        public int chainTier     = 1;
        public int ironTier      = 2;
        public int goldTier      = 3;
        public int diamondTier   = 4;
        public int netheriteTier = 5;

        public List<String> _comment_extraItems = List.of(
                "Per-item overrides for required combat tier.",
                "Key:  full item id, e.g. \"minecraft:turtle_helmet\" or \"modid:super_chestplate\"",
                "Value: required combat tier (0 = leather-level, 5 = netherite-level, etc.)."
        );

        /**
         * Per-item overrides for required tier.
         * Key: full item id string, e.g. "minecraft:turtle_helmet" or "modid:super_armor_chestplate"
         * Value: required combat tier (0..5 or more if you want).
         */
        public Map<String, Integer> extraItemTiers = new HashMap<>();
    }

    // --------------------------------------------------
    // Existing block / item extension lists
    // --------------------------------------------------

    public List<String> _comment_blocks = List.of(
            "Extra blocks that should count for the respective gather knowledges.",
            "Use full block IDs like \"modid:my_ore_block\" or \"modid:my_custom_log\"."
    );

    /** Extra block IDs that should count for Forestry XP (e.g. "mytreesmod:ancient_log"). */
    public List<String> extraForestryBlocks = new ArrayList<>();
    public List<String> extraMiningBlocks   = new ArrayList<>();
    public List<String> extraDiggingBlocks  = new ArrayList<>();
    public List<String> extraFarmingBlocks  = new ArrayList<>();

    // --------------------------------------------------
    // Fishing base minutes
    // --------------------------------------------------

    public List<String> _comment_fishing = List.of(
            "Base minutes for fishing tiers (before minutesMultiplier).",
            "Fishing only has 3 tiers, so this array should have 3 entries."
    );

    /** Base minutes for fishing tiers 1, 2, 3 (before multiplier). */
    public int[] fishingBaseMinutes = new int[] { 60, 120, 240 };

    public List<String> _comment_items = List.of(
            "Extra items that should behave like vanilla tools/armor in crafting quality rules.",
            "Use full item IDs like \"modid:my_wooden_sword\" or \"modid:my_iron_helmet\"."
    );

    /** Extra item IDs that should use the toolsmithing rule. */
    public List<String> extraToolItems   = new ArrayList<>();
    /** Extra item IDs that should use the armor rule. */
    public List<String> extraArmorItems  = new ArrayList<>();
    /** Extra item IDs that should use the weaponsmithing rule. */
    public List<String> extraWeaponItems = new ArrayList<>();

    // --------------------------------------------------
    // Load / save
    // --------------------------------------------------

    public static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path path = configDir.resolve("knowledgebound.json");

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                INSTANCE = GSON.fromJson(reader, KnowledgeBoundConfig.class);
                KnowledgeBound.LOGGER.info("[KnowledgeBound] Loaded config from {}", path);
            } catch (IOException e) {
                KnowledgeBound.LOGGER.error("[KnowledgeBound] Failed to load config, using defaults.", e);
            }
        } else {
            // Create default config file
            try {
                Files.createDirectories(configDir);
                try (Writer writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(INSTANCE, writer);
                }
                KnowledgeBound.LOGGER.info("[KnowledgeBound] Created default config at {}", path);
            } catch (IOException e) {
                KnowledgeBound.LOGGER.error("[KnowledgeBound] Failed to write default config.", e);
            }
        }
    }
}
