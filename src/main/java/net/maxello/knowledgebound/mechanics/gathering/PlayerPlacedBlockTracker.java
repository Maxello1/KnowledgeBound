package net.maxello.knowledgebound.mechanics.gathering;

import net.maxello.knowledgebound.KnowledgeBound;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashSet;
import java.util.Set;

/**
 * A simple tracker that remembers exactly which blocks a player has placed down.
 * 
 * Why do we need this? Because of the Ore Respawn system! 
 * If a player mines an iron ore, it gets replaced by a placeholder and then respawns later.
 * But what if a player mines an iron ore, picks it up (with Silk Touch), and places it back down?
 * We don't want them to mine that same placed block and trigger the respawn logic again.
 * That would lead to infinite ore farming. So we track player-placed blocks here.
 */
public final class PlayerPlacedBlockTracker {

    private PlayerPlacedBlockTracker() {}

    // The key used to save this data into the world's level.dat folder.
    private static final String STATE_KEY = "knowledgebound_player_placed";

    // --- PersistentState ---

    /**
     * Minecraft's built-in way to save arbitrary data to the world save file.
     * We just store a massive set of coordinates where players have placed blocks.
     */
    public static class PlacedBlockState extends PersistentState {
        // A HashSet is perfect here because we just need to do extremely fast lookups.
        public final Set<BlockPos> placedPositions = new HashSet<>();

        public PlacedBlockState() {}

        public static PlacedBlockState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
            PlacedBlockState state = new PlacedBlockState();
            NbtList list = nbt.getList("positions", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound posNbt = list.getCompound(i);
                state.placedPositions.add(new BlockPos(
                        posNbt.getInt("x"), posNbt.getInt("y"), posNbt.getInt("z")));
            }
            return state;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
            NbtList list = new NbtList();
            for (BlockPos pos : placedPositions) {
                NbtCompound posNbt = new NbtCompound();
                posNbt.putInt("x", pos.getX());
                posNbt.putInt("y", pos.getY());
                posNbt.putInt("z", pos.getZ());
                list.add(posNbt);
            }
            nbt.put("positions", list);
            return nbt;
        }

        public static PersistentState.Type<PlacedBlockState> getType() {
            return new PersistentState.Type<>(
                    PlacedBlockState::new,
                    PlacedBlockState::fromNbt,
                    null
            );
        }
    }

    // --- Public API ---

    /**
     * Called whenever a player right-clicks to place a block.
     * We only bother recording this if the block they placed is an ore that
     * is eligible for respawning (handled upstream).
     */
    public static void onPlayerPlace(ServerWorld world, BlockPos pos) {
        PlacedBlockState state = getState(world);
        
        // We have to call .toImmutable() because sometimes BlockPos instances 
        // are mutable and recycled by Minecraft, which would corrupt our HashSet!
        state.placedPositions.add(pos.toImmutable());
        
        // Tells Minecraft "hey, this data changed, make sure you save it to disk eventually".
        state.markDirty();
    }

    /**
     * Called whenever ANY block is broken. 
     * If they broke a block we were tracking, we remove it from the list to free up memory.
     */
    public static void onBlockBreak(ServerWorld world, BlockPos pos) {
        PlacedBlockState state = getState(world);
        if (state.placedPositions.remove(pos)) {
            state.markDirty();
        }
    }

    /**
     * The core check used by the mining event.
     * If this returns true, the block was placed by a player, so we do NOT
     * trigger any ore respawn logic.
     */
    public static boolean isPlayerPlaced(ServerWorld world, BlockPos pos) {
        return getState(world).placedPositions.contains(pos);
    }

    private static PlacedBlockState getState(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                PlacedBlockState.getType(),
                STATE_KEY
        );
    }
}


