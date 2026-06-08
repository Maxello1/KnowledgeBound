package net.maxello.knowledgebound.mixin;

import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantmentScreenHandlerMixin {

    /**
     * Temporarily set the player's XP level high enough so vanilla's
     * level-check inside onButtonClick passes. We do NOT cancel the method,
     * allowing vanilla to apply the enchantment normally.
     * PlayerEntityMixin already blocks the negative addExperienceLevels call,
     * so the player won't actually lose XP levels.
     */
    @Inject(method = "onButtonClick", at = @At("HEAD"))
    private void knowledgebound$freeEnchant(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        // Give the player enough levels to pass any enchanting requirement
        if (player.experienceLevel < 30) {
            player.experienceLevel = 30;
        }
    }
}
