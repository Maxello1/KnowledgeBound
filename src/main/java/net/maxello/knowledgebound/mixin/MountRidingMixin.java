package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.*;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into AbstractHorseEntity.interactMob to gate mounting/taming behind Husbandry.
 * Covers Horse, Donkey, Mule, Llama, Camel (all extend AbstractHorseEntity).
 *
 * Mounting requires at least Tier 1 (not completely untrained).
 * Riding below the animal's required tier is unreliable (kick-off handled by HusbandryEvents tick).
 */
@Mixin(AbstractHorseEntity.class)
public abstract class MountRidingMixin {

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onInteractMob(PlayerEntity player, Hand hand,
                                               CallbackInfoReturnable<ActionResult> cir) {
        if (player.getWorld().isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.husbandryEnabled || !cfg.husbandryRidingEnabled) return;

        AbstractHorseEntity self = (AbstractHorseEntity) (Object) this;
        ItemStack stack = player.getStackInHand(hand);

        // Don't intercept feeding interactions — only gate mounting/taming
        if (self.isBreedingItem(stack)) return;

        int playerTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);

        // Tier 0 — completely untrained, can't interact with mounts at all
        if (playerTier < 1) {
            String msg;
            if (!self.isTame()) {
                msg = cfg.messages.husbandryTamingTierLow.replace("{minTier}", "1");
            } else {
                msg = cfg.messages.husbandryRidingTierLow.replace("{minTier}", "1");
            }
            serverPlayer.sendMessage(Text.literal(msg), true);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // Tier 1+ — allow mounting/taming, riding reliability handled by HusbandryEvents tick
        PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
    }
}
