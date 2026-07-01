package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.mechanics.animals.AnimalTierRegistry;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;

import net.maxello.knowledgebound.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Hooks into AnimalEntity to gate breeding and taming behind Husbandry tier.
 *
 * interactMob (HEAD): Tier gate + fail chance for breeding and taming.
 * breed (TAIL): Custom breeding cooldown applied AFTER animals successfully mate.
 */
@Mixin(AnimalEntity.class)
public abstract class AnimalInteractionMixin {

    private static final Random RANDOM = new Random();

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onInteractMob(PlayerEntity player, Hand hand,
                                               CallbackInfoReturnable<ActionResult> cir) {
        if (player.getWorld().isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.husbandryEnabled) return;

        AnimalEntity self = (AnimalEntity) (Object) this;
        ItemStack stack = player.getStackInHand(hand);

        // --- Taming check (runs first, before breeding) ---
        if (cfg.husbandryTamingEnabled && self instanceof TameableEntity tameable && !tameable.isTamed()) {
            boolean isTamingItem = false;
            if (self instanceof WolfEntity) {
                isTamingItem = stack.isOf(Items.BONE);
            } else if (self instanceof CatEntity) {
                isTamingItem = stack.isOf(Items.COD) || stack.isOf(Items.SALMON);
            } else if (self instanceof ParrotEntity) {
                isTamingItem = stack.isOf(Items.WHEAT_SEEDS) || stack.isOf(Items.MELON_SEEDS)
                        || stack.isOf(Items.PUMPKIN_SEEDS) || stack.isOf(Items.BEETROOT_SEEDS)
                        || stack.isOf(Items.TORCHFLOWER_SEEDS) || stack.isOf(Items.PITCHER_POD);
            }

            if (isTamingItem) {
                int requiredTier = AnimalTierRegistry.getRequiredTier(self);
                int playerTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);

                // Tier too low — hard block
                if (playerTier < requiredTier) {
                    String msg = cfg.messages.husbandryTamingTierLow
                            .replace("{minTier}", String.valueOf(requiredTier));
                    serverPlayer.sendMessage(Text.literal(msg), true);

                    if (cfg.husbandryTamingConsumeItemOnFail && !serverPlayer.isCreative()) {
                        stack.decrement(1);
                    }

                    cir.setReturnValue(ActionResult.SUCCESS);
                    return;
                }

                // Fail chance roll
                double failChance = cfg.husbandryTamingFail.getForTier(playerTier);
                if (RANDOM.nextDouble() < failChance) {
                    serverPlayer.sendMessage(Text.literal(cfg.messages.husbandryTamingFail), true);

                    if (cfg.husbandryTamingConsumeItemOnFail && !serverPlayer.isCreative()) {
                        stack.decrement(1);
                    }

                    PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
                    cir.setReturnValue(ActionResult.SUCCESS);
                    return;
                }

                // Success — grant XP, let vanilla handle taming
                PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
                return; // don't cancel
            }
        }

        // --- Breeding check ---
        if (!cfg.husbandryBreedingEnabled) return;
        if (!self.isBreedingItem(stack)) return;
        if (self.isBaby()) return;
        if (self.getLoveTicks() > 0) return; // already in love mode

        int requiredTier = AnimalTierRegistry.getRequiredTier(self);
        int playerTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);

        // Tier too low — hard block
        if (playerTier < requiredTier) {
            String msg = cfg.messages.husbandryBreedingTierLow
                    .replace("{minTier}", String.valueOf(requiredTier));
            serverPlayer.sendMessage(Text.literal(msg), true);

            if (cfg.husbandryBreedingConsumeItemOnFail && !serverPlayer.isCreative()) {
                stack.decrement(1);
            }

            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // Fail chance roll
        double failChance = cfg.husbandryBreedingFail.getForTier(playerTier);
        if (RANDOM.nextDouble() < failChance) {
            serverPlayer.sendMessage(Text.literal(cfg.messages.husbandryBreedingFail), true);

            if (cfg.husbandryBreedingConsumeItemOnFail && !serverPlayer.isCreative()) {
                stack.decrement(1);
            }

            PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // Success — grant XP, let vanilla handle the breeding
        // Cooldown is applied in the breed() TAIL hook below, not here
        PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);

        // Don't cancel — let vanilla handle entering love mode
    }

    /**
     * After animals successfully breed, override the vanilla 6000-tick (5 min) cooldown
     * with our custom per-tier cooldown. This runs AFTER the baby is spawned and
     * loveTicks are already reset, so it doesn't interfere with love mode.
     */
    @Inject(method = "breed", at = @At("TAIL"))
    private void knowledgebound$onBreed(ServerWorld world, AnimalEntity other, CallbackInfo ci) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.husbandryEnabled || !cfg.husbandryBreedingCooldownEnabled) return;

        AnimalEntity self = (AnimalEntity) (Object) this;

        // Apply custom cooldown to this parent
        int selfTier = AnimalTierRegistry.getRequiredTier(self);
        int selfCooldownSec = selfTier < cfg.husbandryBreedingCooldownSeconds.length
                ? cfg.husbandryBreedingCooldownSeconds[selfTier]
                : 300;
        self.setBreedingAge(selfCooldownSec * 20);

        // Apply custom cooldown to the other parent
        int otherTier = AnimalTierRegistry.getRequiredTier(other);
        int otherCooldownSec = otherTier < cfg.husbandryBreedingCooldownSeconds.length
                ? cfg.husbandryBreedingCooldownSeconds[otherTier]
                : 300;
        other.setBreedingAge(otherCooldownSec * 20);
    }
}


