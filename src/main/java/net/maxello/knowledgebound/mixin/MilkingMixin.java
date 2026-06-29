package net.maxello.knowledgebound.mixin;

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
        if (player.getWorld().isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.husbandryEnabled || !cfg.husbandryMilkingEnabled) return;

        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Items.BUCKET)) return;

        AnimalEntity self = (AnimalEntity) (Object) this;
        if (self.isBaby()) return;

        int requiredTier = AnimalTierRegistry.getRequiredTier(self);
        int playerTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);

        // Tier too low — hard block
        if (playerTier < requiredTier) {
            String msg = cfg.messages.husbandryMilkingTierLow
                    .replace("{minTier}", String.valueOf(requiredTier));
            serverPlayer.sendMessage(Text.literal(msg), true);
            cir.setReturnValue(ActionResult.SUCCESS);
            // Sync inventory to fix ghost milk bucket on client
            serverPlayer.currentScreenHandler.syncState();
            return;
        }

        // Fail chance roll
        double failChance = cfg.husbandryMilkingFail.getForTier(playerTier);
        if (RANDOM.nextDouble() < failChance) {
            serverPlayer.sendMessage(Text.literal(cfg.messages.husbandryMilkingFail), true);

            if (cfg.husbandryMilkingConsumeBucketOnFail && !serverPlayer.isCreative()) {
                stack.decrement(1);
            }

            PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
            cir.setReturnValue(ActionResult.SUCCESS);
            // Sync inventory to fix ghost milk bucket on client
            serverPlayer.currentScreenHandler.syncState();
            return;
        }

        // Success — grant XP, let vanilla handle milking
        PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
    }
}
