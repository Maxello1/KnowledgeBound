package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;

import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * We don't want players bypassing the whole Husbandry animal breeding grind by just 
 * throwing 500 eggs at a wall to get an instant chicken farm. 
 * This mixin lets us turn off chicken spawning from thrown eggs entirely.
 */
@Mixin(EggEntity.class)
public abstract class EggEntityMixin {

    // We inject at HEAD to completely intercept the egg breaking before vanilla gets to it.
    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onCollision(HitResult hitResult, CallbackInfo ci) {
        EggEntity self = (EggEntity) (Object) this;
        
        // This stuff only matters on the server.
        if (self.getWorld().isClient()) return;

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        // If they disabled husbandry or disabled this specific rule, let vanilla do its thing.
        if (!cfg.husbandryEnabled || !cfg.husbandryDisableEggChickenSpawn) return;

        // Cancel the vanilla onCollision method. This prevents the random chance of chickens spawning.
        ci.cancel();

        // But wait! We still want the egg to act like an egg when it hits something.
        // Throwing eggs at your friends is a core Minecraft mechanic.
        // So if the egg hit an entity, we manually apply the tiny 0-damage hit, which triggers 
        // the knockback and the "oof" sound.
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            entityHit.getEntity().damage(
                    self.getDamageSources().thrown(self, self.getOwner()), 0.0F
            );
        }

        // And then we manually clean up the egg entity so it doesn't just float there forever.
        // It goes splat!
        self.discard();
    }
}


