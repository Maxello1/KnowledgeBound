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
            "=== KnowledgeBound Configuration ===",
            "This file controls all settings for the KnowledgeBound mod.",
            "You can change XP speed, fail chances, crafting quality, combat damage, and more.",
            "All values here are safe to edit. If you break something, just delete this file",
            "and restart the server — a fresh config with default values will be created.",
            "You can also reload changes in-game with the /kb reload command (requires OP)."
    );

    // --------------------------------------------------
    // Global XP tuning
    // --------------------------------------------------

    public List<String> _comment_xp = List.of(
            "=== XP Progression ===",
            "Controls how fast players level up their knowledge.",
            "Players earn 1 XP point per real-time minute of activity.",
            "",
            "baseMinutesPerTier: How many minutes of activity needed for each tier.",
            "  [0] = minutes to reach Tier 1, [1] = Tier 2, [2] = Tier 3, etc.",
            "  Default: [60, 120, 240, 480, 960] = 1hr, 2hr, 4hr, 8hr, 16hr",
            "",
            "minutesMultiplier: Scales ALL tier requirements.",
            "  1.0 = normal speed, 2.0 = takes twice as long, 0.5 = takes half as long."
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
    // Class job base minutes (3-tier jobs like Carpentry, Masonry)
    // --------------------------------------------------

    public List<String> _comment_classJobs = List.of(
            "=== Class Job Progression ===",
            "Class jobs (Carpentry, Masonry, Beekeeping) only have 3 tiers instead of 5.",
            "This setting controls how many minutes each tier requires.",
            "  [0] = minutes to Tier 1, [1] = Tier 2, [2] = Tier 3",
            "  Default: [60, 120, 240] = 1hr, 2hr, 4hr",
            "The minutesMultiplier also applies to these values."
    );

    /** Base minutes for class job tiers 1, 2, 3 (before multiplier). */
    public int[] classJobBaseMinutes = new int[] { 60, 120, 240 };

    // --------------------------------------------------
    // Proficiency limits
    // --------------------------------------------------

    public List<String> _comment_proficiency = List.of(
            "=== Proficiency Limits ===",
            "These limits force players to specialize — they can't master everything.",
            "",
            "maxMasterMaterial: How many 5-tier Material jobs (Toolsmithing, Weaponsmithing, Armouring)",
            "  a player can reach the highest tier in. Default: 1 (pick one specialty).",
            "",
            "maxTier4Material: How many Material jobs can reach Tier 4 or higher.",
            "  Default: 3 (all three can reach Tier 4, but only 1 can hit Tier 5).",
            "",
            "maxMasterClass: How many 3-tier Class jobs (Carpentry, Masonry, Beekeeping)",
            "  a player can fully master. Default: 1.",
            "",
            "Set any value to -1 to disable that limit (no restriction)."
    );

    public int maxMasterMaterial = 1;
    public int maxTier4Material = 3;
    public int maxMasterClass = 1;

    // --------------------------------------------------
    // Combat damage scaling
    // --------------------------------------------------

    public List<String> _comment_damageScale = List.of(
            "=== Combat Damage Scaling ===",
            "Controls how much damage players deal based on their Combat knowledge.",
            "Players use the HIGHER of their Melee Combat and Ranged Combat tiers.",
            "",
            "Each value is a multiplier: 1.0 = full damage, 0.5 = half damage, 0.0 = no damage.",
            "  [0] = Tier 0 (untrained), [1] = Tier 1, ... [5] = Tier 5 (mastered)",
            "  Default: [0.40, 0.55, 0.70, 0.85, 1.0, 1.0]",
            "  Meaning: untrained players deal only 40% damage!"
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
            "=== Gathering Fail Chances ===",
            "When a player breaks a block and 'fails', the block still breaks but drops NOTHING.",
            "Higher tiers = lower fail chance = more reliable gathering.",
            "",
            "Values are between 0.0 (0% = never fails) and 1.0 (100% = always fails).",
            "Each knowledge type has its own fail chances per tier:",
            "  tier0 = complete beginner, tier4 = expert",
            "",
            "Example: tier0=0.40 means a new player loses 40% of their gathered resources."
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
            "=== Crafting Quality System ===",
            "When a player crafts a tool, weapon, or armor, the result depends on their skill.",
            "The system compares the player's knowledge tier to the item's required tier.",
            "",
            "diff = playerTier - itemTier",
            "  Negative = item is above your skill level (risky!)",
            "  Zero     = item matches your skill level",
            "  Positive = item is below your skill level (easy)",
            "",
            "Array entries (6 levels of difficulty):",
            "  [0] = diff <= -3  (way out of your league — guaranteed fail)",
            "  [1] = diff  = -2  (very risky — 90% fail)",
            "  [2] = diff  = -1  (challenging — 75% fail)",
            "  [3] = diff  =  0  (at your level — 50% fail)",
            "  [4] = diff  = +1  (below your skill — 20% fail)",
            "  [5] = diff >= +2  (trivial — always perfect)",
            "",
            "Each entry has three chances that must add up to 1.0 (auto-normalized):",
            "  failChance   = item is destroyed, ingredients lost",
            "  poorChance   = item is created with very low durability",
            "  normalChance = item is created normally at full durability",
            "",
            "poorDurabilityFraction: what fraction of durability a 'poor' item gets.",
            "  0.10 = only 10% durability remaining (very fragile). Default: 0.10"
    );

    /**
     * Fraction of max durability for poor-quality crafts.
     * 0.10 = 10% durability (only 10% of the item's max durability remains).
     */
    public double poorDurabilityFraction = 0.10;

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
                new CraftingTierChances(0.90, 0.08, 0.02), // diff  = -2: very risky
                new CraftingTierChances(0.75, 0.20, 0.05), // diff  = -1: challenging
                new CraftingTierChances(0.50, 0.40, 0.10), // diff  =  0: at your level
                new CraftingTierChances(0.20, 0.30, 0.50), // diff  = +1: below your skill
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
            "=== Armor Restrictions ===",
            "Players can only EQUIP armor matching their Combat knowledge tier.",
            "Combat tier = the higher of Melee Combat and Ranged Combat.",
            "If a player tries to wear armor above their tier, it gets unequipped automatically.",
            "",
            "You can customize which combat tier is needed for each armor material,",
            "and add specific item overrides for modded armor."
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
    /** Extra item IDs that should use the carpentry rule. */
    public List<String> extraCarpentryItems = new ArrayList<>();
    /** Extra item IDs that should use the masonry rule. */
    public List<String> extraMasonryItems = new ArrayList<>();

    // --------------------------------------------------
    // Blocked crafting items
    // --------------------------------------------------

    public List<String> _comment_blocked = List.of(
            "=== Blocked Crafting Items ===",
            "Items listed here are completely BLOCKED from crafting.",
            "If a player tries to craft them, the ingredients are consumed but nothing is given.",
            "",
            "blockBoats: Set to true to block all vanilla boats and rafts from being crafted.",
            "  Default: true (boats are blocked)",
            "",
            "blockedCraftingItems: A list of additional item IDs to block.",
            "  Example: [\"minecraft:tnt\", \"mymod:overpowered_sword\"]"
    );

    /** Whether vanilla boats/rafts are blocked from crafting. */
    public boolean blockBoats = true;

    /** Additional item IDs that cannot be crafted at all. */
    public List<String> blockedCraftingItems = new ArrayList<>();

    // --------------------------------------------------
    // Stonecutter settings
    // --------------------------------------------------

    public List<String> _comment_stonecutter = List.of(
            "=== Stonecutter Settings ===",
            "The stonecutter requires Masonry knowledge to use.",
            "",
            "stonecutterMinTier: Minimum Masonry tier needed to use the stonecutter at all.",
            "  Default: 1 (players need at least Masonry Tier 1)",
            "",
            "stonecutterCutChanceTier1: Chance of cutting yourself when using the stonecutter.",
            "  Only applies at Masonry Tier 1. Value between 0.0 and 1.0. Default: 0.10 (10%)",
            "",
            "stonecutterCutReductionPerTier: How much the cut chance decreases per tier above 1.",
            "  Example with defaults: Tier 1 = 10%, Tier 2 = 5%, Tier 3 = 0%",
            "",
            "stonecutterCutDamage: Hearts of damage when you cut yourself.",
            "  Default: 2.0 (1 full heart)"
    );

    public int stonecutterMinTier = 1;
    public double stonecutterCutChanceTier1 = 0.10;
    public double stonecutterCutReductionPerTier = 0.05;
    public float stonecutterCutDamage = 2.0f;

    // --------------------------------------------------
    // Beekeeping
    // --------------------------------------------------

    public List<String> _comment_beekeeping = List.of(
            "=== Beekeeping Settings ===",
            "Controls what happens when players interact with beehives.",
            "",
            "beekeepingHarvestFail: Chance per tier that harvesting honey fails and angers the bees.",
            "  Works the same as gathering fail chances (0.0 to 1.0).",
            "",
            "betterHoneyChance: Chance per beekeeping tier to get 'Royal Honey' on a successful harvest.",
            "  Royal Honey is a special honey bottle with an enchant glint and potion effects.",
            "  Array of 3 values (one per tier): Default: [0.0, 0.10, 0.25]",
            "  Meaning: Tier 1 = 0% chance, Tier 2 = 10%, Tier 3 = 25%",
            "",
            "silkTouchBeehiveMinTier: Minimum beekeeping tier to move beehives with silk touch.",
            "  Default: 3 (only master beekeepers can move hives)",
            "",
            "betterHoney: Customize what the Royal Honey item looks like and what effects it gives.",
            "  itemId = base item (default: minecraft:honey_bottle)",
            "  customName = display name shown to players",
            "  nameColor = text color (e.g. gold, yellow, aqua)",
            "  effects = list of potion effects applied when consumed",
            "    Each effect has: effectId, durationTicks (20 ticks = 1 second), amplifier (0 = level I)"
    );

    public GatherFailConfig beekeepingHarvestFail = new GatherFailConfig(0.50, 0.30, 0.10, 0.0, 0.0);
    public double[] betterHoneyChance = new double[] { 0.0, 0.10, 0.25 };
    public int silkTouchBeehiveMinTier = 3;
    /** CustomModelData value applied to Royal Honey for resource pack textures. */
    public int royalHoneyCustomModelData = 1;

    public BetterHoneyConfig betterHoney = new BetterHoneyConfig();

    public static class BetterHoneyConfig {
        public String itemId = "minecraft:honey_bottle";
        public String customName = "Royal Honey";
        public String nameColor = "gold";
        public List<PotionEffectEntry> effects = List.of(
                new PotionEffectEntry("minecraft:regeneration", 200, 1),
                new PotionEffectEntry("minecraft:saturation", 100, 0)
        );
    }

    public static class PotionEffectEntry {
        public String effectId;
        public int durationTicks;
        public int amplifier;

        public PotionEffectEntry() {}

        public PotionEffectEntry(String effectId, int durationTicks, int amplifier) {
            this.effectId = effectId;
            this.durationTicks = durationTicks;
            this.amplifier = amplifier;
        }
    }

    // --------------------------------------------------
    // Configurable gameplay messages
    // --------------------------------------------------

    public List<String> _comment_messages = List.of(
            "=== Gameplay Messages ===",
            "Customize the text messages sent to players during gameplay.",
            "You can use Minecraft formatting codes (e.g. §a for green, §c for red, §6 for gold, etc.).",
            "Use standard placeholders: {knowledge}, {tier}, {minTier}, {honeyName}, {tierName}."
    );

    public MessagesConfig messages = new MessagesConfig();

    public static class MessagesConfig {
        public String learning = "§aYou're learning {knowledge}!";
        public String levelUp = "§6Your {knowledge} knowledge increased to Tier {tier}!";
        public String craftingFail = "§cYour {knowledge} attempt failed to yield any items.";
        public String craftingLevelTooLow = "§cYou don't have enough {knowledge} knowledge to work with these materials.";
        public String craftingQualityPoor = "§bYou crafted a §d§lpoor§r§b quality item. Improve your {knowledge} knowledge for better quality.";
        public String craftingQualityNormal = "§bYou crafted a §a§lnormal§r§b quality item.";
        public String gatheringFail = "§cYour {knowledge} attempt failed to yield any resources.";
        public String proficiencyLimitReached = "§4You've reached your proficiency limit for {knowledge}. You cannot advance further.";
        public String blockedCraftingItem = "§cThis item cannot be crafted.";
        public String stonecutterMinTierLimit = "§cYou need Masonry Tier {minTier} to use the stonecutter.";
        public String stonecutterCutSelf = "§cYou cut yourself on the stonecutter!";
        public String silkTouchBeehiveLimit = "§cYou need Beekeeping Tier {minTier} to move beehives with bees.";
        public String beehiveAngeredBees = "§cYour clumsy handling angered the bees!";
        public String royalHoneyHarvested = "§6You harvested some {honeyName}!";
        public String armorRestricted = "§cYou need {tierName} Combat Knowledge to wear this armor!";

        // Combat messages
        public String weaponDropped = "§cYou fumbled your weapon!";
        public String rangedPoorAccuracy = "§eYour aim was shaky...";

        // Smelting messages
        public String smeltingFail = "§cYour smelting attempt failed!";
        public String smeltingSuccess = "§aYou successfully smelted the ore!";
        public String smeltingTierLocked = "§cYou need Smelting Tier {minTier} to smelt this.";
        public String smeltingLeftUnattended = "§cYou left the furnace unattended and the job failed!";
        public String smeltingCollectionExpired = "§cYou didn't collect the result in time!";

        // Cooking messages
        public String cookingFail = "§cYour cooking attempt burned the food!";
        public String cookingSuccess = "§aYou successfully cooked the food!";
        public String cookingSpecialResult = "§6You cooked something special!";
        public String cookingLeftUnattended = "§cYou left the furnace unattended and the food burned!";
        public String cookingCollectionExpired = "§cYou didn't collect the food in time!";

        // Shared workstation messages
        public String furnaceBusy = "§cSomeone else is using this furnace.";
    }

    // --------------------------------------------------
    // Mob spawn blocking
    // --------------------------------------------------

    public List<String> _comment_mobBlocking = List.of(
            "=== Mob Spawn Blocking ===",
            "Toggle specific mob spawns on or off, or add custom entity IDs to block.",
            "Set any of the boolean fields to true to prevent that mob from spawning.",
            "",
            "blockedMobSpawns: A list of additional entity IDs to block from spawning.",
            "  Example: [\"minecraft:phantom\", \"mymod:custom_zombie\"]",
            "",
            "Admins can toggle individual mob spawns (e.g. blockVillagerSpawns = true)",
            "or add any entity ID to blockedMobSpawns for full control over the world."
    );

    /** Whether villager spawns are blocked. */
    public boolean blockVillagerSpawns = false;
    /** Whether iron golem spawns are blocked. */
    public boolean blockIronGolemSpawns = false;
    /** Whether snow golem spawns are blocked. */
    public boolean blockSnowGolemSpawns = false;
    /** Whether copper golem spawns are blocked. */
    public boolean blockCopperGolemSpawns = false;
    /** Whether zombie villager spawns are blocked. */
    public boolean blockZombieVillagerSpawns = false;
    /** Whether wandering trader spawns are blocked. */
    public boolean blockWanderingTraderSpawns = false;
    /** Whether pillager spawns are blocked. */
    public boolean blockPillagerSpawns = false;

    /** Additional entity IDs that are blocked from spawning. */
    public List<String> blockedMobSpawns = new ArrayList<>();

    // --------------------------------------------------
    // Combat weapon dropping & accuracy
    // --------------------------------------------------

    public List<String> _comment_combat = List.of(
            "=== Combat Weapon Dropping & Accuracy ===",
            "weaponDropOnFail: If true, a failed combat roll drops the player's weapon.",
            "combatWeaponTierOverrides: Per-weapon required tier overrides for combat.",
            "  Defaults fall back to crafting tier registry (e.g. wooden_sword=0, bow=1, crossbow=2).",
            "",
            "bowSpreadPerDiff / crossbowSpreadPerDiff: Extra projectile divergence per tier diff.",
            "  Index 0 = diff <= -3, 1 = -2, 2 = -1, 3 = 0, 4 = +1, 5 = +2 or higher.",
            "  Higher values = more inaccuracy. 0 = vanilla accuracy.",
            "poorSpreadModifier: Additional spread added on a 'poor' combat roll."
    );

    /** Whether weapon dropping on combat fail is enabled. */
    public boolean weaponDropOnFail = true;
    /** Per-weapon required combat tier overrides (item ID -> tier). */
    public Map<String, Integer> combatWeaponTierOverrides = new HashMap<>();
    /** Extra bow divergence per tier-difference index. */
    public double[] bowSpreadPerDiff = new double[] { 6.0, 4.0, 2.0, 0.5, 0.0, 0.0 };
    /** Extra crossbow divergence per tier-difference index. */
    public double[] crossbowSpreadPerDiff = new double[] { 4.0, 2.5, 1.5, 0.3, 0.0, 0.0 };
    /** Additional spread modifier applied on a 'poor' combat roll. */
    public double poorSpreadModifier = 2.0;

    // --------------------------------------------------
    // Smelting knowledge (supervised furnace jobs)
    // --------------------------------------------------

    public List<String> _comment_smelting = List.of(
            "=== Smelting Knowledge ===",
            "Smelting is a 3-tier supervised job for metallurgy furnace/blast furnace recipes.",
            "Players must keep the furnace UI open while smelting. Only one tracked item at a time.",
            "",
            "smeltingGraceTimeTicks: Ticks before an unattended job fails (100 = 5 seconds).",
            "smeltingCollectionWindowTicks: Ticks to collect the result after smelting finishes.",
            "smeltingLeaveBehaviour: What happens when the grace expires: FAIL or RESET_PROGRESS.",
            "",
            "metallurgyItems: Items that require smelting supervision.",
            "smeltingRecipeTiers: Per-item minimum smelting tier (item ID -> tier). Default: all tier 0.",
            "smeltingTierYields: Per-item output quantity by tier (item ID -> [tier0, tier1, tier2])."
    );

    /** Whether smelting supervision is enabled. */
    public boolean smeltingEnabled = true;
    /** Base minutes for smelting tiers 1, 2, 3 (before multiplier). */
    public int[] smeltingBaseMinutes = new int[] { 60, 120, 240 };
    /** Grace period ticks when the player closes the furnace UI. */
    public int smeltingGraceTimeTicks = 100;
    /** Collection window ticks after smelting completes. */
    public int smeltingCollectionWindowTicks = 100;
    /** Behavior on grace expiry: "FAIL" or "RESET_PROGRESS". */
    public String smeltingLeaveBehaviour = "FAIL";
    /** Fail chances per smelting tier (0-2). */
    public GatherFailConfig smeltingFailChances = new GatherFailConfig(0.50, 0.30, 0.10, 0.0, 0.0);
    /** Item IDs that require smelting supervision (metallurgy inputs). */
    public List<String> metallurgyItems = List.of(
            "minecraft:raw_iron", "minecraft:raw_gold", "minecraft:raw_copper",
            "minecraft:iron_ore", "minecraft:gold_ore", "minecraft:copper_ore",
            "minecraft:deepslate_iron_ore", "minecraft:deepslate_gold_ore", "minecraft:deepslate_copper_ore",
            "minecraft:ancient_debris", "minecraft:clay_ball", "minecraft:clay"
    );
    /** Per-item minimum smelting tier overrides (item ID -> required tier). */
    public Map<String, Integer> smeltingRecipeTiers = new HashMap<>();
    /** Per-item output yields by tier (item ID -> [yield at tier 0, 1, 2]). */
    public Map<String, List<Integer>> smeltingTierYields = new HashMap<>();

    // --------------------------------------------------
    // Cooking knowledge (supervised furnace/smoker jobs)
    // --------------------------------------------------

    public List<String> _comment_cooking = List.of(
            "=== Cooking Knowledge ===",
            "Cooking is a 3-tier supervised job for food furnace/smoker recipes.",
            "Uses the same supervision system as smelting (grace timer, collection window, etc.).",
            "",
            "cookingAppliesToCampfire: Whether campfire cooking requires supervision.",
            "  Default: false (campfires have no GUI to supervise).",
            "",
            "cookingSpecialOutputs: Data-driven special food results at higher tiers.",
            "  Each entry has: inputItemId, requiredTier, outputItemId, chancePerTier."
    );

    /** Whether cooking supervision is enabled. */
    public boolean cookingEnabled = true;
    /** Grace period ticks when the player closes the furnace/smoker UI. */
    public int cookingGraceTimeTicks = 100;
    /** Collection window ticks after cooking completes. */
    public int cookingCollectionWindowTicks = 100;
    /** Behavior on grace expiry: "FAIL" or "RESET_PROGRESS". */
    public String cookingLeaveBehaviour = "FAIL";
    /** Whether campfire cooking requires supervision. */
    public boolean cookingAppliesToCampfire = false;
    /** Fail chances per cooking tier (0-2). */
    public GatherFailConfig cookingFailChances = new GatherFailConfig(0.40, 0.25, 0.10, 0.0, 0.0);
    /** Item IDs that require cooking supervision (food inputs). */
    public List<String> cookingItems = List.of(
            "minecraft:beef", "minecraft:porkchop", "minecraft:chicken",
            "minecraft:mutton", "minecraft:rabbit", "minecraft:cod",
            "minecraft:salmon", "minecraft:potato", "minecraft:kelp"
    );
    /** Special cooking outputs (data-driven, like Royal Honey for cooking). */
    public List<CookingSpecialOutput> cookingSpecialOutputs = new ArrayList<>();

    public static class CookingSpecialOutput {
        /** The input item that can produce a special result. */
        public String inputItemId = "";
        /** Minimum cooking tier required for the special result. */
        public int requiredTier = 1;
        /** The special output item ID. */
        public String outputItemId = "";
        /** Chance of special output per cooking tier [tier0, tier1, tier2]. */
        public double[] chancePerTier = new double[] { 0.0, 0.10, 0.25 };

        public CookingSpecialOutput() {}

        public CookingSpecialOutput(String inputItemId, int requiredTier, String outputItemId,
                                     double[] chancePerTier) {
            this.inputItemId = inputItemId;
            this.requiredTier = requiredTier;
            this.outputItemId = outputItemId;
            this.chancePerTier = chancePerTier;
        }
    }

    // --------------------------------------------------
    // Ore respawning
    // --------------------------------------------------

    public List<String> _comment_oreRespawn = List.of(
            "=== Ore Respawning ===",
            "When a natural ore is mined, it is replaced by a placeholder block.",
            "After a configurable delay, the ore returns (if the placeholder is intact).",
            "",
            "oreRespawnEnabled: Master toggle. Default: false (disabled).",
            "oreRespawnDelayTicks: Ticks before the ore respawns (72000 = 1 hour).",
            "oreRespawnMaxCount: Max times an ore position can respawn. -1 = unlimited.",
            "orePlaceholderMap: Maps ore block IDs to placeholder block IDs.",
            "respawnableOres: List of ore block IDs that can respawn.",
            "",
            "Player-placed ores never respawn. Breaking the placeholder cancels respawning."
    );

    /** Whether ore respawning is enabled. */
    public boolean oreRespawnEnabled = false;
    /** Delay in ticks before an ore respawns. */
    public int oreRespawnDelayTicks = 72000;
    /** Max number of respawns per ore position (-1 = unlimited). */
    public int oreRespawnMaxCount = -1;
    /** Maps ore block IDs to their placeholder block IDs. */
    public Map<String, String> orePlaceholderMap = defaultOrePlaceholderMap();
    /** List of ore block IDs that can respawn. */
    public List<String> respawnableOres = List.of(
            "minecraft:coal_ore", "minecraft:iron_ore", "minecraft:gold_ore",
            "minecraft:diamond_ore", "minecraft:lapis_ore", "minecraft:redstone_ore",
            "minecraft:emerald_ore", "minecraft:copper_ore",
            "minecraft:deepslate_coal_ore", "minecraft:deepslate_iron_ore",
            "minecraft:deepslate_gold_ore", "minecraft:deepslate_diamond_ore",
            "minecraft:deepslate_lapis_ore", "minecraft:deepslate_redstone_ore",
            "minecraft:deepslate_emerald_ore", "minecraft:deepslate_copper_ore",
            "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore",
            "minecraft:ancient_debris"
    );

    private static Map<String, String> defaultOrePlaceholderMap() {
        Map<String, String> map = new HashMap<>();
        map.put("minecraft:coal_ore", "minecraft:cobblestone");
        map.put("minecraft:iron_ore", "minecraft:cobblestone");
        map.put("minecraft:gold_ore", "minecraft:cobblestone");
        map.put("minecraft:diamond_ore", "minecraft:cobblestone");
        map.put("minecraft:lapis_ore", "minecraft:cobblestone");
        map.put("minecraft:redstone_ore", "minecraft:cobblestone");
        map.put("minecraft:emerald_ore", "minecraft:cobblestone");
        map.put("minecraft:copper_ore", "minecraft:cobblestone");
        map.put("minecraft:deepslate_coal_ore", "minecraft:cobbled_deepslate");
        map.put("minecraft:deepslate_iron_ore", "minecraft:cobbled_deepslate");
        map.put("minecraft:deepslate_gold_ore", "minecraft:cobbled_deepslate");
        map.put("minecraft:deepslate_diamond_ore", "minecraft:cobbled_deepslate");
        map.put("minecraft:deepslate_lapis_ore", "minecraft:cobbled_deepslate");
        map.put("minecraft:deepslate_redstone_ore", "minecraft:cobbled_deepslate");
        map.put("minecraft:deepslate_emerald_ore", "minecraft:cobbled_deepslate");
        map.put("minecraft:deepslate_copper_ore", "minecraft:cobbled_deepslate");
        map.put("minecraft:nether_gold_ore", "minecraft:netherrack");
        map.put("minecraft:nether_quartz_ore", "minecraft:netherrack");
        map.put("minecraft:ancient_debris", "minecraft:netherrack");
        return map;
    }

    // --------------------------------------------------
    // Crop indirect destruction
    // --------------------------------------------------

    public List<String> _comment_cropDestruction = List.of(
            "=== Crop Indirect Destruction ===",
            "When true, breaking the block under a crop (or trampling farmland)",
            "always suppresses all crop/seed drops and grants no Farming XP.",
            "Applies to all growth stages, not just mature crops."
    );

    /** Whether indirect crop destruction suppresses all drops. */
    public boolean suppressIndirectCropDrops = true;

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
                // Fill in any missing fields from new versions and re-save
                INSTANCE.fillDefaults();
                INSTANCE.save();
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

    /**
     * Fill null or empty fields with defaults so old config files get new fields properly.
     * Gson may set fields to null if not present, or deserialize an empty array/map
     * if the old config had "[]" or "{}".
     */
    private void fillDefaults() {
        KnowledgeBoundConfig defaults = new KnowledgeBoundConfig();

        // Gather fail configs
        if (forestryGatherFail == null) forestryGatherFail = defaults.forestryGatherFail;
        if (miningGatherFail == null) miningGatherFail = defaults.miningGatherFail;
        if (diggingGatherFail == null) diggingGatherFail = defaults.diggingGatherFail;
        if (farmingGatherFail == null) farmingGatherFail = defaults.farmingGatherFail;

        // Crafting chances
        if (craftingDiffChances == null || craftingDiffChances.length == 0)
            craftingDiffChances = defaults.craftingDiffChances;

        // Combat
        if (combatWeaponTierOverrides == null) combatWeaponTierOverrides = defaults.combatWeaponTierOverrides;
        if (bowSpreadPerDiff == null || bowSpreadPerDiff.length == 0)
            bowSpreadPerDiff = defaults.bowSpreadPerDiff;
        if (crossbowSpreadPerDiff == null || crossbowSpreadPerDiff.length == 0)
            crossbowSpreadPerDiff = defaults.crossbowSpreadPerDiff;

        // Smelting
        if (smeltingBaseMinutes == null || smeltingBaseMinutes.length == 0)
            smeltingBaseMinutes = defaults.smeltingBaseMinutes;
        if (smeltingFailChances == null) smeltingFailChances = defaults.smeltingFailChances;
        if (metallurgyItems == null || metallurgyItems.isEmpty())
            metallurgyItems = defaults.metallurgyItems;
        if (smeltingRecipeTiers == null) smeltingRecipeTiers = defaults.smeltingRecipeTiers;
        if (smeltingTierYields == null) smeltingTierYields = defaults.smeltingTierYields;
        if (smeltingLeaveBehaviour == null) smeltingLeaveBehaviour = defaults.smeltingLeaveBehaviour;

        // Cooking
        if (cookingFailChances == null) cookingFailChances = defaults.cookingFailChances;
        if (cookingItems == null || cookingItems.isEmpty())
            cookingItems = defaults.cookingItems;
        if (cookingSpecialOutputs == null) cookingSpecialOutputs = defaults.cookingSpecialOutputs;
        if (cookingLeaveBehaviour == null) cookingLeaveBehaviour = defaults.cookingLeaveBehaviour;

        // Ore respawn
        if (respawnableOres == null || respawnableOres.isEmpty())
            respawnableOres = defaults.respawnableOres;
        if (orePlaceholderMap == null || orePlaceholderMap.isEmpty())
            orePlaceholderMap = defaults.orePlaceholderMap;

        // Messages
        if (messages == null) messages = defaults.messages;

        // Extra block lists (these CAN be empty on purpose, so only fill if null)
        if (extraForestryBlocks == null) extraForestryBlocks = defaults.extraForestryBlocks;
        if (extraMiningBlocks == null) extraMiningBlocks = defaults.extraMiningBlocks;
        if (extraDiggingBlocks == null) extraDiggingBlocks = defaults.extraDiggingBlocks;
        if (extraFarmingBlocks == null) extraFarmingBlocks = defaults.extraFarmingBlocks;
    }

    public void save() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path path = configDir.resolve("knowledgebound.json");
        try {
            Files.createDirectories(configDir);
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
            KnowledgeBound.LOGGER.info("[KnowledgeBound] Saved config to {}", path);
        } catch (IOException e) {
            KnowledgeBound.LOGGER.error("[KnowledgeBound] Failed to save config.", e);
        }
    }

    // --------------------------------------------------
    // Reflection-based config access
    // --------------------------------------------------

    /**
     * Get a config value by dot-separated path (e.g. "messages.learning", "blockVillagerSpawns").
     * Uses reflection to traverse nested objects. Returns the JSON representation.
     */
    public String getFieldValue(String path) {
        try {
            String[] parts = path.split("\\.");
            Object current = this;
            for (String part : parts) {
                if (part.startsWith("_comment")) return null;
                java.lang.reflect.Field field = current.getClass().getField(part);
                current = field.get(current);
            }
            return GSON.toJson(current);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Set a config value by dot-separated path. The value string is parsed as JSON for
     * complex types (arrays, objects, numbers, booleans), or as a raw string for String fields.
     */
    public boolean setFieldValue(String path, String value) {
        try {
            String[] parts = path.split("\\.");
            Object current = this;
            for (int i = 0; i < parts.length - 1; i++) {
                java.lang.reflect.Field field = current.getClass().getField(parts[i]);
                current = field.get(current);
            }
            String lastPart = parts[parts.length - 1];
            if (lastPart.startsWith("_comment")) return false;
            java.lang.reflect.Field field = current.getClass().getField(lastPart);
            Class<?> type = field.getType();

            if (type == String.class) {
                field.set(current, value);
            } else {
                Object parsed = GSON.fromJson(value, field.getGenericType());
                field.set(current, parsed);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * List all settable config field paths (excluding comment fields).
     */
    public static java.util.List<String> allConfigKeys() {
        java.util.List<String> keys = new java.util.ArrayList<>();
        collectKeys("", KnowledgeBoundConfig.class, keys, 0);
        return keys;
    }

    private static void collectKeys(String prefix, Class<?> clazz, java.util.List<String> keys, int depth) {
        if (depth > 2) return; // prevent infinite recursion
        for (java.lang.reflect.Field f : clazz.getFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            if (f.getName().startsWith("_comment")) continue;
            String fullPath = prefix.isEmpty() ? f.getName() : prefix + "." + f.getName();
            Class<?> type = f.getType();
            // Recurse into our own nested config classes
            if (type == MessagesConfig.class || type == ArmorTierConfig.class
                    || type == BetterHoneyConfig.class || type == CookingSpecialOutput.class) {
                collectKeys(fullPath, type, keys, depth + 1);
            } else {
                keys.add(fullPath);
            }
        }
    }
}
