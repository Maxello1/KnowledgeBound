package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.mechanics.animals.AnimalTierRegistry;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;

import net.maxello.knowledgebound.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Gates milking of cows and goats behind Husbandry tier + fail chance.
 * Targets both CowEntity and GoatEntity which both handle milking in interactMob.
 */
@Mixin({CowEntity.class, GoatEntity.class})
public abstract class MilkingMixin {

    private static final Random RANDOM = new Random();

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onMilk(PlayerEntity player, Hand hand,
                                        CallbackInfoReturnable<ActionResult> cir) {
        // First off, we only process this on the server side where the logic actually runs.
        if (player.getWorld().isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        // Check if the Husbandry and milking mechanics are even enabled in the config.
        // If not, just let them milk away normally.
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.husbandryEnabled || !cfg.husbandryMilkingEnabled) return;

        // Ensure the player is actually trying to milk the animal by holding a bucket.
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Items.BUCKET)) return;

        // You can't milk babies, so just ignore that and let vanilla handle (or block) it.
        AnimalEntity self = (AnimalEntity) (Object) this;
        if (self.isBaby()) return;

        // Grab the tier needed to interact with this specific animal (a cow might be easier than a goat).
        int requiredTier = AnimalTierRegistry.getRequiredTier(self);
        int playerTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);

        // If their tier is too low to even try milking this animal...
        if (playerTier < requiredTier) {
            // We tell them straight up that they lack the skills.
            String msg = cfg.messages.husbandryMilkingTierLow
                    .replace("{minTier}", String.valueOf(requiredTier));
            serverPlayer.sendMessage(Text.literal(msg), true);
            
            // We cancel the interaction so vanilla doesn't take over.
            cir.setReturnValue(ActionResult.SUCCESS);
            
            // This sync is super important! Sometimes the client tries to be smart and predicts
            // the bucket turning into a milk bucket. This forces the client to realize it's still empty.
            serverPlayer.currentScreenHandler.syncState();
            return;
        }

        // Okay, their tier is high enough, but milking isn't always easy! Let's roll for failure.
        double failChance = cfg.husbandryMilkingFail.getForTier(playerTier);
        if (RANDOM.nextDouble() < failChance) {
            // Whoops, the animal kicked the bucket or wouldn't cooperate. Let the player know.
            serverPlayer.sendMessage(Text.literal(cfg.messages.husbandryMilkingFail), true);

            // If the config says we should punish them by losing the bucket (maybe the animal kicked it away),
            // we decrement the stack, unless they are in creative mode.
            if (cfg.husbandryMilkingConsumeBucketOnFail && !serverPlayer.isCreative()) {
                stack.decrement(1);
            }

            // Hey, we still grant a little bit of knowledge because you learn from your mistakes!
            PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
            
            // Cancel the vanilla interaction.
            cir.setReturnValue(ActionResult.SUCCESS);
            // Sync inventory again to stop ghost milk buckets.
            serverPlayer.currentScreenHandler.syncState();
            return;
        }

        // If they got past the tier check and didn't fail the random roll... success!
        // We give them their knowledge XP and let the rest of the vanilla interaction play out, 
        // which will handle actually giving them the milk bucket.
        PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
    }
}


