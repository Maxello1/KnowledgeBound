package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;

import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin {

    /**
     * Prevent XP orbs from giving XP. We discard the orb immediately so vanilla
     * pickup logic doesn't award experience — KnowledgeBound tracks/awards XP
     * through its own systems.
     */
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$noXpPickup(PlayerEntity player, CallbackInfo ci) {
        ((ExperienceOrbEntity)(Object)this).discard(); // remove orb
        ci.cancel();
    }
}

