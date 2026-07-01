package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;

import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Our mod completely replaces the vanilla XP system. You don't learn things by absorbing 
 * glowing green orbs from dead pigs anymore. You learn by actually doing things!
 * So, we need to make sure players can't pick up these old XP orbs.
 */
@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin {

    /**
     * We inject into the exact moment the player touches the orb.
     * Before vanilla can play the little ding sound and increase their XP bar,
     * we aggressively step in and say "Nope, this orb doesn't exist anymore."
     */
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$noXpPickup(PlayerEntity player, CallbackInfo ci) {
        // Just delete the orb entirely. Poof. Gone. 
        ((ExperienceOrbEntity)(Object)this).discard(); 
        
        // Cancel the collision event so vanilla doesn't even know it happened.
        ci.cancel();
    }
}

