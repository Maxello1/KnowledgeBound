package net.maxello.knowledgebound;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class KnowledgeTags {

    private KnowledgeTags() {}

    public static final TagKey<Item> WOODEN_TOOLS   = item("wooden_tools");
    public static final TagKey<Item> STONE_TOOLS    = item("stone_tools");
    public static final TagKey<Item> COPPER_TOOLS   = item("copper_tools");
    public static final TagKey<Item> IRON_TOOLS     = item("iron_tools");
    public static final TagKey<Item> DIAMOND_TOOLS  = item("diamond_tools");

    public static final TagKey<Item> LEATHER_ARMOR  = item("leather_armor");
    public static final TagKey<Item> CHAINMAIL_ARMOR = item("chainmail_armor");

    public static final TagKey<Item> BOWS        = item("bows");
    public static final TagKey<Item> CROSSBOWS   = item("crossbows");
    public static final TagKey<Item> FISHING_RODS = item("fishing_rods");

    public static final TagKey<Item> MELEE_WEAPONS = item("melee_weapons");

    private static TagKey<Item> item(String path) {
        return TagKey.of(RegistryKeys.ITEM, Identifier.of(KnowledgeBound.MOD_ID, path));
    }
}
