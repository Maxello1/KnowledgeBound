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

        for (int tier = 0; tier <= 4; tier++) {
            int idx = clamp(tier, 0, cfgArr.length - 1);
            KnowledgeBoundConfig.CraftingTierChances c = cfgArr[idx];
            c.normalize();

            // In TierChance, first param = goodChance, second = poorChance.
            // FailChance is implicit: 1 - (good + poor).
            double good = c.normalChance;
            double poor = c.poorChance;

            tierChances.put(tier, new CraftingKnowledgeRule.TierChance(good, poor));
        }

        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                new Identifier(KnowledgeBound.MOD_ID, "tool_crafting"),
                KnowledgeRegistry.TOOLSMITHING_ID,
                0.10,          // poor tools have 10% of max durability
                tierChances
        );

        // Vanilla wooden tools
        register(
                rule,
                new Identifier("minecraft", "wooden_sword"),
                new Identifier("minecraft", "wooden_axe"),
                new Identifier("minecraft", "wooden_pickaxe"),
                new Identifier("minecraft", "wooden_shovel"),
                new Identifier("minecraft", "wooden_hoe")
        );

        // Vanilla stone tools
        register(
                rule,
                new Identifier("minecraft", "stone_sword"),
                new Identifier("minecraft", "stone_axe"),
                new Identifier("minecraft", "stone_pickaxe"),
                new Identifier("minecraft", "stone_shovel"),
                new Identifier("minecraft", "stone_hoe")
        );

        // Vanilla iron tools
        register(
                rule,
                new Identifier("minecraft", "iron_sword"),
                new Identifier("minecraft", "iron_axe"),
                new Identifier("minecraft", "iron_pickaxe"),
                new Identifier("minecraft", "iron_shovel"),
                new Identifier("minecraft", "iron_hoe")
        );

        // Vanilla diamond tools
        register(
                rule,
                new Identifier("minecraft", "diamond_sword"),
                new Identifier("minecraft", "diamond_axe"),
                new Identifier("minecraft", "diamond_pickaxe"),
                new Identifier("minecraft", "diamond_shovel"),
                new Identifier("minecraft", "diamond_hoe")
        );

        // Vanilla netherite tools
        register(
                rule,
                new Identifier("minecraft", "netherite_sword"),
                new Identifier("minecraft", "netherite_axe"),
                new Identifier("minecraft", "netherite_pickaxe"),
                new Identifier("minecraft", "netherite_shovel"),
                new Identifier("minecraft", "netherite_hoe")
        );

        // Extra tool items from config (e.g. modded tools)
        for (String idStr : KnowledgeBoundConfig.INSTANCE.extraToolItems) {
            try {
                Identifier id = new Identifier(idStr);
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

        for (int tier = 0; tier <= 4; tier++) {
            int idx = clamp(tier, 0, cfgArr.length - 1);
            KnowledgeBoundConfig.CraftingTierChances c = cfgArr[idx];
            c.normalize();

            double good = c.normalChance;
            double poor = c.poorChance;

            tierChances.put(tier, new CraftingKnowledgeRule.TierChance(good, poor));
        }

        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                new Identifier(KnowledgeBound.MOD_ID, "armor_crafting"),
                KnowledgeRegistry.ARMOURING_ID,
                0.10,          // poor armor has 10% of max durability
                tierChances
        );

        // Vanilla armor set
        register(
                rule,
                // Leather
                new Identifier("minecraft", "leather_helmet"),
                new Identifier("minecraft", "leather_chestplate"),
                new Identifier("minecraft", "leather_leggings"),
                new Identifier("minecraft", "leather_boots"),

                // Chainmail
                new Identifier("minecraft", "chainmail_helmet"),
                new Identifier("minecraft", "chainmail_chestplate"),
                new Identifier("minecraft", "chainmail_leggings"),
                new Identifier("minecraft", "chainmail_boots"),

                // Iron
                new Identifier("minecraft", "iron_helmet"),
                new Identifier("minecraft", "iron_chestplate"),
                new Identifier("minecraft", "iron_leggings"),
                new Identifier("minecraft", "iron_boots"),

                // Gold
                new Identifier("minecraft", "golden_helmet"),
                new Identifier("minecraft", "golden_chestplate"),
                new Identifier("minecraft", "golden_leggings"),
                new Identifier("minecraft", "golden_boots"),

                // Diamond
                new Identifier("minecraft", "diamond_helmet"),
                new Identifier("minecraft", "diamond_chestplate"),
                new Identifier("minecraft", "diamond_leggings"),
                new Identifier("minecraft", "diamond_boots"),

                // Netherite
                new Identifier("minecraft", "netherite_helmet"),
                new Identifier("minecraft", "netherite_chestplate"),
                new Identifier("minecraft", "netherite_leggings"),
                new Identifier("minecraft", "netherite_boots"),

                // Misc
                new Identifier("minecraft", "turtle_helmet")
        );

        // Extra armor items from config (e.g. modded armor)
        for (String idStr : KnowledgeBoundConfig.INSTANCE.extraArmorItems) {
            try {
                Identifier id = new Identifier(idStr);
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

        for (int tier = 0; tier <= 4; tier++) {
            int idx = clamp(tier, 0, cfgArr.length - 1);
            KnowledgeBoundConfig.CraftingTierChances c = cfgArr[idx];
            c.normalize();

            double good = c.normalChance;
            double poor = c.poorChance;

            tierChances.put(tier, new CraftingKnowledgeRule.TierChance(good, poor));
        }

        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                new Identifier(KnowledgeBound.MOD_ID, "weapon_crafting"),
                KnowledgeRegistry.WEAPONSMITHING_ID,
                0.10,          // poor weapons have 10% of max durability
                tierChances
        );

        // Vanilla swords
        register(
                rule,
                new Identifier("minecraft", "wooden_sword"),
                new Identifier("minecraft", "stone_sword"),
                new Identifier("minecraft", "iron_sword"),
                new Identifier("minecraft", "golden_sword"),
                new Identifier("minecraft", "diamond_sword"),
                new Identifier("minecraft", "netherite_sword")
        );

        // Extra weapons from config (e.g. modded swords)
        for (String idStr : KnowledgeBoundConfig.INSTANCE.extraWeaponItems) {
            try {
                Identifier id = new Identifier(idStr);
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
