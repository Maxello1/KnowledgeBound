package net.maxello.knowledgebound;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
        long currentMinute = System.currentTimeMillis() / 60000L;

        // Only one XP tick per real-time minute per knowledge
        if (currentMinute > state.lastXpMinuteIndex) {
            state.lastXpMinuteIndex = currentMinute;
            state.currentMinutes += 1;

            int nextTier = state.tier + 1;
            int neededForNext = (nextTier <= def.getMaxTier())
                    ? def.getMinutesForTier(nextTier)
                    : 0;

            if (neededForNext > 0) {
                boolean isCraftingKnowledge =
                        knowledgeId.equals(KnowledgeRegistry.TOOLSMITHING_ID) ||
                                knowledgeId.equals(KnowledgeRegistry.WEAPONSMITHING_ID) ||
                                knowledgeId.equals(KnowledgeRegistry.ARMOURING_ID);

                if (!isCraftingKnowledge) {
                    player.sendMessage(
                            KnowledgeBoundTextFormatter.learningTick(knowledgeId),
                            true // action bar
                    );
                }
            }

            // Only try to level up if we actually gained XP
            tryLevelUp(player, knowledgeId, def, state);
            sendFullSync(player);
            KnowledgeScoreboardHud.updateScoreboard(player);
        }

        // ALWAYS: XP bar should reflect this knowledge's current state
        updateXpBarForKnowledge(player, knowledgeId, def, state);
    }

    /**
     * Grants one minute of XP unconditionally (bypasses rate limiter).
     * Used by admin commands like /kb grant.
     */
    public static void grantMinute(ServerPlayerEntity player, Identifier knowledgeId) {
        KnowledgeDefinition def = KnowledgeRegistry.get(knowledgeId);
        if (def == null) return;

        PlayerKnowledgeState state = getState(player, knowledgeId);
        state.currentMinutes += 1;

        tryLevelUp(player, knowledgeId, def, state);
        sendFullSync(player);
        KnowledgeScoreboardHud.updateScoreboard(player);
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
            // check proficiency limits before leveling up
            if (!canReachTier(player, knowledgeId, def, nextTier)) {
                // cap the minutes so they don't keep accumulating past the needed
                state.currentMinutes = needed;
                player.sendMessage(
                        KnowledgeBoundTextFormatter.proficiencyLimitReached(knowledgeId),
                        true
                );
                return;
            }

            state.currentMinutes -= needed;
            state.tier = nextTier;

            player.sendMessage(
                    KnowledgeBoundTextFormatter.levelUp(knowledgeId, nextTier),
                    true // action bar
            );
        }
    }

    /**
     * Checks proficiency limits. Returns false if leveling up would exceed the cap.
     */
    private static boolean canReachTier(ServerPlayerEntity player,
                                        Identifier knowledgeId,
                                        KnowledgeDefinition def,
                                        int targetTier) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        KnowledgeDefinition.JobCategory cat = def.getJobCategory();

        if (cat == KnowledgeDefinition.JobCategory.MATERIAL_5_TIER) {
            // check master cap (tier 5)
            if (targetTier >= 5 && cfg.maxMasterMaterial >= 0) {
                int currentMasters = countJobsAtOrAboveTier(player, KnowledgeDefinition.JobCategory.MATERIAL_5_TIER, 5, knowledgeId);
                if (currentMasters >= cfg.maxMasterMaterial) return false;
            }
            // check tier 4+ cap
            if (targetTier >= 4 && cfg.maxTier4Material >= 0) {
                int currentTier4Plus = countJobsAtOrAboveTier(player, KnowledgeDefinition.JobCategory.MATERIAL_5_TIER, 4, knowledgeId);
                if (currentTier4Plus >= cfg.maxTier4Material) return false;
            }
        } else if (cat == KnowledgeDefinition.JobCategory.CLASS_3_TIER) {
            // check master cap (tier 3 for class jobs)
            if (targetTier >= 3 && cfg.maxMasterClass >= 0) {
                int currentMasters = countJobsAtOrAboveTier(player, KnowledgeDefinition.JobCategory.CLASS_3_TIER, 3, knowledgeId);
                if (currentMasters >= cfg.maxMasterClass) return false;
            }
        }

        return true;
    }

    /**
     * Counts how many jobs of the given category the player already has at or above the specified tier,
     * excluding the knowledge we're currently trying to level up.
     */
    private static int countJobsAtOrAboveTier(ServerPlayerEntity player,
                                               KnowledgeDefinition.JobCategory category,
                                               int minTier,
                                               Identifier excludeId) {
        int count = 0;
        Map<Identifier, PlayerKnowledgeState> map = PLAYER_DATA.get(player.getUuid());
        if (map == null) return 0;

        for (Map.Entry<Identifier, PlayerKnowledgeState> entry : map.entrySet()) {
            if (entry.getKey().equals(excludeId)) continue;
            KnowledgeDefinition otherDef = KnowledgeRegistry.get(entry.getKey());
            if (otherDef != null && otherDef.getJobCategory() == category) {
                if (entry.getValue().tier >= minTier) {
                    count++;
                }
            }
        }
        return count;
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

    /**
     * Copy all knowledge data from the old player entity to the new one.
     * Called on respawn when vanilla creates a new ServerPlayerEntity.
     */
    public static void copyData(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer) {
        Map<Identifier, PlayerKnowledgeState> oldMap = PLAYER_DATA.get(oldPlayer.getUuid());
        if (oldMap == null || oldMap.isEmpty()) return;

        // Deep-copy the state so old and new don't share references
        Map<Identifier, PlayerKnowledgeState> newMap = getOrCreatePlayerMap(newPlayer);
        newMap.clear();
        for (Map.Entry<Identifier, PlayerKnowledgeState> entry : oldMap.entrySet()) {
            PlayerKnowledgeState copy = new PlayerKnowledgeState();
            copy.tier = entry.getValue().tier;
            copy.currentMinutes = entry.getValue().currentMinutes;
            copy.lastXpMinuteIndex = entry.getValue().lastXpMinuteIndex;
            newMap.put(entry.getKey(), copy);
        }
    }

    /**
     * Re-sync the XP bar after respawn by picking the player's
     * highest-tier knowledge and displaying its progress.
     */
    public static void restoreXpBar(ServerPlayerEntity player) {
        Map<Identifier, PlayerKnowledgeState> map = PLAYER_DATA.get(player.getUuid());
        if (map == null || map.isEmpty()) return;

        // Find the knowledge with the highest tier
        Identifier bestId = null;
        int bestTier = -1;
        for (Map.Entry<Identifier, PlayerKnowledgeState> entry : map.entrySet()) {
            if (entry.getValue().tier > bestTier) {
                bestTier = entry.getValue().tier;
                bestId = entry.getKey();
            }
        }

        if (bestId != null) {
            KnowledgeDefinition def = KnowledgeRegistry.get(bestId);
            if (def != null) {
                PlayerKnowledgeState state = map.get(bestId);
                updateXpBarForKnowledge(player, bestId, def, state);
            }
        }
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

            Identifier id = Identifier.of(tag.getString("id"));
            PlayerKnowledgeState state = new PlayerKnowledgeState();
            state.tier = tag.getInt("tier");
            state.currentMinutes = tag.getInt("minutes");
            state.lastXpMinuteIndex = tag.getLong("lastMinute");

            map.put(id, state);
        }
    }

    /**
     * Sends the player's full knowledge state to their client for HUD display.
     * Called after any state change (XP gain or level-up).
     */
    public static void sendFullSync(ServerPlayerEntity player) {
        Map<Identifier, PlayerKnowledgeState> map = PLAYER_DATA.get(player.getUuid());
        if (map == null) return;

        Map<String, int[]> data = new HashMap<>();
        for (Map.Entry<Identifier, PlayerKnowledgeState> entry : map.entrySet()) {
            Identifier id = entry.getKey();
            PlayerKnowledgeState state = entry.getValue();
            KnowledgeDefinition def = KnowledgeRegistry.get(id);
            int maxTier = def != null ? def.getMaxTier() : 5;
            int needed = 0;
            if (def != null && state.tier < maxTier) {
                needed = def.getMinutesForTier(state.tier + 1);
            }
            data.put(id.toString(), new int[]{state.tier, state.currentMinutes, needed, maxTier});
        }

        ServerPlayNetworking.send(player, new KnowledgeSyncPayload(data));
    }
}
