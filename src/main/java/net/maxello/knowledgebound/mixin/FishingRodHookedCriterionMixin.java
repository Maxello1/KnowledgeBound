package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.util.KbIdHelper;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;

import net.minecraft.advancement.criterion.FishingRodHookedCriterion;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

        // 1 "minute" of Fishing knowledge per successful reel (even if the fish gets away, you learn from the attempt!)
        PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.FISHING_ID);

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        int tier = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.FISHING_ID);
        double failChance = 0.0;
        if (cfg.fishingFailChancePerTier != null && cfg.fishingFailChancePerTier.length > 0) {
            int idx = Math.min(tier, cfg.fishingFailChancePerTier.length - 1);
            failChance = cfg.fishingFailChancePerTier[idx];
        }

        if (failChance > 0) {
            // Check if using good_rod or super_rod to reduce fail chance
            if (rod != null && !rod.isEmpty()) {
                String rodId = KbIdHelper.getKbId(rod);
                if (rodId.equals("knowledgebound:good_rod") || rodId.equals("minecraft:good_rod") || rodId.equals("good_rod")) {
                    failChance *= cfg.fishingGoodRodFailReduction;
                } else if (rodId.equals("knowledgebound:super_rod") || rodId.equals("minecraft:super_rod") || rodId.equals("super_rod")) {
                    failChance *= cfg.fishingSuperRodFailReduction;
                }
            }

            if (player.getRandom().nextDouble() < failChance) {
                try {
                    fishingLoots.clear();
                    player.sendMessage(Text.literal("The fish got away...").formatted(Formatting.RED), true);
                } catch (Exception e) {
                    // Fallback if collection cannot be cleared
                }
            }
        }
    }
}


