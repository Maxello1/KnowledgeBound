package net.maxello.knowledgebound.mixin;
import net.maxello.knowledgebound.util.KbIdHelper;
import net.maxello.knowledgebound.mechanics.gathering.KnowledgeEvents;

import net.maxello.knowledgebound.KnowledgeBound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * We need to intercept the exact moment a player pulls a crafted item out of the crafting table.
 * If they are trying to craft something way above their skill tier, we want to swap it out 
 * at the last second for some garbage (like dirt, or a severely damaged version).
 */
@Mixin(CraftingResultSlot.class)
public abstract class CraftingResultSlotMixin extends Slot {

    public CraftingResultSlotMixin(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    /**
     * Target: void onTakeItem(PlayerEntity player, ItemStack stack)
     * This fires when they actually click to grab the item from the result slot.
     */
    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void knowledgebound$onTakeItem(PlayerEntity player,
                                           ItemStack stack,
                                           CallbackInfo ci) {

        KnowledgeBound.LOGGER.debug("[KB MIXIN] onTakeItem fired. Player={}, stack={}",
                player.getName().getString(),
                stack);

        // Always stick to the server side logic
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            KnowledgeBound.LOGGER.debug("[KB MIXIN] Not a ServerPlayerEntity, skipping.");
            return;
        }

        if (stack.isEmpty()) {
            KnowledgeBound.LOGGER.debug("[KB MIXIN] Stack empty, skipping.");
            return;
        }

        // Shift-clicking in Minecraft is an absolute nightmare to intercept properly.
        // It actually fires multiple events in rapid succession. We use this flag to 
        // make sure we don't accidentally penalize them twice for the exact same click.
        if (KnowledgeEvents.SKIP_NEXT_ROLL.get()) {
            KnowledgeBound.LOGGER.debug("[KB MIXIN] SKIP_NEXT_ROLL is true. Skipping this roll to prevent double-fail on shift-click.");
            KnowledgeEvents.SKIP_NEXT_ROLL.set(false);
            return;
        }

        Identifier itemId = Identifier.of(net.maxello.knowledgebound.util.KbIdHelper.getKbId(stack));
        KnowledgeBound.LOGGER.debug("[KB MIXIN] Item id = {}", itemId);

        // Send the item to our event handler to see if their tier is high enough.
        // If they fail, it'll return a modified stack (like dirt, or empty).
        ItemStack modified = KnowledgeEvents.handleCrafting(
                serverPlayer,
                itemId,
                stack.copy()
        );

        // If it returns null or the exact same stack, they passed the check. We do nothing.
        if (modified == null) {
            KnowledgeBound.LOGGER.debug("[KB MIXIN] Modified is null, leaving original.");
            return;
        }

        if (modified == stack) {
            KnowledgeBound.LOGGER.debug("[KB MIXIN] Modified == original (no rule?), leaving original.");
            return;
        }

        // ── The Sneaky Cursor Swap ──
        // This part is tricky. By the time `onTakeItem` fires, vanilla has *already* moved the 
        // beautifully crafted item onto the player's mouse cursor. If we just modify the `stack` variable,
        // it doesn't do anything because it's too late. 
        // We have to grab what their cursor looked like BEFORE the click (which we saved in another mixin),
        // and manually force the modified item onto their cursor.
        ItemStack preClickCursor = KnowledgeEvents.PRE_CLICK_CURSOR.get();
        if (preClickCursor == null) preClickCursor = ItemStack.EMPTY;

        if (modified.isEmpty()) {
            // They failed so badly that the item was just destroyed.
            KnowledgeBound.LOGGER.debug("[KB MIXIN] Modified is EMPTY, clearing stack.");
            stack.setCount(0); // clear the slot just in case
            // Put their cursor back exactly how it was before they clicked, as if nothing happened.
            serverPlayer.currentScreenHandler.setCursorStack(preClickCursor);
        } else {
            // They got a downgraded item. We apply the changes directly to the slot stack,
            KnowledgeBound.LOGGER.debug("[KB MIXIN] Applying modified stack: dmg={}, count={}",
                    modified.getDamage(), modified.getCount());
            stack.setCount(modified.getCount());
            stack.setDamage(modified.getDamage());
            
            // And then if their mouse cursor was empty when they clicked, we place the garbage item right on it.
            if (preClickCursor.isEmpty()) {
                serverPlayer.currentScreenHandler.setCursorStack(modified);
            }
        }

        // Because we're doing a bunch of shady stuff behind the scenes by modifying the cursor 
        // and the slots directly, the client (the player's screen) will probably desync and show the wrong items.
        // We force the server to shout at the client to refresh the whole crafting UI so it looks right.
        serverPlayer.currentScreenHandler.sendContentUpdates();
    }
}


