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

    @Inject(method = "setToDirt", at = @At("HEAD"))
    private static void knowledgebound$onSetToDirt(@Nullable Entity entity, BlockState state, World world, BlockPos pos, CallbackInfo ci) {
        if (entity instanceof ServerPlayerEntity player) {
            KnowledgeEvents.handleCropAboveDestroyed(world, player, pos);
        }
    }
}


