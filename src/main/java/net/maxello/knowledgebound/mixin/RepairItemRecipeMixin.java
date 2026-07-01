package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;

import net.minecraft.recipe.RepairItemRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public class RepairItemRecipeMixin {
    /**
     * Vanilla Minecraft lets you repair tools by smashing two broken ones together in your crafting grid.
     * We don't want players doing that! It bypasses all of our custom repair mechanics and durability rules.
     * So, we just completely disable this recipe.
     */
    @Inject(method = "matches(Lnet/minecraft/recipe/input/CraftingRecipeInput;Lnet/minecraft/world/World;)Z", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$disableRepairInCraftingGrid(CraftingRecipeInput craftingRecipeInput, World world, CallbackInfoReturnable<Boolean> cir) {
        // By setting the return value to false right away, the game thinks this recipe never matches,
        // no matter what items the player puts in the crafting grid. Easy fix!
        cir.setReturnValue(false);
    }
}

