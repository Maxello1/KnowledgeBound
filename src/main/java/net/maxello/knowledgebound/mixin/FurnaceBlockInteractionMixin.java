package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.mechanics.jobs.SupervisedJobManager;

import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlock.class)
public class FurnaceBlockInteractionMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$blockFurnaceAccess(BlockState state, World world, BlockPos pos,
                                                    PlayerEntity player, BlockHitResult hit,
                                                    CallbackInfoReturnable<ActionResult> cir) {
        if (world.isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        if (!SupervisedJobManager.onPlayerOpenScreen(serverPlayer, serverWorld, pos)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}


