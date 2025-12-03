package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.KnowledgeEvents;
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

@Mixin(CraftingResultSlot.class)
public abstract class CraftingResultSlotMixin extends Slot {

    public CraftingResultSlotMixin(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    /**
     * Target: void onTakeItem(PlayerEntity player, ItemStack stack)
     * Runs for BOTH normal clicks and shift-clicks.
     */
    @Inject(method = "onTakeItem", at = @At("HEAD"))
    private void knowledgebound$onTakeItem(PlayerEntity player,
                                           ItemStack stack,
                                           CallbackInfo ci) {

        KnowledgeBound.LOGGER.debug("[KB MIXIN] onTakeItem fired. Player={}, stack={}",
                player.getName().getString(),
                stack);

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            KnowledgeBound.LOGGER.debug("[KB MIXIN] Not a ServerPlayerEntity, skipping.");
            return;
        }

        if (stack.isEmpty()) {
            KnowledgeBound.LOGGER.debug("[KB MIXIN] Stack empty, skipping.");
            return;
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        KnowledgeBound.LOGGER.debug("[KB MIXIN] Item id = {}", itemId);

        ItemStack modified = KnowledgeEvents.handleCrafting(
                serverPlayer,
                itemId,
                stack.copy()
        );

        if (modified == null) {
            KnowledgeBound.LOGGER.debug("[KB MIXIN] Modified is null, leaving original.");
            return;
        }

        if (modified == stack) {
            KnowledgeBound.LOGGER.debug("[KB MIXIN] Modified == original (no rule?), leaving original.");
            return;
        }

        if (modified.isEmpty()) {
            KnowledgeBound.LOGGER.debug("[KB MIXIN] Modified is EMPTY, clearing stack.");
            stack.setCount(0);
        } else {
            KnowledgeBound.LOGGER.debug("[KB MIXIN] Applying modified stack: dmg={}, count={}",
                    modified.getDamage(), modified.getCount());
            stack.setCount(modified.getCount());
            stack.setDamage(modified.getDamage());
        }
    }
}
