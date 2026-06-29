package net.maxello.knowledgebound;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of config categories for the admin GUI.
 * Each category has a name, icon, and list of editable entries.
 */
public final class ConfigGuiCategory {

    private static final Map<String, ConfigGuiCategory> CATEGORIES = new LinkedHashMap<>();

    private final String id;
    private final String displayName;
    private final Item icon;
    private final List<ConfigGuiEntry> entries;

    public ConfigGuiCategory(String id, String displayName, Item icon, List<ConfigGuiEntry> entries) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.entries = entries;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Item getIcon() { return icon; }
    public List<ConfigGuiEntry> getEntries() { return entries; }

    public static Map<String, ConfigGuiCategory> all() { return CATEGORIES; }

    public static ConfigGuiCategory get(String id) { return CATEGORIES.get(id); }

    public static void init() {
        CATEGORIES.clear();

        // --- XP Progression ---
        register(new ConfigGuiCategory("xp", "XP Progression", Items.EXPERIENCE_BOTTLE, List.of(
                ConfigGuiEntry.decimal("minutesMultiplier", "Minutes Multiplier",
                        "Scales all tier requirements. 1.0 = normal, 2.0 = twice as slow", 0.1, 10.0, 0.1, 1.0),
                ConfigGuiEntry.integer("maxMasterMaterial", "Max Master Material",
                        "How many 5-tier material jobs can be mastered. -1 = no limit", -1, 5),
                ConfigGuiEntry.integer("maxTier4Material", "Max Tier 4 Material",
                        "How many material jobs can reach tier 4+. -1 = no limit", -1, 5),
                ConfigGuiEntry.integer("maxMasterClass", "Max Master Class",
                        "How many 3-tier class jobs can be mastered. -1 = no limit", -1, 5)
        )));

        // --- Combat Settings ---
        register(new ConfigGuiCategory("combat", "Combat Settings", Items.DIAMOND_SWORD, List.of(
                ConfigGuiEntry.bool("weaponDropOnFail", "Weapon Drop on Fail",
                        "Drop weapon on a failed combat roll"),
                ConfigGuiEntry.decimal("poorSpreadModifier", "Poor Shot Spread",
                        "Extra inaccuracy on a 'poor' ranged roll", 0.0, 20.0, 0.5, 2.0),
                ConfigGuiEntry.decimal("poorDurabilityFraction", "Poor Craft Durability",
                        "Durability fraction for 'poor' quality crafts", 0.01, 1.0, 0.01, 0.1)
        )));

        // --- Gathering Fail Chances ---
        List<ConfigGuiEntry> gatherEntries = new ArrayList<>();
        for (String[] g : new String[][]{
                {"forestryGatherFail", "Forestry"},
                {"miningGatherFail", "Mining"},
                {"diggingGatherFail", "Digging"},
                {"farmingGatherFail", "Farming"}}) {
            for (int t = 0; t <= 4; t++) {
                gatherEntries.add(ConfigGuiEntry.decimal(
                        g[0] + ".tier" + t, g[1] + " Tier " + t + " Fail",
                        "Fail chance at tier " + t + " (0.0 - 1.0)", 0.0, 1.0, 0.01, 0.05));
            }
        }
        register(new ConfigGuiCategory("gathering", "Gathering Settings", Items.IRON_PICKAXE, gatherEntries));

        // --- Stonecutter ---
        register(new ConfigGuiCategory("stonecutter", "Stonecutter", Items.STONECUTTER, List.of(
                ConfigGuiEntry.integer("stonecutterMinTier", "Min Masonry Tier",
                        "Minimum tier to use the stonecutter", 0, 5),
                ConfigGuiEntry.decimal("stonecutterCutChanceTier1", "Cut Chance (Tier 1)",
                        "Chance of cutting yourself at tier 1", 0.0, 1.0, 0.01, 0.05),
                ConfigGuiEntry.decimal("stonecutterCutReductionPerTier", "Cut Reduction/Tier",
                        "Cut chance reduction per tier above 1", 0.0, 1.0, 0.01, 0.05),
                ConfigGuiEntry.decimal("stonecutterCutDamage", "Cut Damage",
                        "Hearts of damage when cut (2.0 = 1 heart)", 0.0, 20.0, 0.5, 2.0)
        )));

        // --- Beekeeping ---
        List<ConfigGuiEntry> beekeepingEntries = new ArrayList<>();
        beekeepingEntries.add(ConfigGuiEntry.integer("silkTouchBeehiveMinTier", "Silk Touch Min Tier",
                "Minimum tier to move beehives with bees", 0, 5));
        beekeepingEntries.add(ConfigGuiEntry.integer("royalHoneyCustomModelData", "Royal Honey Model Data",
                "CustomModelData for resource pack textures", 0, 100));
        for (int t = 0; t <= 4; t++) {
            beekeepingEntries.add(ConfigGuiEntry.decimal(
                    "beekeepingHarvestFail.tier" + t, "Harvest Fail Tier " + t,
                    "Fail chance at beekeeping tier " + t, 0.0, 1.0, 0.01, 0.05));
        }
        register(new ConfigGuiCategory("beekeeping", "Beekeeping", Items.HONEYCOMB, beekeepingEntries));

        // --- Smelting & Cooking ---
        register(new ConfigGuiCategory("smelting_cooking", "Smelting & Cooking", Items.FURNACE, List.of(
                ConfigGuiEntry.bool("smeltingEnabled", "Smelting Enabled",
                        "Enable smelting supervision system"),
                ConfigGuiEntry.integer("smeltingGraceTimeTicks", "Smelting Grace (ticks)",
                        "Grace period before unattended job fails", 0, 6000),
                ConfigGuiEntry.integer("smeltingCollectionWindowTicks", "Smelting Collection (ticks)",
                        "Time to collect result after smelting", 0, 6000),
                ConfigGuiEntry.bool("cookingEnabled", "Cooking Enabled",
                        "Enable cooking supervision system"),
                ConfigGuiEntry.integer("cookingGraceTimeTicks", "Cooking Grace (ticks)",
                        "Grace period before unattended cooking fails", 0, 6000),
                ConfigGuiEntry.integer("cookingCollectionWindowTicks", "Cooking Collection (ticks)",
                        "Time to collect food after cooking", 0, 6000),
                ConfigGuiEntry.bool("cookingAppliesToCampfire", "Campfire Requires Supervision",
                        "Whether campfire cooking needs supervision")
        )));

        // --- Armor Restrictions ---
        register(new ConfigGuiCategory("armor", "Armor Restrictions", Items.IRON_CHESTPLATE, List.of(
                ConfigGuiEntry.integer("armorTiers.leatherTier", "Leather Tier", "Required combat tier for leather armor", 0, 10),
                ConfigGuiEntry.integer("armorTiers.chainTier", "Chainmail Tier", "Required combat tier for chainmail armor", 0, 10),
                ConfigGuiEntry.integer("armorTiers.ironTier", "Iron Tier", "Required combat tier for iron armor", 0, 10),
                ConfigGuiEntry.integer("armorTiers.goldTier", "Gold Tier", "Required combat tier for gold armor", 0, 10),
                ConfigGuiEntry.integer("armorTiers.diamondTier", "Diamond Tier", "Required combat tier for diamond armor", 0, 10),
                ConfigGuiEntry.integer("armorTiers.netheriteTier", "Netherite Tier", "Required combat tier for netherite armor", 0, 10)
        )));

        // --- Mob Spawning ---
        register(new ConfigGuiCategory("mobs", "Mob Spawning", Items.ZOMBIE_HEAD, List.of(
                ConfigGuiEntry.bool("blockVillagerSpawns", "Block Villagers", "Prevent villager spawns"),
                ConfigGuiEntry.bool("blockIronGolemSpawns", "Block Iron Golems", "Prevent iron golem spawns"),
                ConfigGuiEntry.bool("blockSnowGolemSpawns", "Block Snow Golems", "Prevent snow golem spawns"),
                ConfigGuiEntry.bool("blockCopperGolemSpawns", "Block Copper Golems", "Prevent copper golem spawns"),
                ConfigGuiEntry.bool("blockZombieVillagerSpawns", "Block Zombie Villagers", "Prevent zombie villager spawns"),
                ConfigGuiEntry.bool("blockWanderingTraderSpawns", "Block Wandering Traders", "Prevent wandering trader spawns"),
                ConfigGuiEntry.bool("blockPillagerSpawns", "Block Pillagers", "Prevent pillager spawns")
        )));

        // --- Ore Respawning ---
        register(new ConfigGuiCategory("ore_respawn", "Ore Respawning", Items.DIAMOND_ORE, List.of(
                ConfigGuiEntry.bool("oreRespawnEnabled", "Ore Respawn Enabled", "Master toggle for ore respawning"),
                ConfigGuiEntry.integer("oreRespawnDelayTicks", "Respawn Delay (ticks)",
                        "Ticks before ore respawns (72000 = 1 hour)", 0, 1000000),
                ConfigGuiEntry.integer("oreRespawnMaxCount", "Max Respawn Count",
                        "Max times an ore position can respawn (-1 = unlimited)", -1, 1000)
        )));

        // --- Gameplay Toggles ---
        register(new ConfigGuiCategory("gameplay", "Gameplay Toggles", Items.COMMAND_BLOCK, List.of(
                ConfigGuiEntry.bool("blockBoats", "Block Boat Crafting", "Prevent crafting of all boats and rafts"),
                ConfigGuiEntry.bool("suppressIndirectCropDrops", "Suppress Indirect Crop Drops",
                        "Destroy crops with no drops when farmland is broken")
        )));

        // --- Husbandry ---
        register(new ConfigGuiCategory("husbandry", "Husbandry", Items.WHEAT, List.of(
                ConfigGuiEntry.bool("husbandryEnabled", "Husbandry Enabled", "Master toggle for husbandry system"),
                ConfigGuiEntry.bool("husbandryBreedingEnabled", "Breeding Enabled", "Enable breeding tier checks"),
                ConfigGuiEntry.bool("husbandryBreedingConsumeItemOnFail", "Consume Item On Fail", "Consume breeding item when attempt fails"),
                ConfigGuiEntry.bool("husbandryBreedingCooldownEnabled", "Breeding Cooldowns", "Enable custom breeding cooldowns"),
                ConfigGuiEntry.bool("husbandryTamingEnabled", "Taming Enabled", "Enable taming tier checks"),
                ConfigGuiEntry.bool("husbandryTamingConsumeItemOnFail", "Consume Taming Item On Fail", "Consume taming item when attempt fails"),
                ConfigGuiEntry.bool("husbandryMilkingEnabled", "Milking Enabled", "Enable milking tier checks"),
                ConfigGuiEntry.bool("husbandryMilkingConsumeBucketOnFail", "Consume Bucket On Fail", "Consume bucket when milking fails"),
                ConfigGuiEntry.bool("husbandryShearingEnabled", "Shearing Enabled", "Enable shearing tier checks"),
                ConfigGuiEntry.bool("husbandryShearingDamageShearsOnFail", "Damage Shears On Fail", "Damage shears when shearing fails"),
                ConfigGuiEntry.bool("husbandryRidingEnabled", "Riding Enabled", "Enable riding tier checks"),
                ConfigGuiEntry.bool("husbandryRidingUnreliableBelowTier", "Unreliable Riding", "Low-tier riders get kicked off randomly"),
                ConfigGuiEntry.decimal("husbandryRidingKickOffChance", "Kick-Off Chance", "Chance to be kicked off per check interval", 0.0, 1.0),
                ConfigGuiEntry.integer("husbandryRidingCheckIntervalTicks", "Riding Check Interval", "Ticks between kick-off checks", 20, 1200),
                ConfigGuiEntry.bool("husbandryDisableEggChickenSpawn", "Disable Egg Chickens", "Thrown eggs won't spawn baby chickens")
        )));
        register(new ConfigGuiCategory("jeweller", "Jeweller", Items.DIAMOND, List.of(
                ConfigGuiEntry.bool("jewellerEnabled", "Jeweller Enabled", "Master toggle for the entire Jeweller system"),
                ConfigGuiEntry.bool("jewellerSmithingEnabled", "Smithing Gate", "Gate smithing table trims/gems behind Jeweller")
        )));
        register(new ConfigGuiCategory("slaughtering", "Slaughtering", Items.BEEF, List.of(
                ConfigGuiEntry.bool("slaughteringEnabled", "Slaughtering Enabled", "Master toggle for the slaughtering system"),
                ConfigGuiEntry.bool("slaughteringAllMobsByDefault", "All Mobs By Default", "If true, all mobs can be slaughtered unless blacklisted"),
                ConfigGuiEntry.integer("slaughteringCorpseDespawnTicks", "Corpse Despawn Ticks", "Ticks before a corpse entity despawns (6000 = 5 min)", 20, 72000),
                ConfigGuiEntry.integer("slaughteringCleaverCustomModelData", "Cleaver Model Data", "CustomModelData for Butcher's Cleaver", 0, 100),
                ConfigGuiEntry.decimal("slaughteringNonCleaverLootChance", "Non-Cleaver Loot Chance", "Chance for vanilla loot without cleaver", 0.0, 1.0, 0.05, 0.2),
                ConfigGuiEntry.string("slaughteringFailChancePerTier", "Fail Chance Per Tier", "Fail chance per slaughtering tier [T0, T1, T2, T3]"),
                ConfigGuiEntry.string("slaughteringAxeDissectionChances", "Axe Dissection Chances", "Axe dissection quality chances [poor, normal, excellent]"),
                ConfigGuiEntry.string("slaughteringCleaverDissectionChances", "Cleaver Dissect Chances", "Cleaver dissection quality chances [poor, normal, excellent]"),
                ConfigGuiEntry.string("slaughteringLootMultipliers", "Loot Multipliers", "Loot multiplier per quality [poor, normal, excellent]"),
                ConfigGuiEntry.string("slaughteringBaseMinutes", "Base Minutes", "Minutes required per tier [T1, T2, T3]")
        )));
        register(new ConfigGuiCategory("fishing", "Fishing", Items.FISHING_ROD, List.of(
                ConfigGuiEntry.string("fishingFailChancePerTier", "Fail Chance Per Tier", "Fail chance per fishing tier [T0, T1, T2, T3]"),
                ConfigGuiEntry.decimal("fishingGoodRodFailReduction", "Good Rod Fail Reduction", "Multiplier on fail chance when using Good Rod", 0.0, 1.0, 0.05, 0.25),
                ConfigGuiEntry.decimal("fishingSuperRodFailReduction", "Super Rod Fail Reduction", "Multiplier on fail chance when using Super Rod", 0.0, 1.0, 0.05, 0.25),
                ConfigGuiEntry.string("fishingBaseMinutes", "Base Minutes", "Minutes required per tier [T1, T2, T3]")
        )));
        register(new ConfigGuiCategory("death_loss", "Death Loss", Items.SKELETON_SKULL, List.of(
                ConfigGuiEntry.bool("knowledgeLossOnDeathEnabled", "Loss On Death Enabled", "Master toggle for knowledge loss on death"),
                ConfigGuiEntry.decimal("knowledgeLossMinutesPercentage", "Minutes Lost Percentage", "Fraction of current minutes lost on death", 0.0, 1.0, 0.05, 0.50),
                ConfigGuiEntry.integer("knowledgeLossTiers", "Tiers Lost", "Number of full tiers lost on death", 0, 5),
                ConfigGuiEntry.string("knowledgeLossExemptUsernames", "Exempt Usernames", "Staff usernames exempt from death loss")
        )));
    }

    private static void register(ConfigGuiCategory category) {
        CATEGORIES.put(category.getId(), category);
    }
}
