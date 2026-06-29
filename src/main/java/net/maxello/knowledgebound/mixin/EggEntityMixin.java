package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBoundConfig;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into EggEntity.onCollision to prevent chicken spawning from thrown eggs
 * when husbandryDisableEggChickenSpawn is enabled.
 * Still handles entity hit damage and egg discard normally.
 */
@Mixin(EggEntity.class)
public abstract class EggEntityMixin {

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onCollision(HitResult hitResult, CallbackInfo ci) {
        EggEntity self = (EggEntity) (Object) this;
        if (self.getWorld().isClient()) return;

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.husbandryEnabled || !cfg.husbandryDisableEggChickenSpawn) return;

        // Cancel vanilla onCollision (which would spawn chickens) and handle manually
        ci.cancel();

        // Handle entity hit damage (same as vanilla — 0 damage but triggers hit)
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            entityHit.getEntity().damage(
                    self.getDamageSources().thrown(self, self.getOwner()), 0.0F
            );
        }

        // Discard the egg entity (same as vanilla end)
        self.discard();
    }
}
