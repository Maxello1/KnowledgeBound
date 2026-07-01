package net.maxello.knowledgebound.config;

import net.maxello.knowledgebound.KnowledgeBound;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin config GUI system — editable config menus using vanilla chest GUIs.
 * All server-side, no client mod required.
 */
public final class ConfigGuiHandler {

    /** Title prefix used to detect admin GUI screens in ScreenHandlerMixin. */
    public static final String MAIN_MENU_TITLE = "§6§lKB Admin Config";
    /** Title prefix for category submenus. Format: "KB Config: <CategoryName>" */
    public static final String CATEGORY_TITLE_PREFIX = "§6§lKB Config: ";

    /** Marker text placed in slot 4 of the main menu for detection. */
    public static final String MAIN_MENU_MARKER = "§6§lKnowledgeBound Config";

    /** Tracks which page a player is on for paginated categories. */
    private static final java.util.concurrent.ConcurrentHashMap<java.util.UUID, Integer> PLAYER_PAGES =
            new java.util.concurrent.ConcurrentHashMap<>();

    private ConfigGuiHandler() {}

    // ──────────────────────────────────────────────
    // Main Menu
    // ──────────────────────────────────────────────

    public static void openMainMenu(ServerPlayerEntity player) {
        SimpleInventory inv = new SimpleInventory(54);

        // Row 0: Title bar
        fillGrayGlass(inv, 0, 8);
        setNamedItem(inv, 4, Items.NETHER_STAR, MAIN_MENU_MARKER);

        // Row 1-3: Category buttons
        // Layout: slots in rows 1, 2, 3
        int[] categorySlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        Map<String, ConfigGuiCategory> categories = ConfigGuiCategory.all();
        int catIndex = 0;
        for (ConfigGuiCategory cat : categories.values()) {
            if (catIndex >= categorySlots.length) break;
            int slot = categorySlots[catIndex];
            setNamedItem(inv, slot, cat.getIcon(),
                    "§e§l" + cat.getDisplayName(),
                    "§7Click to edit " + cat.getDisplayName(),
                    "§8Category: " + cat.getId());
            catIndex++;
        }
        // Fill remaining category area (start at 9 to catch gaps between rows)
        for (int i = 9; i < 36; i++) {
            if (!inv.getStack(i).isEmpty()) continue;
            setGrayGlass(inv, i);
        }

        // Row 4: filler
        fillGrayGlass(inv, 36, 44);

        // Row 5: action bar
        fillGrayGlass(inv, 45, 53);
        setNamedItem(inv, 45, Items.BARRIER, "§c§lClose");
        setNamedItem(inv, 49, Items.EMERALD, "§a§lSave & Reload",
                "§7Saves config to disk and reloads");

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new GenericContainerScreenHandler(
                        ScreenHandlerType.GENERIC_9X6, syncId, playerInv, inv, 6),
                Text.literal(MAIN_MENU_TITLE)
        ));

        PLAYER_PAGES.remove(player.getUuid());
    }

    // ──────────────────────────────────────────────
    // Category Submenu
    // ──────────────────────────────────────────────

    public static void openCategory(ServerPlayerEntity player, String categoryId) {
        openCategory(player, categoryId, 0);
    }

    public static void openCategory(ServerPlayerEntity player, String categoryId, int page) {
        ConfigGuiCategory cat = ConfigGuiCategory.get(categoryId);
        if (cat == null) return;

        List<ConfigGuiEntry> entries = cat.getEntries();
        int entriesPerPage = 27; // rows 1-3 (slots 9-35)
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / entriesPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        SimpleInventory inv = new SimpleInventory(54);

        // Row 0: Header
        setNamedItem(inv, 0, Items.ARROW, "§6§l⬅ Back",
                "§7Return to main menu");
        fillGrayGlass(inv, 1, 7);
        setNamedItem(inv, 8, Items.NETHER_STAR, "§b§l" + cat.getDisplayName());

        // Rows 1-3: Config entries
        int startIdx = page * entriesPerPage;
        int endIdx = Math.min(startIdx + entriesPerPage, entries.size());
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;

        for (int i = startIdx; i < endIdx; i++) {
            int slot = 9 + (i - startIdx);
            ConfigGuiEntry entry = entries.get(i);
            placeConfigEntry(inv, slot, entry, cfg);
        }
        // Fill remaining entry slots
        for (int i = 9 + (endIdx - startIdx); i < 36; i++) {
            setGrayGlass(inv, i);
        }

        // Row 4: filler
        fillGrayGlass(inv, 36, 44);

        // Row 5: Action bar
        fillGrayGlass(inv, 45, 53);
        setNamedItem(inv, 45, Items.ARROW, "§6§l⬅ Back",
                "§7Return to main menu");
        setNamedItem(inv, 46, Items.BARRIER, "§c§lClose");

        if (totalPages > 1) {
            if (page > 0) {
                setNamedItem(inv, 48, Items.SPECTRAL_ARROW, "§d§l◀ Prev Page",
                        "§7Page " + page + " / " + totalPages);
            }
            if (page < totalPages - 1) {
                setNamedItem(inv, 50, Items.SPECTRAL_ARROW, "§d§lNext Page ▶",
                        "§7Page " + (page + 2) + " / " + totalPages);
            }
        }
        setNamedItem(inv, 49, Items.EMERALD, "§a§lSave & Reload",
                "§7Saves config to disk and reloads");

        String title = CATEGORY_TITLE_PREFIX + cat.getDisplayName();
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> new GenericContainerScreenHandler(
                        ScreenHandlerType.GENERIC_9X6, syncId, playerInv, inv, 6),
                Text.literal(title)
        ));

        PLAYER_PAGES.put(player.getUuid(), page);
    }

    // ──────────────────────────────────────────────
    // Click Handling
    // ──────────────────────────────────────────────

    /**
     * Handle a click in a config GUI screen. Called from ScreenHandlerMixin.
     * @return true if the click was handled (cancel vanilla behavior)
     */
    public static boolean handleClick(ServerPlayerEntity player, String screenTitle,
                                       int slotIndex, int button, SlotActionType actionType,
                                       net.minecraft.screen.ScreenHandler handler) {
        if (screenTitle.equals(MAIN_MENU_TITLE)) {
            return handleMainMenuClick(player, slotIndex, handler);
        }
        if (screenTitle.startsWith(CATEGORY_TITLE_PREFIX)) {
            String catDisplayName = screenTitle.substring(CATEGORY_TITLE_PREFIX.length());
            return handleCategoryClick(player, catDisplayName, slotIndex, button, actionType, handler);
        }
        return false;
    }

    private static boolean handleMainMenuClick(ServerPlayerEntity player, int slotIndex,
                                                net.minecraft.screen.ScreenHandler handler) {
        // Close button
        if (slotIndex == 45) {
            player.closeHandledScreen();
            return true;
        }

        // Save & Reload
        if (slotIndex == 49) {
            KnowledgeBoundConfig.INSTANCE.save();
            KnowledgeBoundConfig.load();
            player.sendMessage(Text.literal("§a[KB] Config saved and reloaded!"), false);
            // Refresh the menu
            player.getServer().execute(() -> openMainMenu(player));
            return true;
        }

        // Category buttons (slots 9-35)
        if (slotIndex >= 9 && slotIndex < 36) {
            ItemStack clicked = handler.slots.get(slotIndex).getStack();
            if (clicked.isEmpty()) return true;
            // Find category by matching the slot item's custom name
            var name = clicked.get(DataComponentTypes.CUSTOM_NAME);
            if (name == null) return true;
            String nameStr = name.getString();
            // Extract display name (strip formatting prefix "§e§l")
            for (ConfigGuiCategory cat : ConfigGuiCategory.all().values()) {
                if (nameStr.contains(cat.getDisplayName())) {
                    player.getServer().execute(() -> openCategory(player, cat.getId()));
                    return true;
                }
            }
        }
        return true; // block all other clicks
    }

    private static boolean handleCategoryClick(ServerPlayerEntity player, String catDisplayName,
                                                int slotIndex, int button, SlotActionType actionType,
                                                net.minecraft.screen.ScreenHandler handler) {
        // Find the category
        ConfigGuiCategory cat = null;
        for (ConfigGuiCategory c : ConfigGuiCategory.all().values()) {
            if (c.getDisplayName().equals(catDisplayName)) {
                cat = c;
                break;
            }
        }
        if (cat == null) return true;

        // Back buttons (slot 0 or 45)
        if (slotIndex == 0 || slotIndex == 45) {
            player.getServer().execute(() -> openMainMenu(player));
            return true;
        }

        // Close button
        if (slotIndex == 46) {
            player.closeHandledScreen();
            return true;
        }

        // Save & Reload
        if (slotIndex == 49) {
            KnowledgeBoundConfig.INSTANCE.save();
            KnowledgeBoundConfig.load();
            player.sendMessage(Text.literal("§a[KB] Config saved and reloaded!"), false);
            final ConfigGuiCategory finalCat = cat;
            int page = PLAYER_PAGES.getOrDefault(player.getUuid(), 0);
            player.getServer().execute(() -> openCategory(player, finalCat.getId(), page));
            return true;
        }

        // Pagination
        int page = PLAYER_PAGES.getOrDefault(player.getUuid(), 0);
        if (slotIndex == 48) { // Prev page
            final ConfigGuiCategory finalCat = cat;
            int prevPage = Math.max(0, page - 1);
            player.getServer().execute(() -> openCategory(player, finalCat.getId(), prevPage));
            return true;
        }
        if (slotIndex == 50) { // Next page
            final ConfigGuiCategory finalCat = cat;
            int nextPage = page + 1;
            player.getServer().execute(() -> openCategory(player, finalCat.getId(), nextPage));
            return true;
        }

        // Config entry clicks (slots 9-35)
        if (slotIndex >= 9 && slotIndex < 36) {
            int entriesPerPage = 27;
            int entryIndex = (page * entriesPerPage) + (slotIndex - 9);
            List<ConfigGuiEntry> entries = cat.getEntries();
            if (entryIndex >= entries.size()) return true;

            ConfigGuiEntry entry = entries.get(entryIndex);
            handleEntryClick(player, entry, button, actionType);

            // Refresh the page
            final ConfigGuiCategory finalCat = cat;
            final int currentPage = page;
            player.getServer().execute(() -> openCategory(player, finalCat.getId(), currentPage));
            return true;
        }

        return true; // block all other clicks
    }

    private static void handleEntryClick(ServerPlayerEntity player, ConfigGuiEntry entry,
                                          int button, SlotActionType actionType) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        String currentStr = cfg.getFieldValue(entry.getConfigPath());
        if (currentStr == null) return;

        switch (entry.getType()) {
            case BOOLEAN -> {
                boolean current = "true".equals(currentStr);
                cfg.setFieldValue(entry.getConfigPath(), String.valueOf(!current));
                player.sendMessage(Text.literal("§e" + entry.getDisplayName() + " §7→ §f" + (!current ? "§aON" : "§cOFF")), true);
            }
            case INTEGER -> {
                try {
                    int current = Integer.parseInt(currentStr);
                    boolean isShift = actionType == SlotActionType.QUICK_MOVE;
                    int step = (int) (isShift ? entry.getLargeStep() : entry.getSmallStep());
                    // Left click (button 0) = increase, Right click (button 1) = decrease
                    int delta = (button == 1) ? -step : step;
                    int newVal = (int) Math.max(entry.getMin(), Math.min(entry.getMax(), current + delta));
                    cfg.setFieldValue(entry.getConfigPath(), String.valueOf(newVal));
                    player.sendMessage(Text.literal("§e" + entry.getDisplayName() + " §7→ §f" + newVal), true);
                } catch (NumberFormatException ignored) {}
            }
            case DOUBLE -> {
                try {
                    double current = Double.parseDouble(currentStr);
                    boolean isShift = actionType == SlotActionType.QUICK_MOVE;
                    double step = isShift ? entry.getLargeStep() : entry.getSmallStep();
                    double delta = (button == 1) ? -step : step;
                    double newVal = Math.max(entry.getMin(), Math.min(entry.getMax(), current + delta));
                    // Round to avoid floating point noise
                    newVal = Math.round(newVal * 1000.0) / 1000.0;
                    cfg.setFieldValue(entry.getConfigPath(), String.valueOf(newVal));
                    player.sendMessage(Text.literal("§e" + entry.getDisplayName() + " §7→ §f" + newVal), true);
                } catch (NumberFormatException ignored) {}
            }
            case STRING -> {
                // String editing not supported in chest GUI (use /kb config set)
                player.sendMessage(Text.literal("§cUse §e/kb config set " + entry.getConfigPath() + " <value>§c to edit text values."), false);
            }
        }
    }

    // ──────────────────────────────────────────────
    // Config Entry Display
    // ──────────────────────────────────────────────

    private static void placeConfigEntry(SimpleInventory inv, int slot,
                                          ConfigGuiEntry entry, KnowledgeBoundConfig cfg) {
        String valueStr = cfg.getFieldValue(entry.getConfigPath());
        if (valueStr == null) valueStr = "???";

        List<String> lore = new ArrayList<>();
        lore.add("§7" + entry.getDescription());
        lore.add("");

        switch (entry.getType()) {
            case BOOLEAN -> {
                boolean val = "true".equals(valueStr);
                Item icon = val ? Items.LIME_DYE : Items.GRAY_DYE;
                String status = val ? "§a§lON" : "§c§lOFF";
                lore.add("§7Current: " + status);
                lore.add("");
                lore.add("§eClick §7to toggle");
                setNamedItem(inv, slot, icon, "§f" + entry.getDisplayName(),
                        lore.toArray(new String[0]));
            }
            case INTEGER -> {
                int step = (int) entry.getSmallStep();
                int bigStep = (int) entry.getLargeStep();
                lore.add("§7Current: §f" + valueStr);
                lore.add("");
                lore.add("§eL-Click: §a+" + step + "  §eR-Click: §c-" + step);
                lore.add("§eShift+L: §a+" + bigStep + "  §eShift+R: §c-" + bigStep);
                if (entry.getMin() > Integer.MIN_VALUE || entry.getMax() < Integer.MAX_VALUE) {
                    lore.add("§8Range: " + (int) entry.getMin() + " to " + (int) entry.getMax());
                }
                setNamedItem(inv, slot, Items.PAPER, "§f" + entry.getDisplayName(),
                        lore.toArray(new String[0]));
            }
            case DOUBLE -> {
                lore.add("§7Current: §f" + valueStr);
                lore.add("");
                lore.add("§eL-Click: §a+" + entry.getSmallStep() + "  §eR-Click: §c-" + entry.getSmallStep());
                lore.add("§eShift+L: §a+" + entry.getLargeStep() + "  §eShift+R: §c-" + entry.getLargeStep());
                if (entry.getMin() > -Double.MAX_VALUE || entry.getMax() < Double.MAX_VALUE) {
                    lore.add("§8Range: " + entry.getMin() + " to " + entry.getMax());
                }
                setNamedItem(inv, slot, Items.PAPER, "§f" + entry.getDisplayName(),
                        lore.toArray(new String[0]));
            }
            case STRING -> {
                lore.add("§7Current: §f" + valueStr);
                lore.add("");
                lore.add("§cUse /kb config set to edit");
                setNamedItem(inv, slot, Items.WRITABLE_BOOK, "§f" + entry.getDisplayName(),
                        lore.toArray(new String[0]));
            }
        }
    }

    // ──────────────────────────────────────────────
    // Utility methods
    // ──────────────────────────────────────────────

    private static void setNamedItem(SimpleInventory inv, int slot, Item item, String name, String... loreLines) {
        ItemStack stack = item.getDefaultStack();
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        if (loreLines.length > 0) {
            List<Text> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(Text.literal(line));
            }
            stack.set(DataComponentTypes.LORE,
                    new net.minecraft.component.type.LoreComponent(lore));
        }
        inv.setStack(slot, stack);
    }

    private static void setGrayGlass(SimpleInventory inv, int slot) {
        ItemStack pane = Items.GRAY_STAINED_GLASS_PANE.getDefaultStack();
        pane.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
        inv.setStack(slot, pane);
    }

    private static void fillGrayGlass(SimpleInventory inv, int from, int to) {
        for (int i = from; i <= to; i++) {
            setGrayGlass(inv, i);
        }
    }
}


