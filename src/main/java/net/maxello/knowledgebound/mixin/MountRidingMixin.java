package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.mechanics.animals.HusbandryEvents;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;

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
        // As always, only run this logic on the server side.
        if (player.getWorld().isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        // If husbandry riding isn't even enabled, let the player mount normally without our interference.
        if (!cfg.husbandryEnabled || !cfg.husbandryRidingEnabled) return;

        AbstractHorseEntity self = (AbstractHorseEntity) (Object) this;
        ItemStack stack = player.getStackInHand(hand);

        // We only want to gate the action of actually getting on the animal's back.
        // If they're just feeding it apples or wheat, we should let them do that.
        if (self.isBreedingItem(stack)) return;

        // Find out what level of Husbandry the player has.
        int playerTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);

        // Tier 0 means they haven't learned the very basics of dealing with animals yet.
        // So we completely block them from getting on a mount.
        if (playerTier < 1) {
            String msg;
            // The message changes depending on whether the horse is already tamed or still wild.
            if (!self.isTame()) {
                msg = cfg.messages.husbandryTamingTierLow.replace("{minTier}", "1");
            } else {
                msg = cfg.messages.husbandryRidingTierLow.replace("{minTier}", "1");
            }
            serverPlayer.sendMessage(Text.literal(msg), true);
            // Cancel the vanilla mount action completely.
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // If they are at least Tier 1, we allow them to get on the mount!
        // The actual chance of getting bucked off for being unskilled is handled elsewhere
        // (inside the HusbandryEvents tick handler).
        // But since they interacted with an animal, let's give them some XP!
        PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
    }
}


