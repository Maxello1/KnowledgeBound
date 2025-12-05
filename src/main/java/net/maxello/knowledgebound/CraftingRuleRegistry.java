package net.maxello.knowledgebound;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class CraftingRuleRegistry {

    // Rules keyed by crafted ITEM id
    private static final Map<Identifier, CraftingKnowledgeRule> RULES_BY_ITEM = new HashMap<>();

    public static void init() {
        KnowledgeBound.LOGGER.info("[KnowledgeBound] Registering crafting knowledge rules…");
        registerWoodenToolRules();
        registerArmorRules();
    }

    public static CraftingKnowledgeRule getForItem(Identifier itemId) {
        return RULES_BY_ITEM.get(itemId);
    }

    private static void register(CraftingKnowledgeRule rule, Identifier... itemIds) {
        for (Identifier itemId : itemIds) {
            RULES_BY_ITEM.put(itemId, rule);
        }
    }

    // --------------------------------------------------
    //  Toolsmithing: wooden tools (you can extend later)
    // --------------------------------------------------

    private static void registerWoodenToolRules() {
        Map<Integer, CraftingKnowledgeRule.TierChance> tierChances = new HashMap<>();

        // TODO: tweak these chances to taste.
        // Example: at Tier 0 you often fail, sometimes get poor tools, rarely good ones.
        tierChances.put(0, new CraftingKnowledgeRule.TierChance(0.20, 0.40)); // 20% good, 40% poor, 40% fail
        tierChances.put(1, new CraftingKnowledgeRule.TierChance(0.40, 0.40)); // 40% good, 40% poor, 20% fail
        tierChances.put(2, new CraftingKnowledgeRule.TierChance(0.60, 0.30)); // 60% good, 30% poor, 10% fail
        tierChances.put(3, new CraftingKnowledgeRule.TierChance(0.80, 0.15)); // 80% good, 15% poor, 5% fail
        tierChances.put(4, new CraftingKnowledgeRule.TierChance(0.90, 0.09)); // 90% good, 9% poor, 1% fail

        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                new Identifier(KnowledgeBound.MOD_ID, "wooden_tool_crafting"),
                KnowledgeRegistry.TOOLSMITHING_ID,
                0.10,          // poor tools have 10% of max durability
                tierChances
        );

        register(
                rule,
                new Identifier("minecraft", "wooden_sword"),
                new Identifier("minecraft", "wooden_axe"),
                new Identifier("minecraft", "wooden_pickaxe"),
                new Identifier("minecraft", "wooden_shovel"),
                new Identifier("minecraft", "wooden_hoe")
        );
    }

    // --------------------------------------------------
    //  Armouring: all vanilla armor pieces
    // --------------------------------------------------

    private static void registerArmorRules() {
        Map<Integer, CraftingKnowledgeRule.TierChance> tierChances = new HashMap<>();

        // Same shape as tools – you can tune independently if you want.
        tierChances.put(0, new CraftingKnowledgeRule.TierChance(0.20, 0.40));
        tierChances.put(1, new CraftingKnowledgeRule.TierChance(0.40, 0.40));
        tierChances.put(2, new CraftingKnowledgeRule.TierChance(0.60, 0.30));
        tierChances.put(3, new CraftingKnowledgeRule.TierChance(0.80, 0.15));
        tierChances.put(4, new CraftingKnowledgeRule.TierChance(0.90, 0.09));

        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                new Identifier(KnowledgeBound.MOD_ID, "armor_crafting"),
                KnowledgeRegistry.ARMOURING_ID,
                0.10,          // poor armor has 10% of max durability
                tierChances
        );

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
    }
}
