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
 * The system that makes ores renewable!
 * 
 * When a player mines a natural ore (like iron or diamond), instead of just leaving air,
 * we replace it with a "placeholder" block (usually cobblestone or deepslate).
 * We then set a timer. Once the timer expires, the cobblestone magically turns back
 * into the ore.
 * 
 * We have to save all these timers to the world save files (`PersistentState`) so
 * that if the server crashes or restarts, all the ores still remember when they
 * are supposed to respawn.
 */
public final class OreRespawnManager {

    private OreRespawnManager() {}

    private static final String STATE_KEY = "knowledgebound_ore_respawns";
    
    // We don't need to check every single tick, checking once a second is plenty.
    private static int tickCounter = 0;
    private static final int TICK_INTERVAL = 20; 

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(OreRespawnManager::tick);
        KnowledgeBound.LOGGER.info("[KnowledgeBound] OreRespawnManager initialized.");
    }

    // --- Ore Respawn Entry ---

    /**
     * A simple data class holding all the information about a single ore that is currently waiting to respawn.
     */
    public static class OreRespawnEntry {
        public final BlockPos pos;
        public final String originalBlockId;
        public final String placeholderBlockId;
        public long respawnAtTick; // The exact world tick when it should come back
        public int remainingRespawns; // How many more times it can respawn before dying forever (-1 = infinite)

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

    /**
     * The actual save data object that gets serialized to the world file.
     */
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
     * Called by the block breaking event right after a player successfully mines an ore.
     * We instantly plop down a cobblestone/deepslate block in its place and start the timer.
     */
    public static void scheduleRespawn(ServerWorld world, BlockPos pos, BlockState originalState) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.oreRespawnEnabled) return;

        String oreId = Registries.BLOCK.getId(originalState.getBlock()).toString();

        // Figure out what the "scab" block should be. We usually match deepslate ores with cobbled deepslate.
        String placeholderId = cfg.orePlaceholderMap.getOrDefault(oreId,
                oreId.contains("deepslate") ? "minecraft:cobbled_deepslate" : "minecraft:cobblestone");

        // Swap out the air for the placeholder
        Block placeholderBlock = Registries.BLOCK.get(Identifier.of(placeholderId));
        world.setBlockState(pos, placeholderBlock.getDefaultState(), Block.NOTIFY_ALL);

        // Do the math to figure out when it should come back.
        long respawnAt = world.getServer().getTicks() + cfg.oreRespawnDelayTicks;
        int remaining = cfg.oreRespawnMaxCount;

        // Save it to the tracking list!
        OreRespawnState state = getState(world);
        state.entries.put(pos, new OreRespawnEntry(pos, oreId, placeholderId, respawnAt, remaining));
        state.markDirty();

        KnowledgeBound.LOGGER.debug("[KnowledgeBound] Scheduled ore respawn at {} ({}), respawn at tick {}",
                pos, oreId, respawnAt);
    }

    /**
     * If a player mines the cobblestone placeholder before it turns back into an ore,
     * the ore is gone forever! We cancel the timer.
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
     * Checks if a specific block is actually a placeholder waiting to respawn.
     * We use this to prevent players from getting XP for mining placeholders.
     */
    public static boolean isPlaceholder(ServerWorld world, BlockPos pos) {
        OreRespawnState state = getState(world);
        return state.entries.containsKey(pos);
    }

    /**
     * Checks if an ore is even allowed to respawn based on the config list.
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

    /**
     * Our main loop. Runs every second.
     * Iterates through all tracked ores and sees if any of their timers have expired.
     */
    private static void tick(MinecraftServer server) {
        if (!KnowledgeBoundConfig.INSTANCE.oreRespawnEnabled) return;

        tickCounter++;
        if (tickCounter < TICK_INTERVAL) return;
        tickCounter = 0; // Reset every second

        long currentTick = server.getTicks();

        for (ServerWorld world : server.getWorlds()) {
            OreRespawnState state = getState(world);
            if (state.entries.isEmpty()) continue;

            Iterator<Map.Entry<BlockPos, OreRespawnEntry>> it = state.entries.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, OreRespawnEntry> mapEntry = it.next();
                OreRespawnEntry entry = mapEntry.getValue();

                // Not time yet!
                if (currentTick < entry.respawnAtTick) continue;

                BlockPos pos = entry.pos;

                // Important: Don't try to change blocks in unloaded chunks, or we'll 
                // cause massive lag spikes loading random chunks across the map.
                if (!world.isChunkLoaded(pos)) continue;

                // Did someone or something replace our placeholder with something else?
                // (e.g. they placed dirt over it or a creeper blew it up)
                BlockState currentState = world.getBlockState(pos);
                String currentBlockId = Registries.BLOCK.getId(currentState.getBlock()).toString();
                if (!currentBlockId.equals(entry.placeholderBlockId)) {
                    // The placeholder is gone, so the ore is lost forever.
                    it.remove();
                    state.markDirty();
                    continue;
                }

                // Everything looks good! Turn the cobblestone back into the beautiful ore.
                Block oreBlock = Registries.BLOCK.get(Identifier.of(entry.originalBlockId));
                world.setBlockState(pos, oreBlock.getDefaultState(), Block.NOTIFY_ALL);

                KnowledgeBound.LOGGER.debug("[KnowledgeBound] Restored ore {} at {}", entry.originalBlockId, pos);

                // Handle limited respawns (if config dictates they shouldn't last forever)
                if (entry.remainingRespawns > 0) {
                    entry.remainingRespawns--;
                }

                // If exhausted (count was > 0 and is now 0), remove permanently
                // If unlimited (-1), also remove (it will just get re-scheduled the NEXT time they mine it)
                it.remove();
                state.markDirty();
            }
        }
    }
}



