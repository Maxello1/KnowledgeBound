package net.maxello.knowledgebound.core;

import net.maxello.knowledgebound.KnowledgeBound;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * A central place for all of our custom item tags.
 * 
 * We use these tags constantly to categorize items. Instead of hardcoding 
 * "minecraft:wooden_pickaxe", "minecraft:wooden_axe", etc. into our logic, 
 * we just check if an item is in the WOODEN_TOOLS tag. It makes supporting
 * modded tools and weapons infinitely easier since players or modpack makers
 * can just add the new items to the appropriate JSON tag files.
 */
public final class KnowledgeTags {

    // Prevent instantiation, this is just a constants class
    private KnowledgeTags() {}

    // Tool materials. Used heavily for checking if you have the right tool
    // tier to gain experience, or if you meet the requirements to craft.
    public static final TagKey<Item> WOODEN_TOOLS   = item("wooden_tools");
    public static final TagKey<Item> STONE_TOOLS    = item("stone_tools");
    public static final TagKey<Item> COPPER_TOOLS   = item("copper_tools");
    public static final TagKey<Item> IRON_TOOLS     = item("iron_tools");
    public static final TagKey<Item> DIAMOND_TOOLS  = item("diamond_tools");

    // Armor tags. Mostly used for armouring recipes and restriction checks.
    public static final TagKey<Item> LEATHER_ARMOR  = item("leather_armor");
    public static final TagKey<Item> CHAINMAIL_ARMOR = item("chainmail_armor");

    // Ranged weapons and fishing rods.
    public static final TagKey<Item> BOWS        = item("bows");
    public static final TagKey<Item> CROSSBOWS   = item("crossbows");
    public static final TagKey<Item> FISHING_RODS = item("fishing_rods");

    // General bucket for melee weapons, usually encompassing swords and axes.
    public static final TagKey<Item> MELEE_WEAPONS = item("melee_weapons");

    /**
     * Quick little helper method to build the TagKey without repeating 
     * the registry lookup and Identifier boilerplate every single time.
     */
    private static TagKey<Item> item(String path) {
        return TagKey.of(RegistryKeys.ITEM, Identifier.of(KnowledgeBound.MOD_ID, path));
    }
}


