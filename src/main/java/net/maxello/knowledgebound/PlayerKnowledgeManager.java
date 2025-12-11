package net.maxello.knowledgebound;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
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

    // In-memory storage: per-player, per-knowledge state
    private static final Map<UUID, Map<Identifier, PlayerKnowledgeState>> PLAYER_DATA = new HashMap<>();

    // NBT key under which we store our data on the player
    private static final String NBT_KEY = "knowledgebound_knowledge";

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

    /**
     * Grants 1 "minute" of XP if at least one real-time minute has passed
     * since the last gain for this knowledge.
     * Also updates the XP bar to show this knowledge's progress.
     */
    public static void grantMinuteIfAllowed(ServerPlayerEntity player, Identifier knowledgeId) {
        KnowledgeDefinition def = KnowledgeRegistry.get(knowledgeId);
        if (def == null) return;

        PlayerKnowledgeState state = getState(player, knowledgeId);
        long currentMinute = player.getWorld().getTime() / (20L * 60L);

        boolean gainedMinute = false;

        // Only one XP tick per real-time minute per knowledge
        if (currentMinute > state.lastXpMinuteIndex) {
            state.lastXpMinuteIndex = currentMinute;
            state.currentMinutes += 1;
            gainedMinute = true;

            int nextTier = state.tier + 1;
            int neededForNext = (nextTier <= def.getMaxTier())
                    ? def.getMinutesForTier(nextTier)
                    : 0;

            if (neededForNext > 0) {
                boolean isSmithingKnowledge =
                        knowledgeId.equals(KnowledgeRegistry.TOOLSMITHING_ID) ||
                                knowledgeId.equals(KnowledgeRegistry.WEAPONSMITHING_ID) ||
                                knowledgeId.equals(KnowledgeRegistry.ARMOURING_ID);

                if (!isSmithingKnowledge) {
                    player.sendMessage(
                            KnowledgeBoundTextFormatter.learningTick(knowledgeId),
                            true // action bar
                    );
                }
            }

            // Only try to level up if we actually gained XP
            tryLevelUp(player, knowledgeId, def, state);
        }

        // ALWAYS: XP bar should reflect this knowledge's current state
        updateXpBarForKnowledge(player, knowledgeId, def, state);
    }


    private static void tryLevelUp(ServerPlayerEntity player,
                                   Identifier knowledgeId,
                                   KnowledgeDefinition def,
                                   PlayerKnowledgeState state) {
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
                    KnowledgeBoundTextFormatter.levelUp(knowledgeId, nextTier),
                    true // action bar
            );
        }
    }

    /**
     * Sets the vanilla XP bar to represent this knowledge's tier + progress.
     * Level number = current tier, bar progress = minutes / minutes needed.
     */
    private static void updateXpBarForKnowledge(ServerPlayerEntity player,
                                                Identifier knowledgeId,
                                                KnowledgeDefinition def,
                                                PlayerKnowledgeState state) {
        // XP level number = current tier
        int levelDisplay = state.tier;

        int nextTier = state.tier + 1;
        int needed = def.getMinutesForTier(nextTier);
        float progress;

        if (nextTier > def.getMaxTier() || needed <= 0) {
            // Max tier: full bar
            progress = 1.0f;
        } else {
            progress = (float) state.currentMinutes / (float) needed;
        }

        // Directly manipulate vanilla XP fields
        player.experienceLevel = levelDisplay;
        player.experienceProgress = progress;
        player.totalExperience = 0; // we don't use vanilla XP totals

        // Force vanilla to sync the XP bar to the client
        player.addExperience(0);
    }


    public static int getTier(ServerPlayerEntity player, Identifier knowledgeId) {
        return getState(player, knowledgeId).tier;
    }

    // ---------------------------------------------------------------------
    // Persistence: write/read to/from player NBT data
    // ---------------------------------------------------------------------

    public static void writeToNbt(ServerPlayerEntity player, NbtCompound root) {
        Map<Identifier, PlayerKnowledgeState> map = PLAYER_DATA.get(player.getUuid());
        if (map == null || map.isEmpty()) {
            return;
        }

        NbtList list = new NbtList();

        for (Map.Entry<Identifier, PlayerKnowledgeState> entry : map.entrySet()) {
            Identifier id = entry.getKey();
            PlayerKnowledgeState state = entry.getValue();

            NbtCompound tag = new NbtCompound();
            tag.putString("id", id.toString());
            tag.putInt("tier", state.tier);
            tag.putInt("minutes", state.currentMinutes);
            tag.putLong("lastMinute", state.lastXpMinuteIndex);

            list.add(tag);
        }

        root.put(NBT_KEY, list);
    }

    public static void readFromNbt(ServerPlayerEntity player, NbtCompound root) {
        if (!root.contains(NBT_KEY, NbtElement.LIST_TYPE)) {
            return;
        }

        NbtList list = root.getList(NBT_KEY, NbtElement.COMPOUND_TYPE);
        Map<Identifier, PlayerKnowledgeState> map = getOrCreatePlayerMap(player);
        map.clear();

        for (int i = 0; i < list.size(); i++) {
            NbtCompound tag = list.getCompound(i);
            if (!tag.contains("id")) continue;

            Identifier id = new Identifier(tag.getString("id"));
            PlayerKnowledgeState state = new PlayerKnowledgeState();
            state.tier = tag.getInt("tier");
            state.currentMinutes = tag.getInt("minutes");
            state.lastXpMinuteIndex = tag.getLong("lastMinute");

            map.put(id, state);
        }
    }
}
