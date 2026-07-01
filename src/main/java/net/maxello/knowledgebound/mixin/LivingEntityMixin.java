package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.mechanics.jobs.SlaughteringManager;

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
     * This intercepts the updatePostDeath logic for living entities.
     * Normally, Minecraft increments the deathTime up to 20 ticks, and then completely deletes the entity.
     * But when we use a slaughtering tool, we want to leave the corpse around to be harvested!
     */
    @Inject(method = "updatePostDeath", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$keepCorpseEntity(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        // Check if this particular entity has our special slaughtering tag on it.
        // If it does, it means the player landed a clean kill with a cleaver and we should preserve it.
        if (self.getCommandTags().contains(SlaughteringManager.CORPSE_TAG)) {
            if (!self.getWorld().isClient()) {
                // We don't want corpses lying around forever creating lag.
                // So if it's been rotting on the ground too long, we tell the manager to finally get rid of it.
                if (SlaughteringManager.isCorpseExpired(self)) {
                    SlaughteringManager.despawnCorpse(self);
                    ci.cancel();
                    return;
                }
            }
            // If the corpse is still fresh, we freeze the death timer at 19 ticks.
            // Why 19? Because at 20 ticks the game deletes it. Sitting at 19 means the client renders the mob
            // fully tipped over on its side, making it look like a proper corpse on the ground!
            this.deathTime = 19;
            ci.cancel();
        }
    }

    /**
     * If a mob is killed with a slaughtering tool, it drops custom loot later when harvested.
     * So we need to stop the normal vanilla item and XP drops right as it dies.
     */
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$suppressLootOnSlaughter(ServerWorld world, DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        // Check with the slaughtering manager. If it says this kill counts as a slaughter,
        // we completely cancel the drop method so no normal items or XP fly out.
        if (SlaughteringManager.shouldSuppressLoot(self, source)) {
            ci.cancel();
        }
    }
}


