package net.maxello.knowledgebound.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    /**
     * Block anything that tries to subtract XP levels (anvil, enchanting, etc.).
     */
    @Inject(method = "addExperienceLevels", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$noNegativeLevels(int levels, CallbackInfo ci) {
        if (levels < 0) {
            ci.cancel();
        }
    }
}
