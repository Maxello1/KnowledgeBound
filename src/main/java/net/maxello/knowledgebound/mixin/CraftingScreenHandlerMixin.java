package net.maxello.knowledgebound.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingScreenHandler.class)
public abstract class CraftingScreenHandlerMixin {

    /**
     * Disable shift-click crafting from the result slot so KnowledgeBound's
     * CraftingResultSlot mixin can't be bypassed.
     */
    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$disableShiftClickOnResult(PlayerEntity player,
                                                          int slotIndex,
                                                          CallbackInfoReturnable<ItemStack> cir) {
        // In vanilla CraftingScreenHandler the crafting result is always slot 0
        if (slotIndex == 0) {
            // Just do nothing and say "no items moved"
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
