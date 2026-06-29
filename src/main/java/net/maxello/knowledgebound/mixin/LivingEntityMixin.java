package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.SlaughteringManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    public int deathTime;

    /**
     * Prevents vanilla corpse entities from despawning via updatePostDeath.
     * Keeps deathTime frozen at 19 so clients render the mob sideways on the ground.
     */
    @Inject(method = "updatePostDeath", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$keepCorpseEntity(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getCommandTags().contains(SlaughteringManager.CORPSE_TAG)) {
            if (!self.getWorld().isClient()) {
                if (SlaughteringManager.isCorpseExpired(self)) {
                    SlaughteringManager.despawnCorpse(self);
                    ci.cancel();
                    return;
                }
            }
            this.deathTime = 19;
            ci.cancel();
        }
    }

    /**
     * Suppresses standard vanilla loot and XP drops when a mob is killed with a slaughtering tool.
     */
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$suppressLootOnSlaughter(ServerWorld world, DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (SlaughteringManager.shouldSuppressLoot(self, source)) {
            ci.cancel();
        }
    }
}
