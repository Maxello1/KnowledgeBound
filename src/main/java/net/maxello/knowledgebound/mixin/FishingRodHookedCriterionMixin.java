package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeRegistry;
import net.maxello.knowledgebound.PlayerKnowledgeManager;

import net.minecraft.advancement.criterion.FishingRodHookedCriterion;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(FishingRodHookedCriterion.class)
public abstract class FishingRodHookedCriterionMixin {

    @Inject(
            method = "trigger(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/projectile/FishingBobberEntity;Ljava/util/Collection;)V",
            at = @At("HEAD")
    )
    private void knowledgebound$onFishingTrigger(
            ServerPlayerEntity player,
            ItemStack rod,
            FishingBobberEntity bobber,
            Collection<ItemStack> fishingLoots,
            CallbackInfo ci
    ) {
        // Only count *successful* catches (vanilla passes loot here)
        if (fishingLoots == null || fishingLoots.isEmpty()) {
            return;
        }

        // 1 "minute" of Fishing knowledge per successful reel
        PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.FISHING_ID);
    }
}
