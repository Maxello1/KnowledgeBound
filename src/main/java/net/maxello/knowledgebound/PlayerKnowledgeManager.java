package net.maxello.knowledgebound;


import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.KnowledgeDefinition;
import net.maxello.knowledgebound.KnowledgeRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerKnowledgeManager {

    public static class PlayerKnowledgeState {
        public int tier;
        public int currentMinutes;
        public long lastXpMinuteIndex;

        public PlayerKnowledgeState() {
            this.tier = 0;
            this.currentMinutes = 0;
            this.lastXpMinuteIndex = -1L;
        }
    }

    private static final Map<UUID, Map<Identifier, PlayerKnowledgeState>> PLAYER_DATA = new HashMap<>();

    public static void init() {
        KnowledgeBound.LOGGER.info("[KnowledgeBound] PlayerKnowledgeManager initialized.");
    }

    private static Map<Identifier, PlayerKnowledgeState> getOrCreatePlayerMap(ServerPlayerEntity player) {
        return PLAYER_DATA.computeIfAbsent(player.getUuid(), uuid -> new HashMap<>());
    }

    public static PlayerKnowledgeState getState(ServerPlayerEntity player, Identifier knowledgeId) {
        Map<Identifier, PlayerKnowledgeState> map = getOrCreatePlayerMap(player);
        return map.computeIfAbsent(knowledgeId, id -> new PlayerKnowledgeState());
    }

    public static void grantMinuteIfAllowed(ServerPlayerEntity player, Identifier knowledgeId) {
        KnowledgeDefinition def = KnowledgeRegistry.get(knowledgeId);
        if (def == null) return;

        PlayerKnowledgeState state = getState(player, knowledgeId);
        long currentMinute = player.getWorld().getTime() / (20L * 60L);

        if (currentMinute <= state.lastXpMinuteIndex) {
            return;
        }

        state.lastXpMinuteIndex = currentMinute;
        state.currentMinutes += 1;

        // 🔧 TEMP DEBUG: show that XP was gained
        player.sendMessage(
                net.minecraft.text.Text.literal(
                        "[KB] +1 minute in " + knowledgeId.getPath() +
                                " (" + state.currentMinutes + " / " + def.getMinutesForTier(state.tier + 1) + ")"
                ),
                false
        );

        tryLevelUp(player, knowledgeId, def, state);
    }


    private static void tryLevelUp(ServerPlayerEntity player, Identifier knowledgeId, KnowledgeDefinition def, PlayerKnowledgeState state) {
        int currentTier = state.tier;
        if (currentTier >= def.getMaxTier()) {
            return;
        }

        int nextTier = currentTier + 1;
        int needed = def.getMinutesForTier(nextTier);
        if (needed <= 0) return;

        if (state.currentMinutes >= needed) {
            state.currentMinutes -= needed;
            state.tier = nextTier;
            player.sendMessage(
                    net.minecraft.text.Text.literal(
                            "Knowledge increased in " + knowledgeId.getPath() + " (Tier " + nextTier + ")"
                    ),
                    false
            );

        }
    }

    public static int getTier(ServerPlayerEntity player, Identifier knowledgeId) {
        return getState(player, knowledgeId).tier;
    }
}
