package net.maxello.knowledgebound;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class KnowledgeCommands {

    private KnowledgeCommands() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // /checkxp
            dispatcher.register(
                    CommandManager.literal("checkxp")
                            .requires(src -> src.hasPermissionLevel(0))
                            .executes(KnowledgeCommands::executeCheckXp)
            );
            // /kb (alias)
            dispatcher.register(
                    CommandManager.literal("kb")
                            .requires(src -> src.hasPermissionLevel(0))
                            .executes(KnowledgeCommands::executeCheckXp)
            );
        });
    }

    private static int executeCheckXp(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player;
        try {
            player = src.getPlayer();
        } catch (Exception e) {
            src.sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }

        src.sendFeedback(() -> Text.literal("=== Knowledge levels ==="), false);

        for (KnowledgeDefinition def : KnowledgeRegistry.all()) {
            Identifier id = def.getId();
            PlayerKnowledgeManager.PlayerKnowledgeState state =
                    PlayerKnowledgeManager.getState(player, id);

            int tier = state.tier;
            int nextTier = tier + 1;
            int needed = (nextTier <= def.getMaxTier())
                    ? def.getMinutesForTier(nextTier)
                    : 0;

            String name = id.getPath().replace('_', ' ');
            String line;
            if (needed > 0) {
                line = String.format("%s: Tier %d (%d / %d min)",
                        name, tier, state.currentMinutes, needed);
            } else {
                line = String.format("%s: Tier %d (MAX)", name, tier);
            }

            src.sendFeedback(() -> Text.literal(line), false);
        }

        return Command.SINGLE_SUCCESS;
    }
}
