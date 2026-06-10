package net.maxello.knowledgebound;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

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

    /** Green "You’re learning <Knowledge>!" line. */
    public static Text learningTick(Identifier knowledgeId) {
        String name = displayName(knowledgeId);
        return Text.literal("You're learning " + name + "!")
                .formatted(Formatting.GREEN);
    }

    /** Gold level-up line. */
    public static Text levelUp(Identifier knowledgeId, int tier) {
        String name = displayName(knowledgeId);
        return Text.literal("Your " + name + " knowledge increased to Tier " + tier + "!")
                .formatted(Formatting.GOLD);
    }

    // --------------------------------------------------
    //  Crafting result messages
    // --------------------------------------------------

    /** Red failure message for any crafting knowledge */
    public static Text craftingFail(Identifier knowledgeId) {
        String name = displayName(knowledgeId);
        return Text.literal("Your " + name + " attempt failed to yield any items.")
                .formatted(Formatting.RED);
    }

    /** Red message: player doesn't have a high enough tier for material job crafting */
    public static Text craftingLevelTooLow(Identifier knowledgeId) {
        String name = displayName(knowledgeId);
        return Text.literal("You don't have enough " + name + " knowledge to work with these materials.")
                .formatted(Formatting.RED);
    }

    /** Cyan text with purple "<quality>" word for crafting results. */
    public static Text craftingQuality(Identifier knowledgeId, String quality) {
        String name = displayName(knowledgeId);
        MutableText base = Text.literal("You crafted a ")
                .formatted(Formatting.AQUA);

        MutableText qualityWord = Text.literal(quality + " ")
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD);

        MutableText tail = Text.literal("quality item. Improve your " + name + " knowledge for better quality.")
                .formatted(Formatting.AQUA);

        return base.append(qualityWord).append(tail);
    }

    /** Red message for gather failures: Forestry, Mining, Digging, Farming. */
    public static Text gatheringFail(Identifier knowledgeId) {
        String name = displayName(knowledgeId);
        return Text.literal("Your " + name + " attempt failed to yield any resources.")
                .formatted(Formatting.RED);
    }

    /** Dark red message when proficiency cap blocks leveling up */
    public static Text proficiencyLimitReached(Identifier knowledgeId) {
        String name = displayName(knowledgeId);
        return Text.literal("You've reached your proficiency limit for " + name + ". You cannot advance further.")
                .formatted(Formatting.DARK_RED);
    }

}
