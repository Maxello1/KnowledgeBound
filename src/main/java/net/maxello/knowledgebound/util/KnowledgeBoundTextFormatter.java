package net.maxello.knowledgebound.util;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

// This class is our central hub for spitting out chat messages to the player.
// It takes the raw text templates from the config file, swaps out the placeholders 
// (like {knowledge} or {tier}), and turns them into proper Minecraft Text components.
public final class KnowledgeBoundTextFormatter {

    private KnowledgeBoundTextFormatter() {
    }

    // Turn "toolsmithing" → "Toolsmithing", "weapon_smith" → "Weapon Smith"
    private static String displayName(Identifier knowledgeId) {
        String path = knowledgeId.getPath().replace('_', ' ');
        String[] parts = path.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1));
            }
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
    }

    // --------------------------------------------------
    //  XP / Level messages
    // --------------------------------------------------

    /** "You’re learning <Knowledge>!" line. */
    public static Text learningTick(Identifier knowledgeId) {
        String name = displayName(knowledgeId);
        String template = KnowledgeBoundConfig.INSTANCE.messages.learning;
        String formatted = template.replace("{knowledge}", name);
        return Text.literal(formatted);
    }

    /** Level-up line. */
    public static Text levelUp(Identifier knowledgeId, int tier) {
        String name = displayName(knowledgeId);
        String template = KnowledgeBoundConfig.INSTANCE.messages.levelUp;
        String formatted = template.replace("{knowledge}", name)
                .replace("{tier}", String.valueOf(tier));
        return Text.literal(formatted);
    }

    // --------------------------------------------------
    //  Crafting result messages
    // --------------------------------------------------

    /** Failure message for any crafting knowledge */
    public static Text craftingFail(Identifier knowledgeId) {
        String name = displayName(knowledgeId);
        String template = KnowledgeBoundConfig.INSTANCE.messages.craftingFail;
        String formatted = template.replace("{knowledge}", name);
        return Text.literal(formatted);
    }

    /** Message: player doesn't have a high enough tier for material job crafting */
    public static Text craftingLevelTooLow(Identifier knowledgeId) {
        String name = displayName(knowledgeId);
        String template = KnowledgeBoundConfig.INSTANCE.messages.craftingLevelTooLow;
        String formatted = template.replace("{knowledge}", name);
        return Text.literal(formatted);
    }

    /** Colored text for crafting quality results. */
    public static Text craftingQuality(Identifier knowledgeId, String quality) {
        String name = displayName(knowledgeId);
        String template;
        if ("poor".equalsIgnoreCase(quality)) {
            template = KnowledgeBoundConfig.INSTANCE.messages.craftingQualityPoor;
        } else {
            template = KnowledgeBoundConfig.INSTANCE.messages.craftingQualityNormal;
        }
        String formatted = template.replace("{knowledge}", name);
        return Text.literal(formatted);
    }

    /** Message for gather failures: Forestry, Mining, Digging, Farming. */
    public static Text gatheringFail(Identifier knowledgeId) {
        String name = displayName(knowledgeId);
        String template = KnowledgeBoundConfig.INSTANCE.messages.gatheringFail;
        String formatted = template.replace("{knowledge}", name);
        return Text.literal(formatted);
    }

    /** Message when proficiency cap blocks leveling up */
    public static Text proficiencyLimitReached(Identifier knowledgeId) {
        String name = displayName(knowledgeId);
        String template = KnowledgeBoundConfig.INSTANCE.messages.proficiencyLimitReached;
        String formatted = template.replace("{knowledge}", name);
        return Text.literal(formatted);
    }

    /**
     * Format a simple message string (no knowledge placeholder needed).
     * Used for combat and workstation messages.
     */
    public static Text formatSimple(String template) {
        return Text.literal(template);
    }

}



