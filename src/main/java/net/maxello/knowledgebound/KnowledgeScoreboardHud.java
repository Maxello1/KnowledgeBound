package net.maxello.knowledgebound;

import net.minecraft.scoreboard.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Server-side scoreboard HUD that shows knowledge tiers and progress.
 * Toggleable per-player via /kb hud. When toggled off, the sidebar
 * returns to whatever it was before (or nothing).
 */
public final class KnowledgeScoreboardHud {

    private KnowledgeScoreboardHud() {}

    private static final String OBJECTIVE_NAME = "kb_hud";

    // tracks which players have the HUD enabled
    private static final Set<UUID> ENABLED_PLAYERS = new HashSet<>();

    // tracks what sidebar display slot the player had before we took over
    // null = they had no sidebar
    private static final Map<UUID, String> PREVIOUS_SIDEBAR = new HashMap<>();

    // display order for knowledge entries (top to bottom)
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

    public static boolean isEnabled(ServerPlayerEntity player) {
        return ENABLED_PLAYERS.contains(player.getUuid());
    }

    /**
     * Toggle the HUD for a player. Returns true if it's now ON.
     */
    public static boolean toggle(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        if (ENABLED_PLAYERS.contains(uuid)) {
            // turning off
            disable(player);
            return false;
        } else {
            // turning on
            enable(player);
            return true;
        }
    }

    private static void enable(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        ENABLED_PLAYERS.add(uuid);

        Scoreboard scoreboard = player.getScoreboard();

        // save current sidebar objective so we can restore it later
        ScoreboardObjective currentSidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (currentSidebar != null && !currentSidebar.getName().equals(OBJECTIVE_NAME)) {
            PREVIOUS_SIDEBAR.put(uuid, currentSidebar.getName());
        }

        // create or get our objective
        ScoreboardObjective obj = scoreboard.getNullableObjective(OBJECTIVE_NAME);
        if (obj == null) {
            obj = scoreboard.addObjective(
                    OBJECTIVE_NAME,
                    ScoreboardCriterion.DUMMY,
                    Text.literal("Knowledge").formatted(Formatting.GOLD, Formatting.BOLD),
                    ScoreboardCriterion.RenderType.INTEGER,
                    true, // show below name
                    null
            );
        }

        // show it on sidebar
        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, obj);

        // populate
        updateScoreboard(player);
    }

    private static void disable(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        ENABLED_PLAYERS.remove(uuid);

        Scoreboard scoreboard = player.getScoreboard();

        // restore previous sidebar
        String prevName = PREVIOUS_SIDEBAR.remove(uuid);
        if (prevName != null) {
            ScoreboardObjective prevObj = scoreboard.getNullableObjective(prevName);
            if (prevObj != null) {
                scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, prevObj);
            } else {
                scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, null);
            }
        } else {
            scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, null);
        }

        // clean up our scores
        cleanupScores(scoreboard);
    }

    /**
     * Update the scoreboard display for a player. Call this after any XP change.
     */
    public static void updateScoreboard(ServerPlayerEntity player) {
        if (!ENABLED_PLAYERS.contains(player.getUuid())) return;

        Scoreboard scoreboard = player.getScoreboard();
        ScoreboardObjective obj = scoreboard.getNullableObjective(OBJECTIVE_NAME);
        if (obj == null) return;

        // clear old scores for this objective
        cleanupScores(scoreboard);

        // populate rows - higher score = higher on sidebar
        int score = ORDER.size();
        for (String fullId : ORDER) {
            Identifier id = Identifier.of(fullId);
            KnowledgeDefinition def = KnowledgeRegistry.get(id);
            if (def == null) {
                score--;
                continue;
            }

            PlayerKnowledgeManager.PlayerKnowledgeState state =
                    PlayerKnowledgeManager.getState(player, id);

            int tier = state.tier;
            int maxTier = def.getMaxTier();
            boolean maxed = tier >= maxTier;

            String name = formatName(fullId);

            // build display text with color code prefix
            String colorCode;
            if (maxed) {
                colorCode = "§a"; // green
            } else if (tier == 0) {
                colorCode = "§7"; // gray
            } else {
                colorCode = "§e"; // yellow
            }

            String line;
            if (maxed) {
                line = colorCode + name + " T" + tier + "/" + maxTier + " §6MAX";
            } else {
                int needed = def.getMinutesForTier(tier + 1);
                line = colorCode + name + " T" + tier + "/" + maxTier + " §f" + state.currentMinutes + "/" + needed + "m";
            }

            // scoreboard entries use the display line as the score holder name
            ScoreHolder holder = ScoreHolder.fromName(line);
            ScoreAccess access = scoreboard.getOrCreateScore(holder, obj);
            access.setScore(score);

            score--;
        }
    }

    /**
     * Called when a player disconnects - clean up their state.
     */
    public static void onPlayerLeave(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        ENABLED_PLAYERS.remove(uuid);
        PREVIOUS_SIDEBAR.remove(uuid);
    }

    private static void cleanupScores(Scoreboard scoreboard) {
        ScoreboardObjective obj = scoreboard.getNullableObjective(OBJECTIVE_NAME);
        if (obj == null) return;
        // remove all scores from this objective
        scoreboard.removeObjective(obj);
        // recreate it fresh
        scoreboard.addObjective(
                OBJECTIVE_NAME,
                ScoreboardCriterion.DUMMY,
                Text.literal("Knowledge").formatted(Formatting.GOLD, Formatting.BOLD),
                ScoreboardCriterion.RenderType.INTEGER,
                true,
                null
        );
        ScoreboardObjective newObj = scoreboard.getNullableObjective(OBJECTIVE_NAME);
        if (newObj != null) {
            scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, newObj);
        }
    }

    private static String formatName(String fullId) {
        int colon = fullId.indexOf(':');
        String path = colon >= 0 ? fullId.substring(colon + 1) : fullId;
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
        }
        return sb.toString();
    }
}
