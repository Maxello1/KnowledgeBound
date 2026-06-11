package net.maxello.knowledgebound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * Renders the knowledge status overlay (toggled with the knowledge HUD key).
 * Client-only — never reference this class from server-side code.
 */
public final class KnowledgeHudRenderer {

    private KnowledgeHudRenderer() {}

    // Ordered display list — mirrors the registration order in KnowledgeRegistry
    private static final List<String> ORDER = List.of(
            "knowledgebound:forestry",
            "knowledgebound:mining",
            "knowledgebound:digging",
            "knowledgebound:farming",
            "knowledgebound:toolsmithing",
            "knowledgebound:weaponsmithing",
            "knowledgebound:armouring",
            "knowledgebound:melee_combat",
            "knowledgebound:ranged_combat",
            "knowledgebound:fishing",
            "knowledgebound:carpentry",
            "knowledgebound:masonry",
            "knowledgebound:beekeeping"
    );

    private static final int PANEL_X       = 6;
    private static final int PANEL_Y       = 6;
    private static final int PANEL_W       = 230;
    private static final int ROW_H         = 11;
    private static final int PADDING       = 4;
    private static final int BAR_W         = 80;
    private static final int BAR_H         = 5;

    private static final int COL_BG        = 0xB2000000;
    private static final int COL_TITLE     = 0xFFFFD700;
    private static final int COL_NAME      = 0xFFFFFFFF;
    private static final int COL_TIER      = 0xFFAAAAAA;
    private static final int COL_BAR_FILL  = 0xFF22BB55;
    private static final int COL_BAR_EMPTY = 0xFF444444;
    private static final int COL_BAR_MAX   = 0xFF4488FF;

    public static void render(DrawContext context) {
        if (!ClientKnowledgeState.hudVisible) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        int rowCount = ORDER.size();
        int titleH = ROW_H + 2;
        int panelH = PADDING + titleH + rowCount * ROW_H + PADDING;

        int px = PANEL_X;
        int py = PANEL_Y;

        // panel background
        context.fill(px, py, px + PANEL_W, py + panelH, COL_BG);

        // title
        int ty = py + PADDING;
        context.drawText(tr, "Knowledge Status", px + PADDING, ty, COL_TITLE, true);
        ty += titleH;

        // separator line
        context.fill(px + PADDING, ty - 2, px + PANEL_W - PADDING, ty - 1, 0x66FFFFFF);

        // rows
        for (String id : ORDER) {
            renderRow(context, tr, px, ty, id);
            ty += ROW_H;
        }
    }

    private static void renderRow(DrawContext context, TextRenderer tr, int px, int y, String id) {
        int tier     = ClientKnowledgeState.getTier(id);
        int current  = ClientKnowledgeState.getCurrentMinutes(id);
        int needed   = ClientKnowledgeState.getNeededMinutes(id);
        int maxTier  = ClientKnowledgeState.getMaxTier(id);
        boolean maxed = ClientKnowledgeState.isMaxTier(id);

        // knowledge display name from the id path
        String name = formatName(id);
        // truncate to fit
        String nameDisplay = tr.getWidth(name) > 90 ? tr.trimToWidth(name, 87) + ".." : name;

        int textY = y + (ROW_H - 8) / 2; // vertically center 8px font in ROW_H

        // name
        context.drawText(tr, nameDisplay, px + PADDING, textY, COL_NAME, false);

        // tier label ("T3/5")
        String tierLabel = "T" + tier + "/" + maxTier;
        int tierX = px + PADDING + 96;
        context.drawText(tr, tierLabel, tierX, textY, COL_TIER, false);

        // progress bar
        int barX = px + PADDING + 130;
        int barY = y + (ROW_H - BAR_H) / 2;
        int barColor = maxed ? COL_BAR_MAX : COL_BAR_FILL;

        // background
        context.fill(barX, barY, barX + BAR_W, barY + BAR_H, COL_BAR_EMPTY);

        // fill
        float fraction = maxed ? 1.0f
                : (needed > 0 ? Math.min(1.0f, (float) current / needed) : 0.0f);
        int fillW = (int) (BAR_W * fraction);
        if (fillW > 0) {
            context.fill(barX, barY, barX + fillW, barY + BAR_H, barColor);
        }

        // minutes label to the right of bar
        String minLabel = maxed ? "MAX" : current + "/" + needed + "m";
        int labelX = barX + BAR_W + 3;
        if (labelX + tr.getWidth(minLabel) <= px + PANEL_W - 1) {
            context.drawText(tr, minLabel, labelX, textY, COL_TIER, false);
        }
    }

    private static String formatName(String fullId) {
        // "knowledgebound:melee_combat" -> "Melee Combat"
        int colon = fullId.indexOf(':');
        String path = colon >= 0 ? fullId.substring(colon + 1) : fullId;
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1));
        }
        return sb.toString();
    }
}
