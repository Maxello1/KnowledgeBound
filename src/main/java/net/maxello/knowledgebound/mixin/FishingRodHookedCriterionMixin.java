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
        // First off, we only want to count *successful* catches.
        // Vanilla passes the generated loot into this trigger method, so if the collection is null or empty, 
        // it means the player reeled in nothing (or just casted their line), so we don't do anything.
        if (fishingLoots == null || fishingLoots.isEmpty()) {
            return;
        }

        // The player actually hooked something! Let's give them some knowledge.
        // We grant 1 "minute" of Fishing knowledge per successful reel. 
        // Notice we do this *before* checking the fail chance — even if the fish gets away, you still learn from the attempt!
        PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.FISHING_ID);

        // Now let's see if the player's knowledge tier is high enough to actually keep the catch.
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        int tier = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.FISHING_ID);
        double failChance = 0.0;
        
        // Grab the fail chance based on the player's current Fishing tier.
        if (cfg.fishingFailChancePerTier != null && cfg.fishingFailChancePerTier.length > 0) {
            // Make sure we don't go out of bounds if their tier is higher than the config array length
            int idx = Math.min(tier, cfg.fishingFailChancePerTier.length - 1);
            failChance = cfg.fishingFailChancePerTier[idx];
        }

        // If there's a chance they might fail...
        if (failChance > 0) {
            // Let's see if they're using a better rod to offset their lack of skill.
            if (rod != null && !rod.isEmpty()) {
                String rodId = KbIdHelper.getKbId(rod);
                // The Good Rod and Super Rod will multiply the fail chance by a reduction factor.
                // We check a few variations of the ID just in case the namespace is omitted or different.
                if (rodId.equals("knowledgebound:good_rod") || rodId.equals("minecraft:good_rod") || rodId.equals("good_rod")) {
                    failChance *= cfg.fishingGoodRodFailReduction;
                } else if (rodId.equals("knowledgebound:super_rod") || rodId.equals("minecraft:super_rod") || rodId.equals("super_rod")) {
                    failChance *= cfg.fishingSuperRodFailReduction;
                }
            }

            // Roll the dice! Are they going to lose the fish?
            if (player.getRandom().nextDouble() < failChance) {
                try {
                    // Oops, they failed! We wipe out the loot so they get absolutely nothing.
                    fishingLoots.clear();
                    // Let them know what happened so they don't think the game is just bugging out.
                    player.sendMessage(Text.literal("The fish got away...").formatted(Formatting.RED), true);
                } catch (Exception e) {
                    // Just a safety fallback in case some other mod passes an immutable collection here.
                    // If we can't clear it, they get lucky and keep the fish.
                }
            }
        }
    }
}


