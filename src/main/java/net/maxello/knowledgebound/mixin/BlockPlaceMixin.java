package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.mechanics.gathering.OreRespawnManager;
import net.maxello.knowledgebound.mechanics.gathering.PlayerPlacedBlockTracker;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into block placement so we can track when a player places down a respawnable ore.
 * Without this, the PlayerPlacedBlockTracker would never know about placed ores,
 * and players could silk-touch mine an ore, place it back, and mine it again
 * for infinite ore respawning. Not on our watch!
 */
@Mixin(BlockItem.class)
public abstract class BlockPlaceMixin {

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
            at = @At("RETURN"))
    private void kb_trackOrePlace(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (cir.getReturnValue().isAccepted()
                && context.getWorld() instanceof ServerWorld serverWorld) {
            BlockPos pos = context.getBlockPos();
            BlockState placed = serverWorld.getBlockState(pos);
            if (OreRespawnManager.isRespawnableOre(placed)) {
                PlayerPlacedBlockTracker.onPlayerPlace(serverWorld, pos);
            }
        }
    }
}
