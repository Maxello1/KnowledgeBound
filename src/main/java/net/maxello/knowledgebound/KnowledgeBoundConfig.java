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
import java.util.List;

public class KnowledgeBoundConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static KnowledgeBoundConfig INSTANCE = new KnowledgeBoundConfig();

    // --------------------------------------------------
    // Global XP tuning
    // --------------------------------------------------

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

    /**
     * Per-tier crafting chances for toolsmithing.
     * Index 0 = wooden/copper-equivalent tier, up to index 4.
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
         * Call this before using the chances if you want to be safe vs. config edits.
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
        return new CraftingTierChances[] {
                // tier 0
                new CraftingTierChances(0.50, 0.50, 0.00),
                // tier 1
                new CraftingTierChances(0.30, 0.50, 0.20),
                // tier 2
                new CraftingTierChances(0.20, 0.50, 0.30),
                // tier 3
                new CraftingTierChances(0.10, 0.40, 0.50),
                // tier 4
                new CraftingTierChances(0.05, 0.25, 0.70)
        };
    }

    private static CraftingTierChances[] defaultWeaponsmithing() {
        // You can differentiate later; for now copy toolsmithing defaults
        return defaultToolsmithing();
    }

    private static CraftingTierChances[] defaultArmouring() {
        return new CraftingTierChances[] {
                new CraftingTierChances(0.60, 0.40, 0.00),
                new CraftingTierChances(0.40, 0.40, 0.20),
                new CraftingTierChances(0.30, 0.40, 0.30),
                new CraftingTierChances(0.15, 0.35, 0.50),
                new CraftingTierChances(0.05, 0.25, 0.70)
        };
    }

    // --------------------------------------------------
    // Existing block / item extension lists
    // --------------------------------------------------

    /** Extra block IDs that should count for Forestry XP (e.g. "mytreesmod:ancient_log"). */
    public List<String> extraForestryBlocks = new ArrayList<>();
    public List<String> extraMiningBlocks   = new ArrayList<>();
    public List<String> extraDiggingBlocks  = new ArrayList<>();
    public List<String> extraFarmingBlocks  = new ArrayList<>();

    /** Extra item IDs that should use the wooden-tool rule. */
    public List<String> extraToolItems   = new ArrayList<>();
    /** Extra item IDs that should use the armor rule. */
    public List<String> extraArmorItems  = new ArrayList<>();
    /** Reserved for future weapon rules, but already in config. */
    public List<String> extraWeaponItems = new ArrayList<>();

    // --------------------------------------------------
    // Load / save (unchanged logic)
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
