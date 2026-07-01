package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;

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
     * Disable shift-click crafting from the result slot entirely.
     * Why? Because trying to gracefully intercept and swap items during a shift-click
     * rapid-fire crafting session is an absolute nightmare that leads to item duplication glitches 
     * and crazy server desyncs. 
     * So, we just say "No". If they want to craft something, they have to manually pull it 
     * out of the slot so our other mixins can safely run the tier checks.
     */
    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$disableShiftClickOnResult(PlayerEntity player,
                                                          int slotIndex,
                                                          CallbackInfoReturnable<ItemStack> cir) {
        // In the vanilla CraftingScreenHandler, the actual output slot where the result sits is always slot 0.
        if (slotIndex == 0) {
            // By returning an empty item stack here, we basically tell the server 
            // "Nope, nothing moved, ignore the shift-click."
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}

