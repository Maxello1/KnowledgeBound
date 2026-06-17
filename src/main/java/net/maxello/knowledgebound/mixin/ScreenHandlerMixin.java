package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.KnowledgeEvents;
import net.maxello.knowledgebound.SupervisedJob;
import net.maxello.knowledgebound.SupervisedJobManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
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

        // --- Furnace output slot collection (supervised jobs) ---
        if (self instanceof AbstractFurnaceScreenHandler) {
            // Slot 2 is the output slot in furnaces/smokers/blast furnaces
            if (slotIndex == 2) {
                Slot outputSlot = self.slots.get(2);
                if (outputSlot.hasStack() && !outputSlot.getStack().isEmpty()) {
                    if (serverPlayer.getWorld() instanceof ServerWorld serverWorld) {
                        // Find the furnace position from the player's vicinity
                        BlockPos furnacePos = SupervisedJobManager.findFurnacePosForPlayer(serverPlayer);
                        if (furnacePos != null) {
                            boolean allowed = SupervisedJobManager.onItemCollected(
                                    serverPlayer, serverWorld, furnacePos);
                            if (!allowed) {
                                // Fail — consume the output
                                outputSlot.setStack(ItemStack.EMPTY);
                                self.sendContentUpdates();
                                ci.cancel();
                                return;
                            }
                            // Success — let vanilla handle the pickup
                        }
                    }
                }
            }
            // Block manual insertion into input slot
            if (slotIndex == 0) {
                ItemStack incoming = ItemStack.EMPTY;
                if (actionType == SlotActionType.PICKUP) {
                    incoming = serverPlayer.currentScreenHandler.getCursorStack();
                } else if (actionType == SlotActionType.QUICK_CRAFT) {
                    incoming = serverPlayer.currentScreenHandler.getCursorStack();
                } else if (actionType == SlotActionType.SWAP) {
                    incoming = serverPlayer.getInventory().getStack(button);
                }

                if (!incoming.isEmpty()) {
                    Identifier id = Registries.ITEM.getId(incoming.getItem());
                    SupervisedJob.JobType jt = SupervisedJobManager.getJobTypeForItem(id);
                    if (jt != null) {
                        // How many items are trying to be added?
                        Slot inputSlot = self.slots.get(0);
                        int currentCount = inputSlot.hasStack() ? inputSlot.getStack().getCount() : 0;
                        int incomingCount = actionType == SlotActionType.PICKUP && button == 1 ? 1 : incoming.getCount();

                        if (currentCount + incomingCount > 1) {
                            KnowledgeBound.LOGGER.debug("[KB] Blocked manual insertion of >1 supervised items");
                            ci.cancel();
                            return;
                        }
                    }
                }
            }

            // Also block shift-clicking stacks of supervised items INTO the input slot
            if (actionType == SlotActionType.QUICK_MOVE && slotIndex >= 3) {
                // Player shift-clicking from their inventory into furnace
                Slot sourceSlot = self.slots.get(slotIndex);
                if (sourceSlot.hasStack()) {
                    ItemStack sourceStack = sourceSlot.getStack();
                    Identifier sourceItemId = Registries.ITEM.getId(sourceStack.getItem());
                    SupervisedJob.JobType jt = SupervisedJobManager.getJobTypeForItem(sourceItemId);
                    if (jt != null) {
                        // Check if there's already an item in the input slot
                        ItemStack currentInput = self.slots.get(0).getStack();
                        if (currentInput.isEmpty()) {
                            // Allow only 1 item via shift-click: place 1, keep rest
                            ItemStack singleItem = sourceStack.copy();
                            singleItem.setCount(1);
                            self.slots.get(0).setStack(singleItem);
                            sourceStack.decrement(1);
                            self.sendContentUpdates();
                            ci.cancel();
                            return;
                        } else {
                            // Input slot already has something — block
                            ci.cancel();
                            return;
                        }
                    }
                }
            }
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

        KnowledgeEvents.SKIP_NEXT_ROLL.set(true);
        ItemStack modified;
        try {
            modified = KnowledgeEvents.handleCrafting(
                    serverPlayer,
                    itemId,
                    stack.copy()
            );
        } finally {
            // we do NOT clear it here! We must let it remain true so that 
            // the impending slot.onTakeItem (whether called by us below or by vanilla later)
            // will see it and skip. It will be cleared inside onTakeItem.
        }

        if (modified == null || modified == stack) {
            // no rule matched — let vanilla handle normally.
            // Clear the flag here so it doesn't leak. If it reaches CraftingResultSlotMixin, 
            // it will process normally and also find no rule.
            KnowledgeEvents.SKIP_NEXT_ROLL.set(false);
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

    /**
     * Track when a player closes a furnace screen for supervised job grace periods.
     * onClosed lives on ScreenHandler (parent), so we filter for furnace handlers.
     */
    @Inject(method = "onClosed", at = @At("HEAD"))
    private void knowledgebound$onFurnaceClosed(PlayerEntity player, CallbackInfo ci) {
        ScreenHandler self = (ScreenHandler) (Object) this;
        if (self instanceof AbstractFurnaceScreenHandler) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                SupervisedJobManager.onPlayerCloseScreen(serverPlayer);
            }
        }
    }
}
