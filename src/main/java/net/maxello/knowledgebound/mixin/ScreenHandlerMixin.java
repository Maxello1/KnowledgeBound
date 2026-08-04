package net.maxello.knowledgebound.mixin;
import net.maxello.knowledgebound.util.KbIdHelper;
import net.maxello.knowledgebound.mechanics.jobs.SupervisedJob;
import net.maxello.knowledgebound.mechanics.jobs.SupervisedJobManager;
import net.maxello.knowledgebound.mechanics.gathering.KnowledgeEvents;
import net.maxello.knowledgebound.config.ConfigGuiHandler;

import net.maxello.knowledgebound.KnowledgeBound;
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

        // block all interactions inside the read-only knowledge GUI or config admin GUI
        if (self instanceof GenericContainerScreenHandler) {
            try {
                // Check slot 4 for the main menu marker (Nether Star with specific name)
                Slot markerSlot = self.slots.get(4);
                if (markerSlot.hasStack()) {
                    var markerName = markerSlot.getStack().get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
                    if (markerName != null && markerName.getString().equals(net.maxello.knowledgebound.config.ConfigGuiHandler.MAIN_MENU_MARKER)) {
                        // This is the admin config main menu
                        net.maxello.knowledgebound.config.ConfigGuiHandler.handleClick(
                                serverPlayer, net.maxello.knowledgebound.config.ConfigGuiHandler.MAIN_MENU_TITLE,
                                slotIndex, button, actionType, self);
                        ci.cancel();
                        return;
                    }
                }

                // Check slot 8 for category submenu marker (Nether Star in position 8)
                Slot catMarkerSlot = self.slots.get(8);
                if (catMarkerSlot.hasStack()) {
                    var catMarkerName = catMarkerSlot.getStack().get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
                    if (catMarkerName != null) {
                        String catNameStr = catMarkerName.getString();
                        // Category submenus have a Nether Star at slot 8 with "§b§l<CategoryName>"
                        // and an arrow at slot 0 with "§6§l⬅ Back"
                        Slot backSlot = self.slots.get(0);
                        if (backSlot.hasStack()) {
                            var backName = backSlot.getStack().get(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
                            if (backName != null && backName.getString().contains("Back")) {
                                // Strip §b§l prefix to get category display name
                                String displayName = catNameStr.replaceAll("§[0-9a-fk-or]", "");
                                String screenTitle = net.maxello.knowledgebound.config.ConfigGuiHandler.CATEGORY_TITLE_PREFIX + displayName;
                                net.maxello.knowledgebound.config.ConfigGuiHandler.handleClick(
                                        serverPlayer, screenTitle,
                                        slotIndex, button, actionType, self);
                                ci.cancel();
                                return;
                            }
                        }
                    }
                }

                // Knowledge progress GUI (read-only)
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

        // This is a crucial step! We need to take a snapshot of whatever item the player is holding with their cursor
        // BEFORE the vanilla game processes this click.
        // We stash it in a static ThreadLocal variable inside KnowledgeEvents. 
        // Why not here? Because mixins get injected directly into the target class, so adding private static fields
        // can sometimes get messy with cross-mod compatibility or classloading.
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
        // Supervised jobs (like smelting iron) require the player to manually extract the items
        // while the game checks if they were actually standing nearby the whole time.
        if (self instanceof AbstractFurnaceScreenHandler) {
            // Slot 2 is the output slot for standard furnaces, smokers, and blast furnaces.
            if (slotIndex == 2) {
                Slot outputSlot = self.slots.get(2);
                if (outputSlot.hasStack() && !outputSlot.getStack().isEmpty()) {
                    if (serverPlayer.getWorld() instanceof ServerWorld serverWorld) {
                        // Let's try to locate the exact furnace block the player is interacting with.
                        BlockPos furnacePos = SupervisedJobManager.findFurnacePosForPlayer(serverPlayer);
                        if (furnacePos != null) {
                            // Now we ask the manager: "Did they supervise this job properly?"
                            boolean allowed = SupervisedJobManager.onItemCollected(
                                    serverPlayer, serverWorld, furnacePos);
                            if (!allowed) {
                                // They failed! They walked away or did something else.
                                // The punishment is harsh: the item is consumed and they get nothing.
                                outputSlot.setStack(ItemStack.EMPTY);
                                self.sendContentUpdates();
                                ci.cancel();
                                return;
                            }
                            // If allowed is true, we do nothing and let vanilla handle picking up the item normally.
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
                        net.maxello.knowledgebound.config.KnowledgeBoundConfig cfg = net.maxello.knowledgebound.config.KnowledgeBoundConfig.INSTANCE;
                        boolean jobEnabled = jt == SupervisedJob.JobType.SMELTING ? cfg.smeltingEnabled : cfg.cookingEnabled;
                        if (!jobEnabled) return;
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
                        net.maxello.knowledgebound.config.KnowledgeBoundConfig cfg = net.maxello.knowledgebound.config.KnowledgeBoundConfig.INSTANCE;
                        boolean jobEnabled = jt == SupervisedJob.JobType.SMELTING ? cfg.smeltingEnabled : cfg.cookingEnabled;
                        if (!jobEnabled) return;
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

        // We need to apply our custom crafting knowledge rules (like fail chances or poor quality output)
        // BEFORE vanilla actually transfers the item into the player's inventory.
        ItemStack stack = slot.getStack();
        Identifier itemId = Identifier.of(net.maxello.knowledgebound.util.KbIdHelper.getKbId(stack));

        KnowledgeBound.LOGGER.debug("[KB] Shift-click craft intercepted: {}", itemId);

        // This flag tells the crafting listeners down the line not to double-roll for this same craft.
        KnowledgeEvents.SKIP_NEXT_ROLL.set(true);
        ItemStack modified;
        try {
            // Ask the events system what the final outcome of this craft should be.
            modified = KnowledgeEvents.handleCrafting(
                    serverPlayer,
                    itemId,
                    stack.copy()
            );
        } finally {
            // Notice we do NOT clear the SKIP_NEXT_ROLL flag here! 
            // We need to leave it as true because the impending vanilla slot.onTakeItem call 
            // will check it to avoid running the crafting logic a second time.
            // It will be cleared inside onTakeItem later.
        }

        if (modified == null || modified == stack) {
            // No custom knowledge rule applied to this item. Let vanilla handle it exactly as normal.
            // We do have to clear the flag here though, so we don't accidentally skip the next real roll.
            KnowledgeEvents.SKIP_NEXT_ROLL.set(false);
            return;
        }

        if (modified.isEmpty()) {
            // The craft failed entirely! The player messed up.
            // We consume the ingredients, log it, and completely cancel the shift-click.
            KnowledgeBound.LOGGER.debug("[KB] Shift-click craft FAILED for {}", itemId);
            slot.setStack(ItemStack.EMPTY);
            slot.onTakeItem(player, stack);
            self.sendContentUpdates();
            ci.cancel();
        } else {
            // The craft succeeded, but it was poor quality! 
            // We overwrite the item sitting in the result slot with our damaged/modified version
            // right before vanilla swoops in to transfer it to their inventory.
            KnowledgeBound.LOGGER.debug("[KB] Shift-click craft POOR for {}, dmg={}", itemId, modified.getDamage());
            slot.setStack(modified);
            // Now we just let vanilla continue and finish the transfer.
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


