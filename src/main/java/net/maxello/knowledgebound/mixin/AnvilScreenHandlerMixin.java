package net.maxello.knowledgebound.mixin;

import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    // In 1.20.4 this is "final Property levelCost"
    @Shadow @Final private Property levelCost;

    /**
     * After the anvil computes its result, force the XP level cost to 0.
     * This removes the XP requirement and prevents vanilla from charging levels.
     */
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void knowledgebound$zeroAnvilCost(CallbackInfo ci) {
        this.levelCost.set(0);
    }
}
