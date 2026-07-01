package net.maxello.knowledgebound.mechanics.gathering;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.*;

/**
 * Manages ore respawning — when a natural ore is mined, it's replaced by a placeholder
 * block that reverts to the ore after a configurable delay.
 * Data persists across server restarts via PersistentState.
 */
public final class OreRespawnManager {

    private OreRespawnManager() {}

    private static final String STATE_KEY = "knowledgebound_ore_respawns";
    private static int tickCounter = 0;
    private static final int TICK_INTERVAL = 20; // check once per second

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(OreRespawnManager::tick);
        KnowledgeBound.LOGGER.info("[KnowledgeBound] OreRespawnManager initialized.");
    }

    // --- Ore Respawn Entry ---

    public static class OreRespawnEntry {
        public final BlockPos pos;
        public final String originalBlockId;
        public final String placeholderBlockId;
        public long respawnAtTick;
        public int remainingRespawns;

        public OreRespawnEntry(BlockPos pos, String originalBlockId, String placeholderBlockId,
                               long respawnAtTick, int remainingRespawns) {
            this.pos = pos;
            this.originalBlockId = originalBlockId;
            this.placeholderBlockId = placeholderBlockId;
            this.respawnAtTick = respawnAtTick;
            this.remainingRespawns = remainingRespawns;
        }

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putInt("x", pos.getX());
            nbt.putInt("y", pos.getY());
            nbt.putInt("z", pos.getZ());
            nbt.putString("ore", originalBlockId);
            nbt.putString("placeholder", placeholderBlockId);
            nbt.putLong("respawnAt", respawnAtTick);
            nbt.putInt("remaining", remainingRespawns);
            return nbt;
        }

        public static OreRespawnEntry fromNbt(NbtCompound nbt) {
            BlockPos pos = new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
            return new OreRespawnEntry(
                    pos,
                    nbt.getString("ore"),
                    nbt.getString("placeholder"),
                    nbt.getLong("respawnAt"),
                    nbt.getInt("remaining")
            );
        }
    }

    // --- PersistentState ---

    public static class OreRespawnState extends PersistentState {
        public final Map<BlockPos, OreRespawnEntry> entries = new HashMap<>();

        public OreRespawnState() {}

        public static OreRespawnState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
            OreRespawnState state = new OreRespawnState();
            NbtList list = nbt.getList("entries", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                OreRespawnEntry entry = OreRespawnEntry.fromNbt(list.getCompound(i));
                state.entries.put(entry.pos, entry);
            }
            return state;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
            NbtList list = new NbtList();
            for (OreRespawnEntry entry : entries.values()) {
                list.add(entry.toNbt());
            }
            nbt.put("entries", list);
            return nbt;
        }

        public static PersistentState.Type<OreRespawnState> getType() {
            return new PersistentState.Type<>(
                    OreRespawnState::new,
                    OreRespawnState::fromNbt,
                    null
            );
        }
    }

    // --- Public API ---

    /**
     * Schedule an ore to respawn at the given position.
     * Replaces the ore with its placeholder immediately.
     */
    public static void scheduleRespawn(ServerWorld world, BlockPos pos, BlockState originalState) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.oreRespawnEnabled) return;

        String oreId = Registries.BLOCK.getId(originalState.getBlock()).toString();

        // Get placeholder block ID from config
        String placeholderId = cfg.orePlaceholderMap.getOrDefault(oreId,
                oreId.contains("deepslate") ? "minecraft:cobbled_deepslate" : "minecraft:cobblestone");

        // Place the placeholder
        Block placeholderBlock = Registries.BLOCK.get(Identifier.of(placeholderId));
        world.setBlockState(pos, placeholderBlock.getDefaultState(), Block.NOTIFY_ALL);

        // Calculate respawn time
        long respawnAt = world.getServer().getTicks() + cfg.oreRespawnDelayTicks;
        int remaining = cfg.oreRespawnMaxCount;

        // Store the entry
        OreRespawnState state = getState(world);
        state.entries.put(pos, new OreRespawnEntry(pos, oreId, placeholderId, respawnAt, remaining));
        state.markDirty();

        KnowledgeBound.LOGGER.debug("[KnowledgeBound] Scheduled ore respawn at {} ({}), respawn at tick {}",
                pos, oreId, respawnAt);
    }

    /**
     * Called when a placeholder block is broken by a player or other means.
     * Cancels the respawn permanently.
     */
    public static void cancelRespawn(ServerWorld world, BlockPos pos) {
        OreRespawnState state = getState(world);
        OreRespawnEntry removed = state.entries.remove(pos);
        if (removed != null) {
            state.markDirty();
            KnowledgeBound.LOGGER.debug("[KnowledgeBound] Cancelled ore respawn at {}", pos);
        }
    }

    /**
     * Check if a position is a tracked placeholder (should not grant mining XP).
     */
    public static boolean isPlaceholder(ServerWorld world, BlockPos pos) {
        OreRespawnState state = getState(world);
        return state.entries.containsKey(pos);
    }

    /**
     * Check if a block is a configured respawnable ore.
     */
    public static boolean isRespawnableOre(BlockState state) {
        String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
        return KnowledgeBoundConfig.INSTANCE.respawnableOres.contains(blockId);
    }

    // --- Internal ---

    private static OreRespawnState getState(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                OreRespawnState.getType(),
                STATE_KEY
        );
    }

    private static void tick(MinecraftServer server) {
        if (!KnowledgeBoundConfig.INSTANCE.oreRespawnEnabled) return;

        tickCounter++;
        if (tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;

        long currentTick = server.getTicks();

        for (ServerWorld world : server.getWorlds()) {
            OreRespawnState state = getState(world);
            if (state.entries.isEmpty()) continue;

            Iterator<Map.Entry<BlockPos, OreRespawnEntry>> it = state.entries.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, OreRespawnEntry> mapEntry = it.next();
                OreRespawnEntry entry = mapEntry.getValue();

                if (currentTick < entry.respawnAtTick) continue;

                BlockPos pos = entry.pos;

                // Check if chunk is loaded
                if (!world.isChunkLoaded(pos)) continue;

                // Verify placeholder is still there
                BlockState currentState = world.getBlockState(pos);
                String currentBlockId = Registries.BLOCK.getId(currentState.getBlock()).toString();
                if (!currentBlockId.equals(entry.placeholderBlockId)) {
                    // Placeholder was replaced — cancel
                    it.remove();
                    state.markDirty();
                    continue;
                }

                // Restore the ore
                Block oreBlock = Registries.BLOCK.get(Identifier.of(entry.originalBlockId));
                world.setBlockState(pos, oreBlock.getDefaultState(), Block.NOTIFY_ALL);

                KnowledgeBound.LOGGER.debug("[KnowledgeBound] Restored ore {} at {}", entry.originalBlockId, pos);

                // Handle respawn count
                if (entry.remainingRespawns > 0) {
                    entry.remainingRespawns--;
                }

                // If exhausted (count was > 0 and is now 0), remove permanently
                // If unlimited (-1), also remove (will re-schedule on next mine)
                it.remove();
                state.markDirty();
            }
        }
    }
}



