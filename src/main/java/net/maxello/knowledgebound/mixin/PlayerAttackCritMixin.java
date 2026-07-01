package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.mechanics.jobs.SlaughteringManager;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detects whether an attack is a critical hit and stores the result
 * in SlaughteringManager for death event processing.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerAttackCritMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void knowledgebound$detectCrit(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        // In order to give proper slaughtering drops, we need to know if the killing blow was a critical hit.
        // Sadly, Minecraft doesn't natively expose a simple "wasCrit" flag we can read later, 
        // so we have to manually replicate the exact same conditions vanilla uses to decide if an attack is a crit!
        
        // First chunk of checks: Falling, not on ground, not climbing, not in water, not blind, not riding a vehicle.
        boolean isCrit = self.fallDistance > 0.0F
                && !self.isOnGround()
                && !self.isClimbing()
                && !self.isTouchingWater()
                && !self.hasStatusEffect(StatusEffects.BLINDNESS)
                && !self.hasVehicle()
                && target instanceof LivingEntity;

        // Next, the player can't be sprinting while doing a critical hit.
        if (isCrit) {
            isCrit = !self.isSprinting();
        }
        
        // Finally, their attack cooldown progress needs to be nearly full (above 90%). 
        // This stops them from just spam-clicking to get crits.
        if (isCrit) {
            isCrit = self.getAttackCooldownProgress(0.5F) > 0.9F;
        }

        // We stash this result into a ThreadLocal boolean inside SlaughteringManager.
        // That way, if this attack ends up actually killing the entity a microsecond later,
        // our death event handler can check this flag and say "Ah, that was a clean critical strike!"
        SlaughteringManager.LAST_ATTACK_WAS_CRIT.set(isCrit);
    }
}


