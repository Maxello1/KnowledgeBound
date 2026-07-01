package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    // This is how we interact with the anvil's internal level cost tracker. 
    // In 1.20.4 this is exactly "final Property levelCost;", so we shadow it here to gain access.
    @Shadow @Final private Property levelCost;

    /**
     * We're making anvils entirely free! 
     * We let vanilla do all the heavy lifting to figure out if an anvil recipe is valid,
     * but right at the end (the TAIL), we swoop in and just overwrite the calculated cost to 0.
     * This updates the UI so it doesn't show an XP cost, and makes sure when they pull the item out,
     * it doesn't subtract any levels from them.
     */
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void knowledgebound$zeroAnvilCost(CallbackInfo ci) {
        // Boom. Free anvil uses.
        this.levelCost.set(0);
    }

    /**
     * Vanilla has a check where it literally won't let you pick up the result item 
     * if you don't have enough levels to cover the `levelCost`. 
     * Since our anvils are free, we need to bypass that entirely. 
     * 
     * We inject right at the HEAD of the canTakeOutput method. If there is a valid item sitting 
     * in the output slot (which vanilla calls "present"), we just say "Yep, they can absolutely take it"
     * and short-circuit the rest of the checks.
     */
    @Inject(method = "canTakeOutput", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$alwaysCanTakeOutput(PlayerEntity player,
                                                    boolean present,
                                                    CallbackInfoReturnable<Boolean> cir) {
        // "present" just means there is actually an item generated in the output slot.
        // If there is, we instantly return true, ignoring their actual XP level.
        if (present) {
            cir.setReturnValue(true);
        }
    }
}

