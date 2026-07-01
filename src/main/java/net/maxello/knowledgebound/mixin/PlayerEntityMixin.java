package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    /**
     * Minecraft usually subtracts XP levels by calling addExperienceLevels with a negative number.
     * Since this mod completely reworks how XP is used, we want to block the game from draining
     * the player's vanilla experience levels (e.g., when they use an anvil or enchant something).
     */
    @Inject(method = "addExperienceLevels", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$noNegativeLevels(int levels, CallbackInfo ci) {
        // If the game is trying to take levels away (negative value), we just say "No thanks!" and cancel it.
        // Earning levels (positive value) is still perfectly fine.
        if (levels < 0) {
            ci.cancel();
        }
    }
}

