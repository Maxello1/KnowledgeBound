package net.maxello.knowledgebound;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a vanilla double-chest GUI that displays a player's knowledge progress.
 * Fully server-side — the vanilla client renders chest GUIs natively, no client mod needed.
 */
public final class KnowledgeGuiHandler {

    /** Title used to identify the knowledge GUI for read-only slot protection. */
    public static final String GUI_TITLE = "Knowledge Progress";

    private KnowledgeGuiHandler() {}

    /**
     * Opens the knowledge GUI for the given player.
     * Shows all 18 knowledges with tier, progress, and visual indicators.
     */
    public static void open(ServerPlayerEntity player) {
        SimpleInventory inv = new SimpleInventory(54); // 6 rows = double chest

        // Row 0 (slots 0-8): Gathering knowledges
        placeDecor(inv, 0, Items.LIME_STAINED_GLASS_PANE.getDefaultStack(), "§a§lGathering");
        placeKnowledge(inv, 1, player, KnowledgeRegistry.FORESTRY_ID, Items.IRON_AXE.getDefaultStack());
        placeKnowledge(inv, 2, player, KnowledgeRegistry.MINING_ID, Items.IRON_PICKAXE.getDefaultStack());
        placeKnowledge(inv, 3, player, KnowledgeRegistry.DIGGING_ID, Items.IRON_SHOVEL.getDefaultStack());
        placeKnowledge(inv, 4, player, KnowledgeRegistry.FARMING_ID, Items.IRON_HOE.getDefaultStack());
        fillGlass(inv, 5, 8, Formatting.GREEN);

        // Row 1 (slots 9-17): Material jobs
        placeDecor(inv, 9, Items.ORANGE_STAINED_GLASS_PANE.getDefaultStack(), "§6§lMaterial");
        placeKnowledge(inv, 10, player, KnowledgeRegistry.TOOLSMITHING_ID, Items.ANVIL.getDefaultStack());
        placeKnowledge(inv, 11, player, KnowledgeRegistry.WEAPONSMITHING_ID, Items.IRON_SWORD.getDefaultStack());
        placeKnowledge(inv, 12, player, KnowledgeRegistry.ARMOURING_ID, Items.IRON_CHESTPLATE.getDefaultStack());
        fillGlass(inv, 13, 17, Formatting.GOLD);

        // Row 2 (slots 18-26): Combat + Fishing
        placeDecor(inv, 18, Items.RED_STAINED_GLASS_PANE.getDefaultStack(), "§c§lCombat & Fishing");
        placeKnowledge(inv, 19, player, KnowledgeRegistry.MELEE_COMBAT_ID, Items.DIAMOND_SWORD.getDefaultStack());
        placeKnowledge(inv, 20, player, KnowledgeRegistry.RANGED_COMBAT_ID, Items.BOW.getDefaultStack());
        placeKnowledge(inv, 21, player, KnowledgeRegistry.FISHING_ID, Items.FISHING_ROD.getDefaultStack());
        fillGlass(inv, 22, 26, Formatting.RED);

        // Row 3 (slots 27-35): Class jobs
        placeDecor(inv, 27, Items.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultStack(), "§b§lClass Jobs");
        placeKnowledge(inv, 28, player, KnowledgeRegistry.CARPENTRY_ID, Items.CRAFTING_TABLE.getDefaultStack());
        placeKnowledge(inv, 29, player, KnowledgeRegistry.MASONRY_ID, Items.STONECUTTER.getDefaultStack());
        placeKnowledge(inv, 30, player, KnowledgeRegistry.BEEKEEPING_ID, Items.HONEYCOMB.getDefaultStack());
        fillGlass(inv, 31, 35, Formatting.AQUA);

        // Row 4 (slots 36-44): Specialized jobs
        placeDecor(inv, 36, Items.PURPLE_STAINED_GLASS_PANE.getDefaultStack(), "§5§lSpecialized Jobs");
        placeKnowledge(inv, 37, player, KnowledgeRegistry.SMELTING_ID, Items.FURNACE.getDefaultStack());
        placeKnowledge(inv, 38, player, KnowledgeRegistry.COOKING_ID, Items.CAMPFIRE.getDefaultStack());
        placeKnowledge(inv, 39, player, KnowledgeRegistry.HUSBANDRY_ID, Items.WHEAT.getDefaultStack());
        placeKnowledge(inv, 40, player, KnowledgeRegistry.JEWELLER_ID, Items.DIAMOND.getDefaultStack());
        placeKnowledge(inv, 41, player, KnowledgeRegistry.SLAUGHTERING_ID, Items.BEEF.getDefaultStack());
        fillGlass(inv, 42, 44, Formatting.DARK_PURPLE);

        // Row 5 (slots 45-53): Decorative filler
        for (int i = 45; i < 54; i++) {
            ItemStack pane = Items.GRAY_STAINED_GLASS_PANE.getDefaultStack();
            pane.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            inv.setStack(i, pane);
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new GenericContainerScreenHandler(
                        ScreenHandlerType.GENERIC_9X6, syncId, playerInv, inv, 6),
                Text.literal(GUI_TITLE)
        ));
    }

    /**
     * Place a knowledge item in the inventory at the given slot.
     * Includes custom name with tier, lore with progress, and enchant glint if maxed.
     */
    private static void placeKnowledge(SimpleInventory inv, int slot,
                                        ServerPlayerEntity player,
                                        Identifier knowledgeId,
                                        ItemStack icon) {
        KnowledgeDefinition def = KnowledgeRegistry.get(knowledgeId);
        if (def == null) return;

        PlayerKnowledgeManager.PlayerKnowledgeState state =
                PlayerKnowledgeManager.getState(player, knowledgeId);

        int tier = state.tier;
        int maxTier = def.getMaxTier();
        boolean isMaxed = tier >= maxTier;

        String name = formatName(knowledgeId.getPath());

        // set item count to tier (min 1)
        ItemStack display = icon.copy();
        display.setCount(Math.max(1, tier));

        // custom name with color based on status
        Formatting nameColor;
        if (isMaxed) {
            nameColor = Formatting.GREEN;
        } else if (tier == 0) {
            nameColor = Formatting.GRAY;
        } else {
            nameColor = Formatting.YELLOW;
        }

        display.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(name + " — Tier " + tier + "/" + maxTier)
                        .formatted(nameColor, Formatting.BOLD));

        // lore lines showing progress
        List<Text> lore = new ArrayList<>();
        if (isMaxed) {
            lore.add(Text.literal("★ MASTERED ★").formatted(Formatting.GREEN, Formatting.BOLD));
            lore.add(Text.literal(" "));
            lore.add(Text.literal("You have fully mastered this skill.").formatted(Formatting.GRAY));
        } else {
            int nextTier = tier + 1;
            int needed = def.getMinutesForTier(nextTier);
            int current = state.currentMinutes;

            lore.add(Text.literal("Progress to Tier " + nextTier + ":").formatted(Formatting.WHITE));
            lore.add(Text.literal(current + " / " + needed + " minutes").formatted(Formatting.AQUA));
            lore.add(Text.literal(buildProgressBar(current, needed)).formatted(Formatting.GREEN));

            if (tier == 0) {
                lore.add(Text.literal(" "));
                lore.add(Text.literal("Start practicing to unlock!").formatted(Formatting.GRAY, Formatting.ITALIC));
            }
        }

        display.set(DataComponentTypes.LORE,
                new net.minecraft.component.type.LoreComponent(lore));

        // enchant glint for maxed knowledges
        if (isMaxed) {
            display.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        inv.setStack(slot, display);
    }

    /** Place a decorative pane with a category label. */
    private static void placeDecor(SimpleInventory inv, int slot, ItemStack pane, String label) {
        ItemStack item = pane.copy();
        item.set(DataComponentTypes.CUSTOM_NAME, Text.literal(label));
        inv.setStack(slot, item);
    }

    /** Fill slots with colored glass panes (empty spacers). */
    private static void fillGlass(SimpleInventory inv, int from, int to, Formatting color) {
        for (int i = from; i <= to; i++) {
            ItemStack pane;
            if (color == Formatting.GREEN) {
                pane = Items.LIME_STAINED_GLASS_PANE.getDefaultStack();
            } else if (color == Formatting.GOLD) {
                pane = Items.ORANGE_STAINED_GLASS_PANE.getDefaultStack();
            } else if (color == Formatting.RED) {
                pane = Items.RED_STAINED_GLASS_PANE.getDefaultStack();
            } else if (color == Formatting.AQUA) {
                pane = Items.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultStack();
            } else if (color == Formatting.DARK_PURPLE) {
                pane = Items.PURPLE_STAINED_GLASS_PANE.getDefaultStack();
            } else {
                pane = Items.GRAY_STAINED_GLASS_PANE.getDefaultStack();
            }
            pane.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            inv.setStack(i, pane);
        }
    }

    /** Build a text progress bar like [████░░░░░░] */
    private static String buildProgressBar(int current, int needed) {
        int barLength = 20;
        int filled = (needed > 0) ? (int) ((double) current / needed * barLength) : barLength;
        filled = Math.min(filled, barLength);

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            sb.append(i < filled ? "█" : "░");
        }
        sb.append("]");
        return sb.toString();
    }

    /** Convert "melee_combat" → "Melee Combat". */
    private static String formatName(String input) {
        String[] words = input.replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) sb.append(' ');
            if (!words[i].isEmpty()) {
                sb.append(Character.toUpperCase(words[i].charAt(0)));
                sb.append(words[i].substring(1));
            }
        }
        return sb.toString();
    }
}
