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
 * We hook into AnimalEntity here so we can gate breeding and taming behind the player's Husbandry tier.
 * We want to make sure the player has actually earned the right to tame/breed certain animals,
 * and we also want to sprinkle in a little randomness so sometimes they fail and just waste the item.
 */
@Mixin(AnimalEntity.class)
public abstract class AnimalInteractionMixin {

    private static final Random RANDOM = new Random();

    // We inject at HEAD because we want to intercept the interaction before vanilla gets a chance
    // to actually tame or breed the animal. If we wait too long, it's already too late to block it!
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onInteractMob(PlayerEntity player, Hand hand,
                                               CallbackInfoReturnable<ActionResult> cir) {
        // As always, we only care about the server side. The client will figure it out.
        if (player.getWorld().isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.husbandryEnabled) return;

        AnimalEntity self = (AnimalEntity) (Object) this;
        ItemStack stack = player.getStackInHand(hand);

        // ── Taming Check ──
        // We do this first because taming takes priority. You have to tame a wolf before you can breed it.
        // We only care if the animal is actually tameable and hasn't been tamed yet.
        if (cfg.husbandryTamingEnabled && self instanceof TameableEntity tameable && !tameable.isTamed()) {
            boolean isTamingItem = false;
            
            // Hardcode the vanilla taming items for some of the common pets.
            // If they are holding the right item, they are definitely trying to tame it.
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
                // Let's see if the player is experienced enough to tame this beast.
                int requiredTier = AnimalTierRegistry.getRequiredTier(self);
                int playerTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);

                // If their tier is too low, we hard block it. They can't even try.
                if (playerTier < requiredTier) {
                    String msg = cfg.messages.husbandryTamingTierLow
                            .replace("{minTier}", String.valueOf(requiredTier));
                    serverPlayer.sendMessage(Text.literal(msg), true);

                    // Depending on the config, we might punish them by eating the item anyway.
                    if (cfg.husbandryTamingConsumeItemOnFail && !serverPlayer.isCreative()) {
                        stack.decrement(1);
                    }

                    cir.setReturnValue(ActionResult.SUCCESS);
                    return;
                }

                // If they are high enough tier, they still have to pass a random fail check. 
                // Taming wild animals isn't an exact science!
                double failChance = cfg.husbandryTamingFail.getForTier(playerTier);
                if (RANDOM.nextDouble() < failChance) {
                    // They failed the check. Tell them they messed up.
                    serverPlayer.sendMessage(Text.literal(cfg.messages.husbandryTamingFail), true);

                    // Again, we might eat their item just to rub salt in the wound.
                    if (cfg.husbandryTamingConsumeItemOnFail && !serverPlayer.isCreative()) {
                        stack.decrement(1);
                    }

                    // Even though they failed, they learn from their mistakes. Grant them some XP!
                    PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
                    cir.setReturnValue(ActionResult.SUCCESS);
                    return;
                }

                // If they passed both the tier check and the fail chance, we just step back and 
                // let vanilla handle the actual taming logic. But we still give them their XP!
                PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
                return; // don't cancel, let vanilla take over!
            }
        }

        // ── Breeding Check ──
        // If they aren't taming, maybe they're trying to make some babies.
        if (!cfg.husbandryBreedingEnabled) return;
        // Make sure the item they are holding is actually the right food for this animal.
        if (!self.isBreedingItem(stack)) return;
        // Babies obviously can't breed.
        if (self.isBaby()) return;
        // If they already have hearts floating over them, they're already ready to go, no need to check again.
        if (self.getLoveTicks() > 0) return; 

        int requiredTier = AnimalTierRegistry.getRequiredTier(self);
        int playerTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);

        // Same deal as taming — if they aren't a high enough tier, we cut them off right away.
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

        // The ol' random fail chance. Feeding animals doesn't always put them in the mood!
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

        // Success! We grant them their Husbandry XP, and then we let vanilla put the animal 
        // into love mode. The actual breeding cooldown happens later.
        PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
        // Don't cancel — let vanilla handle entering love mode
    }

    /**
     * This fires AFTER the animals have successfully bumped uglies and a baby popped out.
     * Vanilla usually sets a flat 5-minute (6000 tick) cooldown before they can breed again.
     * We're hijacking that cooldown so we can scale it based on how high tier the animal is.
     * High tier animals take way longer to breed again!
     */
    @Inject(method = "breed", at = @At("TAIL"))
    private void knowledgebound$onBreed(ServerWorld world, AnimalEntity other, CallbackInfo ci) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.husbandryEnabled || !cfg.husbandryBreedingCooldownEnabled) return;

        AnimalEntity self = (AnimalEntity) (Object) this;

        // Apply our custom cooldown to the first parent based on its tier
        int selfTier = AnimalTierRegistry.getRequiredTier(self);
        int selfCooldownSec = selfTier < cfg.husbandryBreedingCooldownSeconds.length
                ? cfg.husbandryBreedingCooldownSeconds[selfTier]
                : 300; // Fallback to 5 minutes just in case
        
        // Breeding age is in ticks, so we multiply seconds by 20. Positive values mean it's an adult on cooldown.
        self.setBreedingAge(selfCooldownSec * 20);

        // Don't forget the other parent! They need the exact same cooldown applied.
        int otherTier = AnimalTierRegistry.getRequiredTier(other);
        int otherCooldownSec = otherTier < cfg.husbandryBreedingCooldownSeconds.length
                ? cfg.husbandryBreedingCooldownSeconds[otherTier]
                : 300;
        other.setBreedingAge(otherCooldownSec * 20);
    }
}


