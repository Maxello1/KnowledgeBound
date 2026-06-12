package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into the stonecutter to:
 * 1. Require masonry tier 1+ to use it (on recipe selection)
 * 2. Apply fail chance + cutting damage + XP on shift-click output (quickMove)
 *
 * Normal click output handling is done in ScreenHandlerMixin.
 */
@Mixin(StonecutterScreenHandler.class)
public abstract class StonecutterScreenHandlerMixin extends ScreenHandler {

    protected StonecutterScreenHandlerMixin(ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    /**
     * onButtonClick is called when the player selects a recipe in the stonecutter.
     * We only use this to gate recipe selection behind masonry tier 1.
     */
    @Inject(method = "onButtonClick", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onButtonClick(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        int masonryTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.MASONRY_ID);

        int minTier = KnowledgeBoundConfig.INSTANCE.stonecutterMinTier;

        // require masonry tier to use stonecutter at all
        if (masonryTier < minTier) {
            String template = KnowledgeBoundConfig.INSTANCE.messages.stonecutterMinTierLimit;
            String msgStr = template.replace("{minTier}", String.valueOf(minTier));
            serverPlayer.sendMessage(
                    Text.literal(msgStr),
                    true
            );
            cir.setReturnValue(false);
        }
    }

    /**
     * quickMove IS overridden by StonecutterScreenHandler so we can @Inject here.
     * This handles shift-click on the output slot (index 1).
     */
    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$quickMove(PlayerEntity player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (slotIndex != 1) return;

        Slot outputSlot = this.slots.get(1);
        if (!outputSlot.hasStack() || outputSlot.getStack().isEmpty()) return;

        if (KnowledgeEvents.handleStonecutterOutput(serverPlayer, this)) {
            // craft failed — return EMPTY to stop the quickMove loop
            cir.setReturnValue(ItemStack.EMPTY);
        }
        // on success, let vanilla handle the actual transfer
    }
}
