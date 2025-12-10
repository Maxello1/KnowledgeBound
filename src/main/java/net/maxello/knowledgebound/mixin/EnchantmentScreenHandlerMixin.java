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
     * Remove XP level requirement from enchanting table.
     * We let vanilla continue, but always report: "button click handled".
     */
    @Inject(method = "onButtonClick", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$freeEnchant(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        // The original method checks level + subtracts XP.
        // Instead, we just say "yes" and let PlayerEntityMixin prevent XP cost.
        cir.setReturnValue(true);
    }
}
