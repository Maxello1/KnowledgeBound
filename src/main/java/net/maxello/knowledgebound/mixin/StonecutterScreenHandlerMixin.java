package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.mechanics.gathering.KnowledgeEvents;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;

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
     * onButtonClick is called when the player clicks on one of the little recipe buttons in the stonecutter UI.
     * We hijack this to make sure they even know how to use a stonecutter before they try to cut anything.
     */
    @Inject(method = "onButtonClick", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onButtonClick(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        // Check their masonry knowledge level.
        int masonryTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.MASONRY_ID);

        int minTier = KnowledgeBoundConfig.INSTANCE.stonecutterMinTier;

        // If they don't meet the minimum tier required to use the machine...
        if (masonryTier < minTier) {
            // We slap them with a message telling them what tier they need.
            String template = KnowledgeBoundConfig.INSTANCE.messages.stonecutterMinTierLimit;
            String msgStr = template.replace("{minTier}", String.valueOf(minTier));
            serverPlayer.sendMessage(
                    Text.literal(msgStr),
                    true
            );
            // By returning false, we cancel the button click so the recipe isn't actually selected.
            cir.setReturnValue(false);
        }
    }

    /**
     * This handles shift-clicking specifically on the output slot (slot index 1) of the stonecutter.
     * Normal clicks are handled elsewhere, but shift-clicks go through quickMove.
     */
    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$quickMove(PlayerEntity player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        
        // We only care if they are shift-clicking the actual crafted result out of the machine.
        if (slotIndex != 1) return;

        Slot outputSlot = this.slots.get(1);
        if (!outputSlot.hasStack() || outputSlot.getStack().isEmpty()) return;

        // Hand this over to our central gathering/crafting events logic.
        // It'll check fail chances, apply poor quality damage, and grant XP.
        if (KnowledgeEvents.handleStonecutterOutput(serverPlayer, this)) {
            // If that method returns true, it means the craft completely failed (the player broke the stone).
            // So we return an empty item stack, which tells vanilla Minecraft to stop trying to move items into their inventory.
            cir.setReturnValue(ItemStack.EMPTY);
        }
        // If it succeeded (or they made a poor quality item that we replaced in the slot), 
        // we just do nothing here and let vanilla finish moving the item into their bags!
    }
}


