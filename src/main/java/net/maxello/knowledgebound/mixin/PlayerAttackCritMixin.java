package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.SlaughteringManager;
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

        // Replicate vanilla critical hit conditions from PlayerEntity.attack()
        boolean isCrit = self.fallDistance > 0.0F
                && !self.isOnGround()
                && !self.isClimbing()
                && !self.isTouchingWater()
                && !self.hasStatusEffect(StatusEffects.BLINDNESS)
                && !self.hasVehicle()
                && target instanceof LivingEntity;

        // Vanilla also requires the player is not sprinting for a crit
        // and that the attack cooldown progress is > 0.9
        if (isCrit) {
            isCrit = !self.isSprinting();
        }
        if (isCrit) {
            isCrit = self.getAttackCooldownProgress(0.5F) > 0.9F;
        }

        SlaughteringManager.LAST_ATTACK_WAS_CRIT.set(isCrit);
    }
}
