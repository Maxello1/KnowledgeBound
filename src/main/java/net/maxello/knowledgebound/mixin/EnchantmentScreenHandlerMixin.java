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

/**
 * Since our mod repurposes the vanilla XP bar to show the player's knowledge progress, 
 * we can't have them spending their levels on enchantments. 
 * Instead, we make enchanting completely free! But vanilla really, really wants you to have levels.
 */
@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantmentScreenHandlerMixin {

    /**
     * This fires the exact millisecond the player clicks the enchantment button in the UI.
     * Before vanilla even has a chance to check if they can afford it, we step in.
     * We temporarily inflate their XP level to 30. That way, the vanilla check goes 
     * "Oh, level 30? Yeah you can afford this tier 3 enchantment, go right ahead."
     * (We also have another mixin in PlayerEntityMixin that blocks the actual subtraction 
     * of the XP so they don't drop to negatives).
     */
    @Inject(method = "onButtonClick", at = @At("HEAD"))
    private void knowledgebound$freeEnchant(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (player.experienceLevel < 30) {
            player.experienceLevel = 30;
        }
    }

    /**
     * Now that the vanilla method is totally finished doing its thing (it gave them the enchanted item),
     * we have to clean up our mess. We can't leave them at level 30!
     * We ask our PlayerKnowledgeManager to forcibly rewrite their XP bar back to whatever
     * their actual current knowledge progress is. It happens so fast the player never even sees 
     * the bar jump to 30 and back.
     */
    @Inject(method = "onButtonClick", at = @At("TAIL"))
    private void knowledgebound$restoreXpBar(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            PlayerKnowledgeManager.restoreXpBar(serverPlayer);
        }
    }
}


