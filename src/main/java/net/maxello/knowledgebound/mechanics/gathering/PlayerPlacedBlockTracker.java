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
 * Tracks blocks placed by players so that player-placed ores
 * don't become renewable through the ore respawn system.
 */
public final class PlayerPlacedBlockTracker {

    private PlayerPlacedBlockTracker() {}

    private static final String STATE_KEY = "knowledgebound_player_placed";

    // --- PersistentState ---

    public static class PlacedBlockState extends PersistentState {
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
     * Record that a player placed a block at this position.
     * Only tracks positions for blocks in the respawnable ore list.
     */
    public static void onPlayerPlace(ServerWorld world, BlockPos pos) {
        PlacedBlockState state = getState(world);
        state.placedPositions.add(pos.toImmutable());
        state.markDirty();
    }

    /**
     * Remove tracking when a block is broken at this position.
     */
    public static void onBlockBreak(ServerWorld world, BlockPos pos) {
        PlacedBlockState state = getState(world);
        if (state.placedPositions.remove(pos)) {
            state.markDirty();
        }
    }

    /**
     * Check if a block at this position was placed by a player.
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


