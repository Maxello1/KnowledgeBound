package net.maxello.knowledgebound;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class CraftingRuleRegistry {

    // rule map
    private static final Map<Identifier, CraftingKnowledgeRule> RULES_BY_ITEM = new HashMap<>();

    // tier map
    private static final Map<Identifier, Integer> ITEM_TIERS = new HashMap<>();

    // items that are completely blocked from crafting
    private static final java.util.Set<Identifier> BLOCKED_ITEMS = new java.util.HashSet<>();

    public static void init() {
        KnowledgeBound.LOGGER.info("[KnowledgeBound] Registering crafting knowledge rules…");
        registerToolRules();
        registerArmorRules();
        registerWeaponRules();
        registerCarpentryRules();
        registerMasonryRules();
        registerBlockedItems();
        loadConfigOverrides();
    }

    public static CraftingKnowledgeRule getForItem(Identifier itemId) {
        return RULES_BY_ITEM.get(itemId);
    }

    public static int getItemTier(Identifier itemId) {
        return ITEM_TIERS.getOrDefault(itemId, 0);
    }

    public static boolean isBlocked(Identifier itemId) {
        return BLOCKED_ITEMS.contains(itemId);
    }

    private static void registerWithTier(CraftingKnowledgeRule rule, int itemTier, Identifier... itemIds) {
        for (Identifier itemId : itemIds) {
            RULES_BY_ITEM.put(itemId, rule);
            ITEM_TIERS.put(itemId, itemTier);
        }
    }

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
                0.10
        );

        registerWithTier(rule, 0,
                Identifier.of("minecraft", "wooden_axe"),
                Identifier.of("minecraft", "wooden_pickaxe"),
                Identifier.of("minecraft", "wooden_shovel"),
                Identifier.of("minecraft", "wooden_hoe")
        );

        registerWithTier(rule, 1,
                Identifier.of("minecraft", "stone_axe"),
                Identifier.of("minecraft", "stone_pickaxe"),
                Identifier.of("minecraft", "stone_shovel"),
                Identifier.of("minecraft", "stone_hoe")
        );

        registerWithTier(rule, 2,
                Identifier.of("minecraft", "iron_axe"),
                Identifier.of("minecraft", "iron_pickaxe"),
                Identifier.of("minecraft", "iron_shovel"),
                Identifier.of("minecraft", "iron_hoe")
        );

        registerWithTier(rule, 3,
                Identifier.of("minecraft", "diamond_axe"),
                Identifier.of("minecraft", "diamond_pickaxe"),
                Identifier.of("minecraft", "diamond_shovel"),
                Identifier.of("minecraft", "diamond_hoe")
        );

        registerWithTier(rule, 4,
                Identifier.of("minecraft", "netherite_axe"),
                Identifier.of("minecraft", "netherite_pickaxe"),
                Identifier.of("minecraft", "netherite_shovel"),
                Identifier.of("minecraft", "netherite_hoe")
        );

        for (String idStr : KnowledgeBoundConfig.INSTANCE.extraToolItems) {
            try {
                Identifier id = Identifier.of(idStr);
                RULES_BY_ITEM.put(id, rule);
                ITEM_TIERS.putIfAbsent(id, 0);
            } catch (Exception e) {
                KnowledgeBound.LOGGER.warn("[KnowledgeBound] Invalid extraToolItems id: {}", idStr);
            }
        }
    }

    // --------------------------------------------------
    //  Armouring
    // --------------------------------------------------

    private static void registerArmorRules() {
        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                Identifier.of(KnowledgeBound.MOD_ID, "armor_crafting"),
                KnowledgeRegistry.ARMOURING_ID,
                0.10
        );

        registerWithTier(rule, 0,
                Identifier.of("minecraft", "leather_helmet"),
                Identifier.of("minecraft", "leather_chestplate"),
                Identifier.of("minecraft", "leather_leggings"),
                Identifier.of("minecraft", "leather_boots")
        );

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

        registerWithTier(rule, 2,
                Identifier.of("minecraft", "iron_helmet"),
                Identifier.of("minecraft", "iron_chestplate"),
                Identifier.of("minecraft", "iron_leggings"),
                Identifier.of("minecraft", "iron_boots"),
                Identifier.of("minecraft", "turtle_helmet")
        );

        registerWithTier(rule, 3,
                Identifier.of("minecraft", "diamond_helmet"),
                Identifier.of("minecraft", "diamond_chestplate"),
                Identifier.of("minecraft", "diamond_leggings"),
                Identifier.of("minecraft", "diamond_boots")
        );

        registerWithTier(rule, 4,
                Identifier.of("minecraft", "netherite_helmet"),
                Identifier.of("minecraft", "netherite_chestplate"),
                Identifier.of("minecraft", "netherite_leggings"),
                Identifier.of("minecraft", "netherite_boots")
        );

        for (String idStr : KnowledgeBoundConfig.INSTANCE.extraArmorItems) {
            try {
                Identifier id = Identifier.of(idStr);
                RULES_BY_ITEM.put(id, rule);
                ITEM_TIERS.putIfAbsent(id, 0);
            } catch (Exception e) {
                KnowledgeBound.LOGGER.warn("[KnowledgeBound] Invalid extraArmorItems id: {}", idStr);
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
                0.10
        );

        registerWithTier(rule, 0, Identifier.of("minecraft", "wooden_sword"));

        registerWithTier(rule, 1,
                Identifier.of("minecraft", "stone_sword"),
                Identifier.of("minecraft", "golden_sword"),
                Identifier.of("minecraft", "bow")
        );

        registerWithTier(rule, 2,
                Identifier.of("minecraft", "iron_sword"),
                Identifier.of("minecraft", "crossbow")
        );

        registerWithTier(rule, 3, Identifier.of("minecraft", "diamond_sword"));
        registerWithTier(rule, 4, Identifier.of("minecraft", "netherite_sword"));

        for (String idStr : KnowledgeBoundConfig.INSTANCE.extraWeaponItems) {
            try {
                Identifier id = Identifier.of(idStr);
                RULES_BY_ITEM.put(id, rule);
                ITEM_TIERS.putIfAbsent(id, 0);
            } catch (Exception e) {
                KnowledgeBound.LOGGER.warn("[KnowledgeBound] Invalid extraWeaponItems id: {}", idStr);
            }
        }
    }

    // --------------------------------------------------
    //  Carpentry (class job, 3 tiers)
    // --------------------------------------------------

    private static void registerCarpentryRules() {
        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                Identifier.of(KnowledgeBound.MOD_ID, "carpentry_crafting"),
                KnowledgeRegistry.CARPENTRY_ID,
                0.10
        );

        // Tier 0: basic wood stuff
        registerWithTier(rule, 0,
                Identifier.of("minecraft", "oak_planks"),
                Identifier.of("minecraft", "spruce_planks"),
                Identifier.of("minecraft", "birch_planks"),
                Identifier.of("minecraft", "jungle_planks"),
                Identifier.of("minecraft", "acacia_planks"),
                Identifier.of("minecraft", "dark_oak_planks"),
                Identifier.of("minecraft", "mangrove_planks"),
                Identifier.of("minecraft", "cherry_planks"),
                Identifier.of("minecraft", "bamboo_planks"),
                Identifier.of("minecraft", "crimson_planks"),
                Identifier.of("minecraft", "warped_planks"),
                Identifier.of("minecraft", "stick"),
                Identifier.of("minecraft", "torch"),
                Identifier.of("minecraft", "soul_torch")
        );

        // Tier 1: banners, bookshelves, bowls, buttons, campfires, doors, fences, levers, stairs, slabs, trapped chests
        registerWithTier(rule, 1,
                // banners
                Identifier.of("minecraft", "white_banner"),
                Identifier.of("minecraft", "orange_banner"),
                Identifier.of("minecraft", "magenta_banner"),
                Identifier.of("minecraft", "light_blue_banner"),
                Identifier.of("minecraft", "yellow_banner"),
                Identifier.of("minecraft", "lime_banner"),
                Identifier.of("minecraft", "pink_banner"),
                Identifier.of("minecraft", "gray_banner"),
                Identifier.of("minecraft", "light_gray_banner"),
                Identifier.of("minecraft", "cyan_banner"),
                Identifier.of("minecraft", "purple_banner"),
                Identifier.of("minecraft", "blue_banner"),
                Identifier.of("minecraft", "brown_banner"),
                Identifier.of("minecraft", "green_banner"),
                Identifier.of("minecraft", "red_banner"),
                Identifier.of("minecraft", "black_banner"),
                // bookshelf
                Identifier.of("minecraft", "bookshelf"),
                // bowls
                Identifier.of("minecraft", "bowl"),
                // wooden buttons
                Identifier.of("minecraft", "oak_button"),
                Identifier.of("minecraft", "spruce_button"),
                Identifier.of("minecraft", "birch_button"),
                Identifier.of("minecraft", "jungle_button"),
                Identifier.of("minecraft", "acacia_button"),
                Identifier.of("minecraft", "dark_oak_button"),
                Identifier.of("minecraft", "mangrove_button"),
                Identifier.of("minecraft", "cherry_button"),
                Identifier.of("minecraft", "bamboo_button"),
                Identifier.of("minecraft", "crimson_button"),
                Identifier.of("minecraft", "warped_button"),
                // campfire
                Identifier.of("minecraft", "campfire"),
                Identifier.of("minecraft", "soul_campfire"),
                // doors
                Identifier.of("minecraft", "oak_door"),
                Identifier.of("minecraft", "spruce_door"),
                Identifier.of("minecraft", "birch_door"),
                Identifier.of("minecraft", "jungle_door"),
                Identifier.of("minecraft", "acacia_door"),
                Identifier.of("minecraft", "dark_oak_door"),
                Identifier.of("minecraft", "mangrove_door"),
                Identifier.of("minecraft", "cherry_door"),
                Identifier.of("minecraft", "crimson_door"),
                Identifier.of("minecraft", "warped_door"),
                // trapdoors
                Identifier.of("minecraft", "oak_trapdoor"),
                Identifier.of("minecraft", "spruce_trapdoor"),
                Identifier.of("minecraft", "birch_trapdoor"),
                Identifier.of("minecraft", "jungle_trapdoor"),
                Identifier.of("minecraft", "acacia_trapdoor"),
                Identifier.of("minecraft", "dark_oak_trapdoor"),
                Identifier.of("minecraft", "mangrove_trapdoor"),
                Identifier.of("minecraft", "cherry_trapdoor"),
                Identifier.of("minecraft", "crimson_trapdoor"),
                Identifier.of("minecraft", "warped_trapdoor"),
                // fences
                Identifier.of("minecraft", "oak_fence"),
                Identifier.of("minecraft", "spruce_fence"),
                Identifier.of("minecraft", "birch_fence"),
                Identifier.of("minecraft", "jungle_fence"),
                Identifier.of("minecraft", "acacia_fence"),
                Identifier.of("minecraft", "dark_oak_fence"),
                Identifier.of("minecraft", "mangrove_fence"),
                Identifier.of("minecraft", "cherry_fence"),
                Identifier.of("minecraft", "crimson_fence"),
                Identifier.of("minecraft", "warped_fence"),
                // lever (shared with masonry)
                Identifier.of("minecraft", "lever"),
                // stairs
                Identifier.of("minecraft", "oak_stairs"),
                Identifier.of("minecraft", "spruce_stairs"),
                Identifier.of("minecraft", "birch_stairs"),
                Identifier.of("minecraft", "jungle_stairs"),
                Identifier.of("minecraft", "acacia_stairs"),
                Identifier.of("minecraft", "dark_oak_stairs"),
                Identifier.of("minecraft", "mangrove_stairs"),
                Identifier.of("minecraft", "cherry_stairs"),
                Identifier.of("minecraft", "crimson_stairs"),
                Identifier.of("minecraft", "warped_stairs"),
                // slabs
                Identifier.of("minecraft", "oak_slab"),
                Identifier.of("minecraft", "spruce_slab"),
                Identifier.of("minecraft", "birch_slab"),
                Identifier.of("minecraft", "jungle_slab"),
                Identifier.of("minecraft", "acacia_slab"),
                Identifier.of("minecraft", "dark_oak_slab"),
                Identifier.of("minecraft", "mangrove_slab"),
                Identifier.of("minecraft", "cherry_slab"),
                Identifier.of("minecraft", "crimson_slab"),
                Identifier.of("minecraft", "warped_slab"),
                // trapped chest
                Identifier.of("minecraft", "trapped_chest")
        );

        // Tier 2: bamboo mosaic, beds, beehive, chiseled bookshelf, crafting table,
        //         cartography/fletching, gates, signs, shelves, pressure plates,
        //         paintings, item frames, full logs
        registerWithTier(rule, 2,
                Identifier.of("minecraft", "bamboo_mosaic"),
                Identifier.of("minecraft", "bamboo_mosaic_stairs"),
                Identifier.of("minecraft", "bamboo_mosaic_slab"),
                // beds
                Identifier.of("minecraft", "white_bed"),
                Identifier.of("minecraft", "orange_bed"),
                Identifier.of("minecraft", "magenta_bed"),
                Identifier.of("minecraft", "light_blue_bed"),
                Identifier.of("minecraft", "yellow_bed"),
                Identifier.of("minecraft", "lime_bed"),
                Identifier.of("minecraft", "pink_bed"),
                Identifier.of("minecraft", "gray_bed"),
                Identifier.of("minecraft", "light_gray_bed"),
                Identifier.of("minecraft", "cyan_bed"),
                Identifier.of("minecraft", "purple_bed"),
                Identifier.of("minecraft", "blue_bed"),
                Identifier.of("minecraft", "brown_bed"),
                Identifier.of("minecraft", "green_bed"),
                Identifier.of("minecraft", "red_bed"),
                Identifier.of("minecraft", "black_bed"),
                // beehive (also beekeeping 1 - handled separately)
                Identifier.of("minecraft", "beehive"),
                // chiseled bookshelf
                Identifier.of("minecraft", "chiseled_bookshelf"),
                // crafting table
                Identifier.of("minecraft", "crafting_table"),
                // cartography and fletching table
                Identifier.of("minecraft", "cartography_table"),
                Identifier.of("minecraft", "fletching_table"),
                // fence gates
                Identifier.of("minecraft", "oak_fence_gate"),
                Identifier.of("minecraft", "spruce_fence_gate"),
                Identifier.of("minecraft", "birch_fence_gate"),
                Identifier.of("minecraft", "jungle_fence_gate"),
                Identifier.of("minecraft", "acacia_fence_gate"),
                Identifier.of("minecraft", "dark_oak_fence_gate"),
                Identifier.of("minecraft", "mangrove_fence_gate"),
                Identifier.of("minecraft", "cherry_fence_gate"),
                Identifier.of("minecraft", "crimson_fence_gate"),
                Identifier.of("minecraft", "warped_fence_gate"),
                // hanging signs
                Identifier.of("minecraft", "oak_hanging_sign"),
                Identifier.of("minecraft", "spruce_hanging_sign"),
                Identifier.of("minecraft", "birch_hanging_sign"),
                Identifier.of("minecraft", "jungle_hanging_sign"),
                Identifier.of("minecraft", "acacia_hanging_sign"),
                Identifier.of("minecraft", "dark_oak_hanging_sign"),
                Identifier.of("minecraft", "mangrove_hanging_sign"),
                Identifier.of("minecraft", "cherry_hanging_sign"),
                Identifier.of("minecraft", "crimson_hanging_sign"),
                Identifier.of("minecraft", "warped_hanging_sign"),
                Identifier.of("minecraft", "bamboo_hanging_sign"),
                // signs
                Identifier.of("minecraft", "oak_sign"),
                Identifier.of("minecraft", "spruce_sign"),
                Identifier.of("minecraft", "birch_sign"),
                Identifier.of("minecraft", "jungle_sign"),
                Identifier.of("minecraft", "acacia_sign"),
                Identifier.of("minecraft", "dark_oak_sign"),
                Identifier.of("minecraft", "mangrove_sign"),
                Identifier.of("minecraft", "cherry_sign"),
                Identifier.of("minecraft", "crimson_sign"),
                Identifier.of("minecraft", "warped_sign"),
                Identifier.of("minecraft", "bamboo_sign"),
                // wooden pressure plates
                Identifier.of("minecraft", "oak_pressure_plate"),
                Identifier.of("minecraft", "spruce_pressure_plate"),
                Identifier.of("minecraft", "birch_pressure_plate"),
                Identifier.of("minecraft", "jungle_pressure_plate"),
                Identifier.of("minecraft", "acacia_pressure_plate"),
                Identifier.of("minecraft", "dark_oak_pressure_plate"),
                Identifier.of("minecraft", "mangrove_pressure_plate"),
                Identifier.of("minecraft", "cherry_pressure_plate"),
                Identifier.of("minecraft", "crimson_pressure_plate"),
                Identifier.of("minecraft", "warped_pressure_plate"),
                // painting, item frame
                Identifier.of("minecraft", "painting"),
                Identifier.of("minecraft", "item_frame"),
                Identifier.of("minecraft", "glow_item_frame"),
                // full logs (4 logs -> 1 wood block)
                Identifier.of("minecraft", "oak_wood"),
                Identifier.of("minecraft", "spruce_wood"),
                Identifier.of("minecraft", "birch_wood"),
                Identifier.of("minecraft", "jungle_wood"),
                Identifier.of("minecraft", "acacia_wood"),
                Identifier.of("minecraft", "dark_oak_wood"),
                Identifier.of("minecraft", "mangrove_wood"),
                Identifier.of("minecraft", "cherry_wood"),
                Identifier.of("minecraft", "crimson_hyphae"),
                Identifier.of("minecraft", "warped_hyphae"),
                Identifier.of("minecraft", "stripped_oak_wood"),
                Identifier.of("minecraft", "stripped_spruce_wood"),
                Identifier.of("minecraft", "stripped_birch_wood"),
                Identifier.of("minecraft", "stripped_jungle_wood"),
                Identifier.of("minecraft", "stripped_acacia_wood"),
                Identifier.of("minecraft", "stripped_dark_oak_wood"),
                Identifier.of("minecraft", "stripped_mangrove_wood"),
                Identifier.of("minecraft", "stripped_cherry_wood"),
                Identifier.of("minecraft", "stripped_crimson_hyphae"),
                Identifier.of("minecraft", "stripped_warped_hyphae")
        );

        // Tier 3: chests, barrels, armor stand, composter, jukebox, lectern, loom,
        //         scaffolding, note block, ladders
        registerWithTier(rule, 3,
                Identifier.of("minecraft", "chest"),
                Identifier.of("minecraft", "barrel"),
                Identifier.of("minecraft", "armor_stand"),
                Identifier.of("minecraft", "composter"),
                Identifier.of("minecraft", "jukebox"),
                Identifier.of("minecraft", "lectern"),
                Identifier.of("minecraft", "loom"),
                Identifier.of("minecraft", "scaffolding"),
                Identifier.of("minecraft", "note_block"),
                Identifier.of("minecraft", "ladder")
        );

        // bamboo items are +1 tier, so register specific overrides
        // bamboo door = tier 2 (normal door is 1), bamboo fence = tier 2, etc.
        // bamboo fence gate = tier 3 (normal gate is 2)
        // These override the earlier registrations above
        ITEM_TIERS.put(Identifier.of("minecraft", "bamboo_door"), 2);
        ITEM_TIERS.put(Identifier.of("minecraft", "bamboo_trapdoor"), 2);
        ITEM_TIERS.put(Identifier.of("minecraft", "bamboo_fence"), 2);
        ITEM_TIERS.put(Identifier.of("minecraft", "bamboo_fence_gate"), 3);
        ITEM_TIERS.put(Identifier.of("minecraft", "bamboo_stairs"), 2);
        ITEM_TIERS.put(Identifier.of("minecraft", "bamboo_slab"), 2);
        ITEM_TIERS.put(Identifier.of("minecraft", "bamboo_pressure_plate"), 3);

        // register bamboo items with the carpentry rule if not already
        Identifier[] bambooItems = {
                Identifier.of("minecraft", "bamboo_door"),
                Identifier.of("minecraft", "bamboo_trapdoor"),
                Identifier.of("minecraft", "bamboo_fence"),
                Identifier.of("minecraft", "bamboo_fence_gate"),
                Identifier.of("minecraft", "bamboo_stairs"),
                Identifier.of("minecraft", "bamboo_slab"),
                Identifier.of("minecraft", "bamboo_pressure_plate")
        };
        for (Identifier id : bambooItems) {
            RULES_BY_ITEM.putIfAbsent(id, rule);
        }

        // extra carpentry items from config
        for (String idStr : KnowledgeBoundConfig.INSTANCE.extraCarpentryItems) {
            try {
                Identifier id = Identifier.of(idStr);
                RULES_BY_ITEM.put(id, rule);
                ITEM_TIERS.putIfAbsent(id, 0);
            } catch (Exception e) {
                KnowledgeBound.LOGGER.warn("[KnowledgeBound] Invalid extraCarpentryItems id: {}", idStr);
            }
        }
    }

    // --------------------------------------------------
    //  Masonry (class job, 3 tiers)
    // --------------------------------------------------

    private static void registerMasonryRules() {
        CraftingKnowledgeRule rule = new CraftingKnowledgeRule(
                Identifier.of(KnowledgeBound.MOD_ID, "masonry_crafting"),
                KnowledgeRegistry.MASONRY_ID,
                0.10
        );

        // Tier 1: stairs/slabs/buttons/pressure plates, levers, furnace, smoker,
        //         dyed glass, concrete
        registerWithTier(rule, 1,
                // stone-type stairs
                Identifier.of("minecraft", "stone_stairs"),
                Identifier.of("minecraft", "cobblestone_stairs"),
                Identifier.of("minecraft", "stone_brick_stairs"),
                Identifier.of("minecraft", "mossy_stone_brick_stairs"),
                Identifier.of("minecraft", "mossy_cobblestone_stairs"),
                Identifier.of("minecraft", "granite_stairs"),
                Identifier.of("minecraft", "polished_granite_stairs"),
                Identifier.of("minecraft", "diorite_stairs"),
                Identifier.of("minecraft", "polished_diorite_stairs"),
                Identifier.of("minecraft", "andesite_stairs"),
                Identifier.of("minecraft", "polished_andesite_stairs"),
                Identifier.of("minecraft", "sandstone_stairs"),
                Identifier.of("minecraft", "red_sandstone_stairs"),
                Identifier.of("minecraft", "prismarine_stairs"),
                Identifier.of("minecraft", "prismarine_brick_stairs"),
                Identifier.of("minecraft", "dark_prismarine_stairs"),
                Identifier.of("minecraft", "nether_brick_stairs"),
                Identifier.of("minecraft", "red_nether_brick_stairs"),
                Identifier.of("minecraft", "quartz_stairs"),
                Identifier.of("minecraft", "purpur_stairs"),
                Identifier.of("minecraft", "end_stone_brick_stairs"),
                Identifier.of("minecraft", "blackstone_stairs"),
                Identifier.of("minecraft", "polished_blackstone_stairs"),
                Identifier.of("minecraft", "polished_blackstone_brick_stairs"),
                Identifier.of("minecraft", "deepslate_brick_stairs"),
                Identifier.of("minecraft", "deepslate_tile_stairs"),
                Identifier.of("minecraft", "cobbled_deepslate_stairs"),
                Identifier.of("minecraft", "polished_deepslate_stairs"),
                Identifier.of("minecraft", "tuff_stairs"),
                Identifier.of("minecraft", "polished_tuff_stairs"),
                Identifier.of("minecraft", "tuff_brick_stairs"),
                Identifier.of("minecraft", "mud_brick_stairs"),
                // stone-type slabs
                Identifier.of("minecraft", "stone_slab"),
                Identifier.of("minecraft", "cobblestone_slab"),
                Identifier.of("minecraft", "stone_brick_slab"),
                Identifier.of("minecraft", "mossy_stone_brick_slab"),
                Identifier.of("minecraft", "mossy_cobblestone_slab"),
                Identifier.of("minecraft", "granite_slab"),
                Identifier.of("minecraft", "polished_granite_slab"),
                Identifier.of("minecraft", "diorite_slab"),
                Identifier.of("minecraft", "polished_diorite_slab"),
                Identifier.of("minecraft", "andesite_slab"),
                Identifier.of("minecraft", "polished_andesite_slab"),
                Identifier.of("minecraft", "sandstone_slab"),
                Identifier.of("minecraft", "red_sandstone_slab"),
                Identifier.of("minecraft", "prismarine_slab"),
                Identifier.of("minecraft", "prismarine_brick_slab"),
                Identifier.of("minecraft", "dark_prismarine_slab"),
                Identifier.of("minecraft", "nether_brick_slab"),
                Identifier.of("minecraft", "red_nether_brick_slab"),
                Identifier.of("minecraft", "quartz_slab"),
                Identifier.of("minecraft", "purpur_slab"),
                Identifier.of("minecraft", "end_stone_brick_slab"),
                Identifier.of("minecraft", "blackstone_slab"),
                Identifier.of("minecraft", "polished_blackstone_slab"),
                Identifier.of("minecraft", "polished_blackstone_brick_slab"),
                Identifier.of("minecraft", "deepslate_brick_slab"),
                Identifier.of("minecraft", "deepslate_tile_slab"),
                Identifier.of("minecraft", "cobbled_deepslate_slab"),
                Identifier.of("minecraft", "polished_deepslate_slab"),
                Identifier.of("minecraft", "tuff_slab"),
                Identifier.of("minecraft", "polished_tuff_slab"),
                Identifier.of("minecraft", "tuff_brick_slab"),
                Identifier.of("minecraft", "mud_brick_slab"),
                // stone buttons and pressure plates
                Identifier.of("minecraft", "stone_button"),
                Identifier.of("minecraft", "polished_blackstone_button"),
                Identifier.of("minecraft", "stone_pressure_plate"),
                Identifier.of("minecraft", "polished_blackstone_pressure_plate"),
                // lever (shared with carpentry)
                Identifier.of("minecraft", "lever"),
                // furnace
                Identifier.of("minecraft", "furnace"),
                // smoker
                Identifier.of("minecraft", "smoker"),
                // concrete (all colors)
                Identifier.of("minecraft", "white_concrete"),
                Identifier.of("minecraft", "orange_concrete"),
                Identifier.of("minecraft", "magenta_concrete"),
                Identifier.of("minecraft", "light_blue_concrete"),
                Identifier.of("minecraft", "yellow_concrete"),
                Identifier.of("minecraft", "lime_concrete"),
                Identifier.of("minecraft", "pink_concrete"),
                Identifier.of("minecraft", "gray_concrete"),
                Identifier.of("minecraft", "light_gray_concrete"),
                Identifier.of("minecraft", "cyan_concrete"),
                Identifier.of("minecraft", "purple_concrete"),
                Identifier.of("minecraft", "blue_concrete"),
                Identifier.of("minecraft", "brown_concrete"),
                Identifier.of("minecraft", "green_concrete"),
                Identifier.of("minecraft", "red_concrete"),
                Identifier.of("minecraft", "black_concrete")
        );

        // Tier 2: stone conversions, bricks, blast furnace, pots, glass, glowstone,
        //         stonecutter
        registerWithTier(rule, 2,
                // bricks
                Identifier.of("minecraft", "bricks"),
                Identifier.of("minecraft", "stone_bricks"),
                Identifier.of("minecraft", "mossy_stone_bricks"),
                Identifier.of("minecraft", "nether_bricks"),
                Identifier.of("minecraft", "red_nether_bricks"),
                Identifier.of("minecraft", "deepslate_bricks"),
                Identifier.of("minecraft", "deepslate_tiles"),
                Identifier.of("minecraft", "polished_deepslate"),
                Identifier.of("minecraft", "polished_blackstone_bricks"),
                Identifier.of("minecraft", "tuff_bricks"),
                Identifier.of("minecraft", "polished_tuff"),
                Identifier.of("minecraft", "mud_bricks"),
                // blast furnace
                Identifier.of("minecraft", "blast_furnace"),
                // pots
                Identifier.of("minecraft", "flower_pot"),
                Identifier.of("minecraft", "decorated_pot"),
                // glass
                Identifier.of("minecraft", "glass"),
                Identifier.of("minecraft", "glass_pane"),
                Identifier.of("minecraft", "tinted_glass"),
                Identifier.of("minecraft", "white_stained_glass"),
                Identifier.of("minecraft", "orange_stained_glass"),
                Identifier.of("minecraft", "magenta_stained_glass"),
                Identifier.of("minecraft", "light_blue_stained_glass"),
                Identifier.of("minecraft", "yellow_stained_glass"),
                Identifier.of("minecraft", "lime_stained_glass"),
                Identifier.of("minecraft", "pink_stained_glass"),
                Identifier.of("minecraft", "gray_stained_glass"),
                Identifier.of("minecraft", "light_gray_stained_glass"),
                Identifier.of("minecraft", "cyan_stained_glass"),
                Identifier.of("minecraft", "purple_stained_glass"),
                Identifier.of("minecraft", "blue_stained_glass"),
                Identifier.of("minecraft", "brown_stained_glass"),
                Identifier.of("minecraft", "green_stained_glass"),
                Identifier.of("minecraft", "red_stained_glass"),
                Identifier.of("minecraft", "black_stained_glass"),
                Identifier.of("minecraft", "white_stained_glass_pane"),
                Identifier.of("minecraft", "orange_stained_glass_pane"),
                Identifier.of("minecraft", "magenta_stained_glass_pane"),
                Identifier.of("minecraft", "light_blue_stained_glass_pane"),
                Identifier.of("minecraft", "yellow_stained_glass_pane"),
                Identifier.of("minecraft", "lime_stained_glass_pane"),
                Identifier.of("minecraft", "pink_stained_glass_pane"),
                Identifier.of("minecraft", "gray_stained_glass_pane"),
                Identifier.of("minecraft", "light_gray_stained_glass_pane"),
                Identifier.of("minecraft", "cyan_stained_glass_pane"),
                Identifier.of("minecraft", "purple_stained_glass_pane"),
                Identifier.of("minecraft", "blue_stained_glass_pane"),
                Identifier.of("minecraft", "brown_stained_glass_pane"),
                Identifier.of("minecraft", "green_stained_glass_pane"),
                Identifier.of("minecraft", "red_stained_glass_pane"),
                Identifier.of("minecraft", "black_stained_glass_pane"),
                // glowstone
                Identifier.of("minecraft", "glowstone"),
                // stonecutter
                Identifier.of("minecraft", "stonecutter")
        );

        // Tier 3: dispensers, droppers, observers, pistons
        registerWithTier(rule, 3,
                Identifier.of("minecraft", "dispenser"),
                Identifier.of("minecraft", "dropper"),
                Identifier.of("minecraft", "observer"),
                Identifier.of("minecraft", "piston"),
                Identifier.of("minecraft", "sticky_piston")
        );

        // extra masonry items from config
        for (String idStr : KnowledgeBoundConfig.INSTANCE.extraMasonryItems) {
            try {
                Identifier id = Identifier.of(idStr);
                RULES_BY_ITEM.put(id, rule);
                ITEM_TIERS.putIfAbsent(id, 0);
            } catch (Exception e) {
                KnowledgeBound.LOGGER.warn("[KnowledgeBound] Invalid extraMasonryItems id: {}", idStr);
            }
        }
    }

    // --------------------------------------------------
    //  Blocked items (cannot be crafted at all)
    // --------------------------------------------------

    private static void registerBlockedItems() {
        // all boats are blocked
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "oak_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "spruce_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "birch_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "jungle_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "acacia_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "dark_oak_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "mangrove_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "cherry_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "bamboo_raft"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "oak_chest_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "spruce_chest_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "birch_chest_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "jungle_chest_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "acacia_chest_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "dark_oak_chest_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "mangrove_chest_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "cherry_chest_boat"));
        BLOCKED_ITEMS.add(Identifier.of("minecraft", "bamboo_chest_raft"));

        // extra blocked items from config
        for (String idStr : KnowledgeBoundConfig.INSTANCE.blockedCraftingItems) {
            try {
                Identifier id = Identifier.of(idStr);
                BLOCKED_ITEMS.add(id);
            } catch (Exception e) {
                KnowledgeBound.LOGGER.warn("[KnowledgeBound] Invalid blockedCraftingItems id: {}", idStr);
            }
        }
    }
}
