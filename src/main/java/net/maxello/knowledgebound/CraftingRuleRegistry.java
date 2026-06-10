package net.maxello.knowledgebound;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class CraftingRuleRegistry {

    // rule map
    private static final Map<Identifier, CraftingKnowledgeRule> RULES_BY_ITEM = new HashMap<>();

    // tier map
    private static final Map<Identifier, Integer> ITEM_TIERS = new HashMap<>();

    public static void init() {
        KnowledgeBound.LOGGER.info("[KnowledgeBound] Registering crafting knowledge rules…");
        registerToolRules();
        registerArmorRules();
        registerWeaponRules();
        loadConfigOverrides();
    }

    /**
     * Returns the rule for the given crafted item ID, or null if none registered.
     */
    public static CraftingKnowledgeRule getForItem(Identifier itemId) {
        return RULES_BY_ITEM.get(itemId);
    }

    /**
     * Returns the crafting tier required for the given item.
     * Defaults to 0 if the item has no assigned tier.
     */
    public static int getItemTier(Identifier itemId) {
        return ITEM_TIERS.getOrDefault(itemId, 0);
    }

    /** Register a rule for a set of item IDs, all sharing the same item tier. */
    private static void registerWithTier(CraftingKnowledgeRule rule, int itemTier, Identifier... itemIds) {
        for (Identifier itemId : itemIds) {
            RULES_BY_ITEM.put(itemId, rule);
            ITEM_TIERS.put(itemId, itemTier);
        }
    }

    /** Load per-item tier overrides from config. */
    private static void loadConfigOverrides() {
        Map<String, Integer> overrides = KnowledgeBoundConfig.INSTANCE.itemCraftingTierOverrides;
        if (overrides == null) return;
        for (Map.Entry<String, Integer> entry : overrides.entrySet()) {
            try {
                Identifier id = Identifier.of(entry.getKey());
                ITEM_TIERS.put(id, entry.getValue());
            } catch (Exception e) {
                KnowledgeBound.LOGGER.warn("[KnowledgeBound] Invalid itemCraftingTierOverrides key: {}", entry.getKey());
            }
        }
    }

    // --------------------------------------------------
    //  Toolsmithing: pickaxes, axes, shovels, hoes
    // --------------------------------------------------

    private static void registerToolRules() {
        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                Identifier.of(KnowledgeBound.MOD_ID, "tool_crafting"),
                KnowledgeRegistry.TOOLSMITHING_ID,
                0.10           // poor tools have 10% of max durability
        );

        // Tier 0: Wooden tools
        registerWithTier(rule, 0,
                Identifier.of("minecraft", "wooden_axe"),
                Identifier.of("minecraft", "wooden_pickaxe"),
                Identifier.of("minecraft", "wooden_shovel"),
                Identifier.of("minecraft", "wooden_hoe")
        );

        // Tier 1: Stone tools
        registerWithTier(rule, 1,
                Identifier.of("minecraft", "stone_axe"),
                Identifier.of("minecraft", "stone_pickaxe"),
                Identifier.of("minecraft", "stone_shovel"),
                Identifier.of("minecraft", "stone_hoe")
        );

        // Tier 2: Iron tools
        registerWithTier(rule, 2,
                Identifier.of("minecraft", "iron_axe"),
                Identifier.of("minecraft", "iron_pickaxe"),
                Identifier.of("minecraft", "iron_shovel"),
                Identifier.of("minecraft", "iron_hoe")
        );

        // Tier 3: Diamond tools
        registerWithTier(rule, 3,
                Identifier.of("minecraft", "diamond_axe"),
                Identifier.of("minecraft", "diamond_pickaxe"),
                Identifier.of("minecraft", "diamond_shovel"),
                Identifier.of("minecraft", "diamond_hoe")
        );

        // Tier 4: Netherite tools
        registerWithTier(rule, 4,
                Identifier.of("minecraft", "netherite_axe"),
                Identifier.of("minecraft", "netherite_pickaxe"),
                Identifier.of("minecraft", "netherite_shovel"),
                Identifier.of("minecraft", "netherite_hoe")
        );

        // extra config items (default tier 0 unless overridden)
        for (String idStr : KnowledgeBoundConfig.INSTANCE.extraToolItems) {
            try {
                Identifier id = Identifier.of(idStr);
                RULES_BY_ITEM.put(id, rule);
                ITEM_TIERS.putIfAbsent(id, 0);
            } catch (Exception e) {
                KnowledgeBound.LOGGER.warn("[KnowledgeBound] Invalid extraToolItems id in config: {}", idStr);
            }
        }
    }

    // --------------------------------------------------
    //  Armouring: all vanilla armor pieces
    // --------------------------------------------------

    private static void registerArmorRules() {
        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                Identifier.of(KnowledgeBound.MOD_ID, "armor_crafting"),
                KnowledgeRegistry.ARMOURING_ID,
                0.10           // poor armor has 10% of max durability
        );

        // Tier 0: Leather
        registerWithTier(rule, 0,
                Identifier.of("minecraft", "leather_helmet"),
                Identifier.of("minecraft", "leather_chestplate"),
                Identifier.of("minecraft", "leather_leggings"),
                Identifier.of("minecraft", "leather_boots")
        );

        // Tier 1: Chainmail + Gold
        registerWithTier(rule, 1,
                Identifier.of("minecraft", "chainmail_helmet"),
                Identifier.of("minecraft", "chainmail_chestplate"),
                Identifier.of("minecraft", "chainmail_leggings"),
                Identifier.of("minecraft", "chainmail_boots"),
                Identifier.of("minecraft", "golden_helmet"),
                Identifier.of("minecraft", "golden_chestplate"),
                Identifier.of("minecraft", "golden_leggings"),
                Identifier.of("minecraft", "golden_boots")
        );

        // Tier 2: Iron + Turtle helmet
        registerWithTier(rule, 2,
                Identifier.of("minecraft", "iron_helmet"),
                Identifier.of("minecraft", "iron_chestplate"),
                Identifier.of("minecraft", "iron_leggings"),
                Identifier.of("minecraft", "iron_boots"),
                Identifier.of("minecraft", "turtle_helmet")
        );

        // Tier 3: Diamond
        registerWithTier(rule, 3,
                Identifier.of("minecraft", "diamond_helmet"),
                Identifier.of("minecraft", "diamond_chestplate"),
                Identifier.of("minecraft", "diamond_leggings"),
                Identifier.of("minecraft", "diamond_boots")
        );

        // Tier 4: Netherite
        registerWithTier(rule, 4,
                Identifier.of("minecraft", "netherite_helmet"),
                Identifier.of("minecraft", "netherite_chestplate"),
                Identifier.of("minecraft", "netherite_leggings"),
                Identifier.of("minecraft", "netherite_boots")
        );

        // extra config items
        for (String idStr : KnowledgeBoundConfig.INSTANCE.extraArmorItems) {
            try {
                Identifier id = Identifier.of(idStr);
                RULES_BY_ITEM.put(id, rule);
                ITEM_TIERS.putIfAbsent(id, 0);
            } catch (Exception e) {
                KnowledgeBound.LOGGER.warn("[KnowledgeBound] Invalid extraArmorItems id in config: {}", idStr);
            }
        }
    }

    // --------------------------------------------------
    //  Weaponsmithing: swords, bows, crossbows
    // --------------------------------------------------

    private static void registerWeaponRules() {
        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                Identifier.of(KnowledgeBound.MOD_ID, "weapon_crafting"),
                KnowledgeRegistry.WEAPONSMITHING_ID,
                0.10           // poor weapons have 10% of max durability
        );

        // Tier 0: Wooden sword
        registerWithTier(rule, 0,
                Identifier.of("minecraft", "wooden_sword")
        );

        // Tier 1: Stone sword, Golden sword, Bow
        registerWithTier(rule, 1,
                Identifier.of("minecraft", "stone_sword"),
                Identifier.of("minecraft", "golden_sword"),
                Identifier.of("minecraft", "bow")
        );

        // Tier 2: Iron sword, Crossbow
        registerWithTier(rule, 2,
                Identifier.of("minecraft", "iron_sword"),
                Identifier.of("minecraft", "crossbow")
        );

        // Tier 3: Diamond sword
        registerWithTier(rule, 3,
                Identifier.of("minecraft", "diamond_sword")
        );

        // Tier 4: Netherite sword
        registerWithTier(rule, 4,
                Identifier.of("minecraft", "netherite_sword")
        );

        // extra config items
        for (String idStr : KnowledgeBoundConfig.INSTANCE.extraWeaponItems) {
            try {
                Identifier id = Identifier.of(idStr);
                RULES_BY_ITEM.put(id, rule);
                ITEM_TIERS.putIfAbsent(id, 0);
            } catch (Exception e) {
                KnowledgeBound.LOGGER.warn("[KnowledgeBound] Invalid extraWeaponItems id in config: {}", idStr);
            }
        }
    }
}
