package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.KnowledgeEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into ScreenHandler.onSlotClick to:
 *
 * 1. Save the cursor stack BEFORE any slot click processing, so
 *    CraftingResultSlotMixin can restore it correctly on craft fail.
 *
 * 2. Intercept normal clicks on the stonecutter output slot.
 *
 * 3. Intercept shift-clicks on ANY CraftingResultSlot to apply knowledge
 *    rules before the item is transferred to inventory (onTakeItem fires
 *    too late for shift-clicks).
 */
@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {

    // PRE_CLICK_CURSOR is stored in KnowledgeEvents (mixin classes can't have
    // non-private static fields since they get merged into the target class).

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onSlotClick(int slotIndex, int button,
                                            SlotActionType actionType,
                                            PlayerEntity player,
                                            CallbackInfo ci) {

        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        ScreenHandler self = (ScreenHandler) (Object) this;

        // block all interactions inside the read-only knowledge GUI
        if (self instanceof GenericContainerScreenHandler) {
            // check if this is our knowledge GUI by testing the title
            // The screen handler's sync ID doesn't help, but we can check if
            // the inventory contains our GUI marker (slot 0 has our category pane)
            try {
                Slot firstSlot = self.slots.get(0);
                if (firstSlot.hasStack()) {
                    ItemStack stack = firstSlot.getStack();
                    var name = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
                    if (name != null && name.getString().contains("Gathering")) {
                        ci.cancel();
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        // save cursor before any processing (stored in KnowledgeEvents, not here,
        // because mixin classes can't have non-private static fields)
        KnowledgeEvents.PRE_CLICK_CURSOR.set(self.getCursorStack().copy());

        // --- Stonecutter output (normal clicks only, shift-click via quickMove) ---
        if (self instanceof StonecutterScreenHandler handler) {
            if (slotIndex == 1 && actionType != SlotActionType.QUICK_MOVE) {
                Slot outputSlot = handler.slots.get(1);
                if (outputSlot.hasStack() && !outputSlot.getStack().isEmpty()) {
                    if (KnowledgeEvents.handleStonecutterOutput(serverPlayer, handler)) {
                        ci.cancel();
                        return;
                    }
                }
            }
            return;
        }

        // --- Crafting result slot shift-click protection (all screen handlers) ---
        if (actionType != SlotActionType.QUICK_MOVE) return;
        if (slotIndex < 0 || slotIndex >= self.slots.size()) return;

        Slot slot = self.slots.get(slotIndex);
        if (!(slot instanceof CraftingResultSlot)) return;
        if (!slot.hasStack() || slot.getStack().isEmpty()) return;

        // apply crafting knowledge rules BEFORE vanilla transfers the item
        ItemStack stack = slot.getStack();
        Identifier itemId = Registries.ITEM.getId(stack.getItem());

        KnowledgeBound.LOGGER.debug("[KB] Shift-click craft intercepted: {}", itemId);

        ItemStack modified = KnowledgeEvents.handleCrafting(
                serverPlayer,
                itemId,
                stack.copy()
        );

        if (modified == null || modified == stack) {
            // no rule matched — let vanilla handle normally
            return;
        }

        if (modified.isEmpty()) {
            // craft failed — consume ingredients, cancel the shift-click
            KnowledgeBound.LOGGER.debug("[KB] Shift-click craft FAILED for {}", itemId);
            slot.setStack(ItemStack.EMPTY);
            slot.onTakeItem(player, stack);
            self.sendContentUpdates();
            ci.cancel();
        } else {
            // poor quality — replace the stack in the slot before vanilla transfers it
            KnowledgeBound.LOGGER.debug("[KB] Shift-click craft POOR for {}, dmg={}", itemId, modified.getDamage());
            slot.setStack(modified);
            // let vanilla continue with the modified stack
        }
    }
}
