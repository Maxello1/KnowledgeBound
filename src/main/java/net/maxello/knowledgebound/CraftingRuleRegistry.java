package net.maxello.knowledgebound;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class CraftingRuleRegistry {

    // Rules keyed by crafted ITEM id
    private static final Map<Identifier, CraftingKnowledgeRule> RULES_BY_ITEM = new HashMap<>();

    public static void init() {
        KnowledgeBound.LOGGER.info("[KnowledgeBound] Registering crafting knowledge rules…");
        registerToolRules();
        registerArmorRules();
        registerWeaponRules();
    }

    /**
     * Returns the rule for the given crafted item ID, or null if none registered.
     */
    public static CraftingKnowledgeRule getForItem(Identifier itemId) {
        return RULES_BY_ITEM.get(itemId);
    }

    /** Basic helper: register a rule for a set of item IDs. */
    private static void register(CraftingKnowledgeRule rule, Identifier... itemIds) {
        for (Identifier itemId : itemIds) {
            RULES_BY_ITEM.put(itemId, rule);
        }
    }

    // --------------------------------------------------
    //  Toolsmithing: tools (currently wooden, stone, iron, diamond, netherite)
    //  Chances per knowledge tier come from config.toolsmithingChances
    // --------------------------------------------------

    private static void registerToolRules() {
        Map<Integer, CraftingKnowledgeRule.TierChance> tierChances = new HashMap<>();

        // Fill per-knowledge-tier chances (0..4) from config
        KnowledgeBoundConfig.CraftingTierChances[] cfgArr =
                KnowledgeBoundConfig.INSTANCE.toolsmithingChances;

        for (int tier = 0; tier < cfgArr.length; tier++) {
            KnowledgeBoundConfig.CraftingTierChances c = cfgArr[tier];
            c.normalize();

            // In TierChance, first param = goodChance, second = poorChance.
            // FailChance is implicit: 1 - (good + poor).
            double good = c.normalChance;
            double poor = c.poorChance;

            tierChances.put(tier, new CraftingKnowledgeRule.TierChance(good, poor));
        }

        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                Identifier.of(KnowledgeBound.MOD_ID, "tool_crafting"),
                KnowledgeRegistry.TOOLSMITHING_ID,
                0.10,          // poor tools have 10% of max durability
                tierChances
        );

        // Vanilla wooden tools
        register(
                rule,
                Identifier.of("minecraft", "wooden_sword"),
                Identifier.of("minecraft", "wooden_axe"),
                Identifier.of("minecraft", "wooden_pickaxe"),
                Identifier.of("minecraft", "wooden_shovel"),
                Identifier.of("minecraft", "wooden_hoe")
        );

        // Vanilla stone tools
        register(
                rule,
                Identifier.of("minecraft", "stone_sword"),
                Identifier.of("minecraft", "stone_axe"),
                Identifier.of("minecraft", "stone_pickaxe"),
                Identifier.of("minecraft", "stone_shovel"),
                Identifier.of("minecraft", "stone_hoe")
        );

        // Vanilla iron tools
        register(
                rule,
                Identifier.of("minecraft", "iron_sword"),
                Identifier.of("minecraft", "iron_axe"),
                Identifier.of("minecraft", "iron_pickaxe"),
                Identifier.of("minecraft", "iron_shovel"),
                Identifier.of("minecraft", "iron_hoe")
        );

        // Vanilla diamond tools
        register(
                rule,
                Identifier.of("minecraft", "diamond_sword"),
                Identifier.of("minecraft", "diamond_axe"),
                Identifier.of("minecraft", "diamond_pickaxe"),
                Identifier.of("minecraft", "diamond_shovel"),
                Identifier.of("minecraft", "diamond_hoe")
        );

        // Vanilla netherite tools
        register(
                rule,
                Identifier.of("minecraft", "netherite_sword"),
                Identifier.of("minecraft", "netherite_axe"),
                Identifier.of("minecraft", "netherite_pickaxe"),
                Identifier.of("minecraft", "netherite_shovel"),
                Identifier.of("minecraft", "netherite_hoe")
        );

        // Extra tool items from config (e.g. modded tools)
        for (String idStr : KnowledgeBoundConfig.INSTANCE.extraToolItems) {
            try {
                Identifier id = Identifier.of(idStr);
                RULES_BY_ITEM.put(id, rule);
            } catch (Exception e) {
                KnowledgeBound.LOGGER.warn("[KnowledgeBound] Invalid extraToolItems id in config: {}", idStr);
            }
        }
    }

    // --------------------------------------------------
    //  Armouring: all vanilla armor pieces
    //  Chances per knowledge tier come from config.armouringChances
    // --------------------------------------------------

    private static void registerArmorRules() {
        Map<Integer, CraftingKnowledgeRule.TierChance> tierChances = new HashMap<>();

        KnowledgeBoundConfig.CraftingTierChances[] cfgArr =
                KnowledgeBoundConfig.INSTANCE.armouringChances;

        for (int tier = 0; tier < cfgArr.length; tier++) {
            KnowledgeBoundConfig.CraftingTierChances c = cfgArr[tier];
            c.normalize();

            double good = c.normalChance;
            double poor = c.poorChance;

            tierChances.put(tier, new CraftingKnowledgeRule.TierChance(good, poor));
        }

        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                Identifier.of(KnowledgeBound.MOD_ID, "armor_crafting"),
                KnowledgeRegistry.ARMOURING_ID,
                0.10,          // poor armor has 10% of max durability
                tierChances
        );

        // Vanilla armor set
        register(
                rule,
                // Leather
                Identifier.of("minecraft", "leather_helmet"),
                Identifier.of("minecraft", "leather_chestplate"),
                Identifier.of("minecraft", "leather_leggings"),
                Identifier.of("minecraft", "leather_boots"),

                // Chainmail
                Identifier.of("minecraft", "chainmail_helmet"),
                Identifier.of("minecraft", "chainmail_chestplate"),
                Identifier.of("minecraft", "chainmail_leggings"),
                Identifier.of("minecraft", "chainmail_boots"),

                // Iron
                Identifier.of("minecraft", "iron_helmet"),
                Identifier.of("minecraft", "iron_chestplate"),
                Identifier.of("minecraft", "iron_leggings"),
                Identifier.of("minecraft", "iron_boots"),

                // Gold
                Identifier.of("minecraft", "golden_helmet"),
                Identifier.of("minecraft", "golden_chestplate"),
                Identifier.of("minecraft", "golden_leggings"),
                Identifier.of("minecraft", "golden_boots"),

                // Diamond
                Identifier.of("minecraft", "diamond_helmet"),
                Identifier.of("minecraft", "diamond_chestplate"),
                Identifier.of("minecraft", "diamond_leggings"),
                Identifier.of("minecraft", "diamond_boots"),

                // Netherite
                Identifier.of("minecraft", "netherite_helmet"),
                Identifier.of("minecraft", "netherite_chestplate"),
                Identifier.of("minecraft", "netherite_leggings"),
                Identifier.of("minecraft", "netherite_boots"),

                // Misc
                Identifier.of("minecraft", "turtle_helmet")
        );

        // Extra armor items from config (e.g. modded armor)
        for (String idStr : KnowledgeBoundConfig.INSTANCE.extraArmorItems) {
            try {
                Identifier id = Identifier.of(idStr);
                RULES_BY_ITEM.put(id, rule);
            } catch (Exception e) {
                KnowledgeBound.LOGGER.warn("[KnowledgeBound] Invalid extraArmorItems id in config: {}", idStr);
            }
        }
    }

    // --------------------------------------------------
    //  Weaponsmithing: swords only (for now)
    //  Chances per knowledge tier come from config.weaponsmithingChances
    // --------------------------------------------------

    private static void registerWeaponRules() {
        Map<Integer, CraftingKnowledgeRule.TierChance> tierChances = new HashMap<>();

        KnowledgeBoundConfig.CraftingTierChances[] cfgArr =
                KnowledgeBoundConfig.INSTANCE.weaponsmithingChances;

        for (int tier = 0; tier < cfgArr.length; tier++) {
            KnowledgeBoundConfig.CraftingTierChances c = cfgArr[tier];
            c.normalize();

            double good = c.normalChance;
            double poor = c.poorChance;

            tierChances.put(tier, new CraftingKnowledgeRule.TierChance(good, poor));
        }

        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                Identifier.of(KnowledgeBound.MOD_ID, "weapon_crafting"),
                KnowledgeRegistry.WEAPONSMITHING_ID,
                0.10,          // poor weapons have 10% of max durability
                tierChances
        );

        // Vanilla swords
        register(
                rule,
                Identifier.of("minecraft", "wooden_sword"),
                Identifier.of("minecraft", "stone_sword"),
                Identifier.of("minecraft", "iron_sword"),
                Identifier.of("minecraft", "golden_sword"),
                Identifier.of("minecraft", "diamond_sword"),
                Identifier.of("minecraft", "netherite_sword")
        );

        // Extra weapons from config (e.g. modded swords)
        for (String idStr : KnowledgeBoundConfig.INSTANCE.extraWeaponItems) {
            try {
                Identifier id = Identifier.of(idStr);
                RULES_BY_ITEM.put(id, rule);
            } catch (Exception e) {
                KnowledgeBound.LOGGER.warn("[KnowledgeBound] Invalid extraWeaponItems id in config: {}", idStr);
            }
        }
    }

    // --------------------------------------------------
    //  Helpers
    // --------------------------------------------------

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
