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

    // Here we're intercepting right as the player right-clicks a furnace, smoker, or blast furnace.
    // We want to hook this because supervised jobs (like smelting) require the player to stay nearby.
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$blockFurnaceAccess(BlockState state, World world, BlockPos pos,
                                                    PlayerEntity player, BlockHitResult hit,
                                                    CallbackInfoReturnable<ActionResult> cir) {
        // We only care about doing this on the server side where real decisions are made.
        if (world.isClient()) return;
        
        // Safety check to ensure we're dealing with a real player and world.
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        // Ask the SupervisedJobManager if the player is even allowed to open this block.
        // It'll check things like whether they already have an active supervised job at another station.
        if (!SupervisedJobManager.onPlayerOpenScreen(serverPlayer, serverWorld, pos)) {
            // If they aren't allowed, we just shut it down right here. 
            // The block won't open and nothing else happens.
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}


