package net.maxello.knowledgebound;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Collection;

public final class KnowledgeCommands {

    private KnowledgeCommands() {}

    /** Suggests all registered knowledge IDs (e.g. "forestry", "mining"). */
    private static final SuggestionProvider<ServerCommandSource> KNOWLEDGE_SUGGESTIONS =
            (ctx, builder) -> {
                for (KnowledgeDefinition def : KnowledgeRegistry.all()) {
                    builder.suggest(def.getId().getPath());
                }
                return builder.buildFuture();
            };

    /** Suggests all custom item IDs (e.g. "royal_honey"). */
    private static final SuggestionProvider<ServerCommandSource> CUSTOM_ITEM_SUGGESTIONS =
            (ctx, builder) -> {
                for (String id : CustomItemRegistry.allIds()) {
                    builder.suggest(id);
                }
                return builder.buildFuture();
            };

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // /kb — player self-check (no args)
            // /kb check <player> — admin check another player
            // /kb set <player> <knowledge> <tier> — set a player's tier
            // /kb reset <player> [knowledge] — reset one or all knowledges
            // /kb reload — reload config
            // /kb hud — toggle sidebar knowledge display
            // /kb help — show command list

            dispatcher.register(
                    CommandManager.literal("kb")
                            .requires(src -> src.hasPermissionLevel(0))
                            .executes(KnowledgeCommands::executeSelfCheck)

                            // /kb help
                            .then(CommandManager.literal("help")
                                    .executes(KnowledgeCommands::executeHelp))

                            // /kb check <player>
                            .then(CommandManager.literal("check")
                                    .requires(src -> src.hasPermissionLevel(2))
                                    .then(CommandManager.argument("player", EntityArgumentType.player())
                                            .executes(KnowledgeCommands::executeCheckOther)))

                            // /kb set <player> <knowledge> <tier>
                            .then(CommandManager.literal("set")
                                    .requires(src -> src.hasPermissionLevel(2))
                                    .then(CommandManager.argument("player", EntityArgumentType.player())
                                            .then(CommandManager.argument("knowledge", StringArgumentType.word())
                                                    .suggests(KNOWLEDGE_SUGGESTIONS)
                                                    .then(CommandManager.argument("tier", IntegerArgumentType.integer(0, 10))
                                                            .executes(KnowledgeCommands::executeSet)))))

                            // /kb reset <player> [knowledge]
                            .then(CommandManager.literal("reset")
                                    .requires(src -> src.hasPermissionLevel(2))
                                    .then(CommandManager.argument("player", EntityArgumentType.player())
                                            .executes(KnowledgeCommands::executeResetAll)
                                            .then(CommandManager.argument("knowledge", StringArgumentType.word())
                                                    .suggests(KNOWLEDGE_SUGGESTIONS)
                                                    .executes(KnowledgeCommands::executeResetOne))))

                            // /kb reload
                            .then(CommandManager.literal("reload")
                                    .requires(src -> src.hasPermissionLevel(2))
                                    .executes(KnowledgeCommands::executeReload))

                            // /kb hud — toggle sidebar (any player)
                            .then(CommandManager.literal("hud")
                                    .executes(KnowledgeCommands::executeHud))

                            // /kb gui — open knowledge chest GUI
                            .then(CommandManager.literal("gui")
                                    .executes(KnowledgeCommands::executeGui))

                            // /kb list — list all registered knowledges
                            .then(CommandManager.literal("list")
                                    .executes(KnowledgeCommands::executeList))

                            // /kb grant <player> <knowledge> <minutes>
                            .then(CommandManager.literal("grant")
                                    .requires(src -> src.hasPermissionLevel(2))
                                    .then(CommandManager.argument("player", EntityArgumentType.player())
                                            .then(CommandManager.argument("knowledge", StringArgumentType.word())
                                                    .suggests(KNOWLEDGE_SUGGESTIONS)
                                                    .then(CommandManager.argument("minutes", IntegerArgumentType.integer(1, 10000))
                                                            .executes(KnowledgeCommands::executeGrant)))))

                            // /kb give <item> — give a custom KB item to yourself
                            .then(CommandManager.literal("give")
                                    .requires(src -> src.hasPermissionLevel(2))
                                    .then(CommandManager.argument("item", StringArgumentType.word())
                                            .suggests(CUSTOM_ITEM_SUGGESTIONS)
                                            .executes(KnowledgeCommands::executeGive)))

                            // /kb config get <key> / set <key> <value>
                            .then(CommandManager.literal("config")
                                    .requires(src -> src.hasPermissionLevel(2))
                                    .then(CommandManager.literal("get")
                                            .then(CommandManager.argument("key", StringArgumentType.word())
                                                    .suggests(CONFIG_KEY_SUGGESTIONS)
                                                    .executes(KnowledgeCommands::executeConfigGet)))
                                    .then(CommandManager.literal("set")
                                            .then(CommandManager.argument("key", StringArgumentType.word())
                                                    .suggests(CONFIG_KEY_SUGGESTIONS)
                                                    .then(CommandManager.argument("value", StringArgumentType.greedyString())
                                                            .executes(KnowledgeCommands::executeConfigSet)))))

                            // /kb admin — open config GUI
                            .then(CommandManager.literal("admin")
                                    .requires(src -> src.hasPermissionLevel(2))
                                    .executes(KnowledgeCommands::executeAdmin))
            );

            // /checkxp — legacy alias for /kb
            dispatcher.register(
                    CommandManager.literal("checkxp")
                            .requires(src -> src.hasPermissionLevel(0))
                            .executes(KnowledgeCommands::executeSelfCheck)
            );
        });
    }

    // --------------------------------------------------
    // /kb help
    // --------------------------------------------------

    private static int executeHelp(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();

        src.sendFeedback(() -> Text.literal("=== KnowledgeBound Commands ===").formatted(Formatting.GOLD), false);
        src.sendFeedback(() -> Text.literal("/kb").formatted(Formatting.YELLOW)
                .append(Text.literal(" — Show your knowledge levels").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/kb help").formatted(Formatting.YELLOW)
                .append(Text.literal(" — Show this help").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/kb gui").formatted(Formatting.YELLOW)
                .append(Text.literal(" — Open knowledge progress GUI").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/kb list").formatted(Formatting.YELLOW)
                .append(Text.literal(" — List all knowledge types").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/kb check <player>").formatted(Formatting.YELLOW)
                .append(Text.literal(" — Check another player's knowledge [OP]").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/kb set <player> <knowledge> <tier>").formatted(Formatting.YELLOW)
                .append(Text.literal(" — Set a player's tier [OP]").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/kb grant <player> <knowledge> <minutes>").formatted(Formatting.YELLOW)
                .append(Text.literal(" — Grant XP minutes to a player [OP]").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/kb give <item>").formatted(Formatting.YELLOW)
                .append(Text.literal(" — Give yourself a custom KB item [OP]").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/kb reset <player> [knowledge]").formatted(Formatting.YELLOW)
                .append(Text.literal(" — Reset one or all knowledges [OP]").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/kb reload").formatted(Formatting.YELLOW)
                .append(Text.literal(" — Reload config from disk [OP]").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/kb hud").formatted(Formatting.YELLOW)
                .append(Text.literal(" — Toggle knowledge sidebar display").formatted(Formatting.GRAY)), false);
        src.sendFeedback(() -> Text.literal("/kb admin").formatted(Formatting.YELLOW)
                .append(Text.literal(" — Open admin config GUI [OP]").formatted(Formatting.GRAY)), false);

        return Command.SINGLE_SUCCESS;
    }

    // --------------------------------------------------
    // /kb admin
    // --------------------------------------------------

    private static int executeAdmin(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player;
        try {
            player = src.getPlayerOrThrow();
        } catch (Exception e) {
            src.sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }

        ConfigGuiHandler.openMainMenu(player);
        return Command.SINGLE_SUCCESS;
    }

    // --------------------------------------------------
    // /kb  (self-check)
    // --------------------------------------------------

    private static int executeSelfCheck(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player;
        try {
            player = src.getPlayerOrThrow();
        } catch (Exception e) {
            src.sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }
        sendKnowledgeList(src, player, "Your");
        return Command.SINGLE_SUCCESS;
    }

    // --------------------------------------------------
    // /kb check <player>
    // --------------------------------------------------

    private static int executeCheckOther(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity target;
        try {
            target = EntityArgumentType.getPlayer(ctx, "player");
        } catch (Exception e) {
            src.sendError(Text.literal("Player not found."));
            return 0;
        }
        sendKnowledgeList(src, target, target.getName().getString() + "'s");
        return Command.SINGLE_SUCCESS;
    }

    // --------------------------------------------------
    // /kb set <player> <knowledge> <tier>
    // --------------------------------------------------

    private static int executeSet(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();

        ServerPlayerEntity target;
        try {
            target = EntityArgumentType.getPlayer(ctx, "player");
        } catch (Exception e) {
            src.sendError(Text.literal("Player not found."));
            return 0;
        }

        String knowledgeName = StringArgumentType.getString(ctx, "knowledge");
        int tier = IntegerArgumentType.getInteger(ctx, "tier");

        Identifier knowledgeId = resolveKnowledge(knowledgeName);
        if (knowledgeId == null) {
            src.sendError(Text.literal("Unknown knowledge: " + knowledgeName + ". Use tab-complete for valid names."));
            return 0;
        }

        KnowledgeDefinition def = KnowledgeRegistry.get(knowledgeId);
        if (def == null) {
            src.sendError(Text.literal("Knowledge definition not found for: " + knowledgeName));
            return 0;
        }

        if (tier > def.getMaxTier()) {
            src.sendError(Text.literal("Max tier for " + knowledgeName + " is " + def.getMaxTier() + "."));
            return 0;
        }

        PlayerKnowledgeManager.PlayerKnowledgeState state =
                PlayerKnowledgeManager.getState(target, knowledgeId);
        state.tier = tier;
        state.currentMinutes = 0;

        // Refresh the XP bar if the player is online
        PlayerKnowledgeManager.restoreXpBar(target);

        String displayName = titleCase(knowledgeName);
        src.sendFeedback(() -> Text.literal("Set " + target.getName().getString()
                + "'s " + displayName + " to tier " + tier + ".").formatted(Formatting.GREEN), true);

        target.sendMessage(
                Text.literal("Your " + displayName + " knowledge has been set to tier " + tier + "!")
                        .formatted(Formatting.GOLD), false);

        return Command.SINGLE_SUCCESS;
    }

    // --------------------------------------------------
    // /kb reset <player>
    // --------------------------------------------------

    private static int executeResetAll(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();

        ServerPlayerEntity target;
        try {
            target = EntityArgumentType.getPlayer(ctx, "player");
        } catch (Exception e) {
            src.sendError(Text.literal("Player not found."));
            return 0;
        }

        for (KnowledgeDefinition def : KnowledgeRegistry.all()) {
            PlayerKnowledgeManager.PlayerKnowledgeState state =
                    PlayerKnowledgeManager.getState(target, def.getId());
            state.tier = 0;
            state.currentMinutes = 0;
            state.lastXpMinuteIndex = -1L;
        }

        PlayerKnowledgeManager.restoreXpBar(target);

        src.sendFeedback(() -> Text.literal("Reset ALL knowledge for " + target.getName().getString() + ".")
                .formatted(Formatting.GREEN), true);

        target.sendMessage(
                Text.literal("All your knowledge has been reset!").formatted(Formatting.RED), false);

        return Command.SINGLE_SUCCESS;
    }

    // --------------------------------------------------
    // /kb reset <player> <knowledge>
    // --------------------------------------------------

    private static int executeResetOne(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();

        ServerPlayerEntity target;
        try {
            target = EntityArgumentType.getPlayer(ctx, "player");
        } catch (Exception e) {
            src.sendError(Text.literal("Player not found."));
            return 0;
        }

        String knowledgeName = StringArgumentType.getString(ctx, "knowledge");
        Identifier knowledgeId = resolveKnowledge(knowledgeName);
        if (knowledgeId == null) {
            src.sendError(Text.literal("Unknown knowledge: " + knowledgeName));
            return 0;
        }

        PlayerKnowledgeManager.PlayerKnowledgeState state =
                PlayerKnowledgeManager.getState(target, knowledgeId);
        state.tier = 0;
        state.currentMinutes = 0;
        state.lastXpMinuteIndex = -1L;

        PlayerKnowledgeManager.restoreXpBar(target);

        String displayName = titleCase(knowledgeName);
        src.sendFeedback(() -> Text.literal("Reset " + displayName + " for " + target.getName().getString() + ".")
                .formatted(Formatting.GREEN), true);

        target.sendMessage(
                Text.literal("Your " + displayName + " knowledge has been reset!").formatted(Formatting.RED), false);

        return Command.SINGLE_SUCCESS;
    }

    // --------------------------------------------------
    // /kb reload
    // --------------------------------------------------

    private static int executeReload(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();

        KnowledgeBoundConfig.load();

        src.sendFeedback(() -> Text.literal("KnowledgeBound config reloaded!").formatted(Formatting.GREEN), true);
        return Command.SINGLE_SUCCESS;
    }

    // --------------------------------------------------
    // /kb hud
    // --------------------------------------------------

    private static int executeHud(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player;
        try {
            player = src.getPlayerOrThrow();
        } catch (Exception e) {
            src.sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }

        boolean nowOn = KnowledgeScoreboardHud.toggle(player);
        if (nowOn) {
            src.sendFeedback(() -> Text.literal("Knowledge HUD enabled.").formatted(Formatting.GREEN), false);
        } else {
            src.sendFeedback(() -> Text.literal("Knowledge HUD disabled.").formatted(Formatting.GREEN), false);
        }
        return Command.SINGLE_SUCCESS;
    }
    // --------------------------------------------------
    // /kb gui
    // --------------------------------------------------

    private static int executeGui(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player;
        try {
            player = src.getPlayerOrThrow();
        } catch (Exception e) {
            src.sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }

        KnowledgeGuiHandler.open(player);
        return Command.SINGLE_SUCCESS;
    }

    // --------------------------------------------------
    // /kb list
    // --------------------------------------------------

    private static int executeList(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("=== Registered Knowledges ===").formatted(Formatting.GOLD), false);

        for (KnowledgeDefinition def : KnowledgeRegistry.all()) {
            String name = titleCase(def.getId().getPath());
            int maxTier = def.getMaxTier();
            String category = def.getJobCategory().name().replace('_', ' ').toLowerCase();
            String line = String.format("  %s — Max Tier %d (%s)", name, maxTier, category);
            src.sendFeedback(() -> Text.literal(line).formatted(Formatting.YELLOW), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    // --------------------------------------------------
    // /kb grant <player> <knowledge> <minutes>
    // --------------------------------------------------

    private static int executeGrant(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();

        ServerPlayerEntity target;
        try {
            target = EntityArgumentType.getPlayer(ctx, "player");
        } catch (Exception e) {
            src.sendError(Text.literal("Player not found."));
            return 0;
        }

        String name = StringArgumentType.getString(ctx, "knowledge");
        Identifier knowledgeId = resolveKnowledge(name);
        if (knowledgeId == null) {
            src.sendError(Text.literal("Unknown knowledge: " + name + ". Use tab-complete for valid names."));
            return 0;
        }

        KnowledgeDefinition def = KnowledgeRegistry.get(knowledgeId);
        if (def == null) {
            src.sendError(Text.literal("Knowledge definition not found for: " + name));
            return 0;
        }

        int minutes = IntegerArgumentType.getInteger(ctx, "minutes");
        String displayName = titleCase(knowledgeId.getPath());

        // grant the minutes
        PlayerKnowledgeManager.PlayerKnowledgeState state =
                PlayerKnowledgeManager.getState(target, knowledgeId);

        for (int i = 0; i < minutes; i++) {
            PlayerKnowledgeManager.grantMinute(target, knowledgeId);
        }

        // feedback
        ServerPlayerEntity finalTarget = target;
        src.sendFeedback(() -> Text.literal("Granted " + minutes + " minutes of " + displayName + " to " + finalTarget.getName().getString() + ".")
                .formatted(Formatting.GREEN), true);

        target.sendMessage(
                Text.literal("You received " + minutes + " minutes of " + displayName + " experience!")
                        .formatted(Formatting.GOLD),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    // --------------------------------------------------
    // /kb give <item>
    // --------------------------------------------------

    private static int executeGive(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player;
        try {
            player = src.getPlayerOrThrow();
        } catch (Exception e) {
            src.sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }

        String itemName = StringArgumentType.getString(ctx, "item");
        ItemStack stack = CustomItemRegistry.create(itemName);

        if (stack == null) {
            src.sendError(Text.literal("Unknown custom item: " + itemName + ". Use tab-complete for valid items."));
            return 0;
        }

        // give to player
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }

        String displayName = CustomItemRegistry.displayName(itemName);
        src.sendFeedback(() -> Text.literal("Given " + displayName + "!").formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    // --------------------------------------------------
    // Shared helpers
    // --------------------------------------------------

    /** Resolve a knowledge name (e.g. "forestry") to its full Identifier. */
    private static Identifier resolveKnowledge(String name) {
        // Try exact match first (e.g. "forestry" → "knowledgebound:forestry")
        Identifier id = Identifier.of(KnowledgeBound.MOD_ID, name);
        if (KnowledgeRegistry.get(id) != null) {
            return id;
        }
        // Try matching by path suffix (e.g. partial names)
        for (KnowledgeDefinition def : KnowledgeRegistry.all()) {
            if (def.getId().getPath().equals(name)) {
                return def.getId();
            }
        }
        return null;
    }

    /** Convert "melee_combat" → "Melee Combat". */
    private static String titleCase(String input) {
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

    /** Send a formatted knowledge list to the command source. */
    private static void sendKnowledgeList(ServerCommandSource src, ServerPlayerEntity player, String prefix) {
        src.sendFeedback(() -> Text.literal("=== " + prefix + " Knowledge Levels ===")
                .formatted(Formatting.GOLD), false);

        for (KnowledgeDefinition def : KnowledgeRegistry.all()) {
            Identifier id = def.getId();
            PlayerKnowledgeManager.PlayerKnowledgeState state =
                    PlayerKnowledgeManager.getState(player, id);

            int tier = state.tier;
            int nextTier = tier + 1;
            int needed = (nextTier <= def.getMaxTier())
                    ? def.getMinutesForTier(nextTier)
                    : 0;

            String name = titleCase(id.getPath());
            String line;
            if (needed > 0) {
                line = String.format("  %s: Tier %d (%d / %d min)", name, tier, state.currentMinutes, needed);
            } else {
                line = String.format("  %s: Tier %d (MAX)", name, tier);
            }

            Formatting color = (tier == 0) ? Formatting.GRAY
                    : (tier >= def.getMaxTier()) ? Formatting.GREEN
                    : Formatting.YELLOW;

            src.sendFeedback(() -> Text.literal(line).formatted(color), false);
        }
    }

    private static final SuggestionProvider<ServerCommandSource> CONFIG_KEY_SUGGESTIONS =
            (ctx, builder) -> {
                for (String key : KnowledgeBoundConfig.allConfigKeys()) {
                    builder.suggest(key);
                }
                return builder.buildFuture();
            };

    private static int executeConfigGet(CommandContext<ServerCommandSource> ctx) {
        String key = StringArgumentType.getString(ctx, "key");
        String val = KnowledgeBoundConfig.INSTANCE.getFieldValue(key);
        if (val == null) {
            ctx.getSource().sendError(Text.literal("Unknown config key: " + key));
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.literal(key + " = " + val).formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeConfigSet(CommandContext<ServerCommandSource> ctx) {
        String key = StringArgumentType.getString(ctx, "key");
        String valueStr = StringArgumentType.getString(ctx, "value");
        boolean success = KnowledgeBoundConfig.INSTANCE.setFieldValue(key, valueStr);
        if (!success) {
            ctx.getSource().sendError(Text.literal("Failed to set '" + key + "'. Check the key name and value format."));
            return 0;
        }
        KnowledgeBoundConfig.INSTANCE.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Set '" + key + "' to: " + valueStr).formatted(Formatting.GREEN), true);
        return Command.SINGLE_SUCCESS;
    }
}
