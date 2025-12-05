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
    }

    public static CraftingKnowledgeRule getForItem(Identifier itemId) {
        return RULES_BY_ITEM.get(itemId);
    }

    private static void register(CraftingKnowledgeRule rule, Identifier... itemIds) {
        for (Identifier itemId : itemIds) {
            RULES_BY_ITEM.put(itemId, rule);
        }
    }

    private static void registerWoodenToolRules() {
        Map<Integer, CraftingKnowledgeRule.TierChance> tierChances = new HashMap<>();

        tierChances.put(0, new CraftingKnowledgeRule.TierChance(0.20, 0.40)); // Tier 0: 20% success, 40% poor
        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                new Identifier(KnowledgeBound.MOD_ID, "wooden_tool_crafting"),
                KnowledgeRegistry.TOOLSMITHING_ID,
                0.10, // poor durability fraction
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
}
