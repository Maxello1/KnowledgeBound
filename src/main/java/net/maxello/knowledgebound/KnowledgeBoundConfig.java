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
        /** Fail chance at tier 0,1,2,3,4 (0–1 range). */
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
    // Crafting chances (Tool / Weapon / Armour smithing)
    // --------------------------------------------------

    public List<String> _comment_crafting = List.of(
            "Crafting result chances for Toolsmithing, Weaponsmithing and Armouring.",
            "Each array entry represents a KNOWLEDGE TIER (0..4).",
            "Inside each entry:",
            "  failChance   = chance to get NO item",
            "  poorChance   = chance to get a POOR quality item (10% durability)",
            "  normalChance = chance to get a NORMAL item (full durability)",
            "The values are automatically normalized if they don't sum to 1.0."
    );

    /**
     * Per-tier crafting chances for toolsmithing.
     * Index 0..4 = knowledge tiers 0..4 (how good the smith is).
     */
    public CraftingTierChances[] toolsmithingChances = defaultToolsmithing();

    /**
     * Per-tier crafting chances for weaponsmithing.
     */
    public CraftingTierChances[] weaponsmithingChances = defaultWeaponsmithing();

    /**
     * Per-tier crafting chances for armouring.
     */
    public CraftingTierChances[] armouringChances = defaultArmouring();

    public static class CraftingTierChances {
        /** Chance the craft completely fails (no output item). */
        public double failChance;
        /** Chance the craft is “poor quality” (e.g. 10% durability). */
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

    private static CraftingTierChances[] defaultToolsmithing() {
        // Reasonable default curve; tweak in config if you want.
        return new CraftingTierChances[] {
                new CraftingTierChances(0.50, 0.50, 0.00), // knowledge tier 0
                new CraftingTierChances(0.30, 0.50, 0.20), // tier 1
                new CraftingTierChances(0.20, 0.50, 0.30), // tier 2
                new CraftingTierChances(0.10, 0.40, 0.50), // tier 3
                new CraftingTierChances(0.05, 0.25, 0.70)  // tier 4
        };
    }

    private static CraftingTierChances[] defaultWeaponsmithing() {
        // Copy of toolsmithing for now; can be adjusted separately later.
        return defaultToolsmithing();
    }

    private static CraftingTierChances[] defaultArmouring() {
        // Slightly harsher at low tiers.
        return new CraftingTierChances[] {
                new CraftingTierChances(0.60, 0.40, 0.00),
                new CraftingTierChances(0.40, 0.40, 0.20),
                new CraftingTierChances(0.30, 0.40, 0.30),
                new CraftingTierChances(0.15, 0.35, 0.50),
                new CraftingTierChances(0.05, 0.25, 0.70)
        };
    }

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
