package net.maxello.knowledgebound.mixin;

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

    // In 1.20.4 this is "final Property levelCost;"
    @Shadow @Final private Property levelCost;

    /**
     * After vanilla computes the anvil result, force the level cost to 0.
     * This fixes the displayed cost and makes sure any XP subtraction is 0.
     */
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void knowledgebound$zeroAnvilCost(CallbackInfo ci) {
        this.levelCost.set(0);
    }

    /**
     * Remove the XP *requirement* from the "can I take the output?" check.
     * If there is an actual output present, we allow taking it regardless of levels.
     */
    @Inject(method = "canTakeOutput", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$alwaysCanTakeOutput(PlayerEntity player,
                                                    boolean present,
                                                    CallbackInfoReturnable<Boolean> cir) {
        // "present" is vanilla's "is there an item we can take?"
        if (present) {
            cir.setReturnValue(true);
        }
    }
}
