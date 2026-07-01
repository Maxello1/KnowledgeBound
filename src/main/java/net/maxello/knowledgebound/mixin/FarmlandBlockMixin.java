package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.mechanics.gathering.KnowledgeEvents;

import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
public class FarmlandBlockMixin {

    // This mixin hooks right into the moment farmland turns back into regular dirt.
    // Why? Because if the farmland reverts (like when a player jumps on it), any crop on top of it gets popped off!
    // We want to make sure the player doesn't skip out on our custom crop-destroy logic just by breaking the soil under it.
    @Inject(method = "setToDirt", at = @At("HEAD"))
    private static void knowledgebound$onSetToDirt(@Nullable Entity entity, BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        // Check if the entity causing the dirt reversion is actually a player on the server side.
        // We only care about player actions here, since a pig trampling crops shouldn't trigger player knowledge events.
        if (entity instanceof ServerPlayerEntity player) {
            // Tell the KnowledgeEvents system that a crop might have just been destroyed at this position.
            // It will handle the rest (checking if a crop actually existed, processing knowledge drops, etc.).
            KnowledgeEvents.handleCropAboveDestroyed(world, player, pos);
        }
    }
}


