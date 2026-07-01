package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantmentScreenHandlerMixin {

    /**
     * Temporarily inflate the player's XP level so vanilla's level check inside
     * onButtonClick passes. PlayerEntityMixin blocks the resulting negative
     * addExperienceLevels call so no XP is actually spent.
     */
    @Inject(method = "onButtonClick", at = @At("HEAD"))
    private void knowledgebound$freeEnchant(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (player.experienceLevel < 30) {
            player.experienceLevel = 30;
        }
    }

    /**
     * After vanilla finishes onButtonClick, restore the XP bar to the player's
     * actual knowledge progress so the transient level=30 is never visible.
     */
    @Inject(method = "onButtonClick", at = @At("TAIL"))
    private void knowledgebound$restoreXpBar(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            PlayerKnowledgeManager.restoreXpBar(serverPlayer);
        }
    }
}


