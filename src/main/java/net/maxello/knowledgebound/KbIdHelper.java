package net.maxello.knowledgebound;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class KbIdHelper {
    public static String getKbId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null && customData.contains("kb_id")) {
            return customData.getNbt().getString("kb_id");
        }
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return id != null ? id.toString() : "";
    }
}
