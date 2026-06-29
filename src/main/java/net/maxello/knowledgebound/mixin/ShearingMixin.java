package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.*;
import net.minecraft.entity.passive.SheepEntity;
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
 * Gates shearing of sheep behind Husbandry tier + fail chance.
 */
@Mixin(SheepEntity.class)
public abstract class ShearingMixin {

    private static final Random RANDOM = new Random();

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onShear(PlayerEntity player, Hand hand,
                                         CallbackInfoReturnable<ActionResult> cir) {
        if (player.getWorld().isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.husbandryEnabled || !cfg.husbandryShearingEnabled) return;

        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Items.SHEARS)) return;

        SheepEntity self = (SheepEntity) (Object) this;
        if (self.isBaby() || self.isSheared()) return;

        int requiredTier = AnimalTierRegistry.getRequiredTier(self);
        int playerTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);

        // Tier too low — hard block
        if (playerTier < requiredTier) {
            String msg = cfg.messages.husbandryShearingTierLow
                    .replace("{minTier}", String.valueOf(requiredTier));
            serverPlayer.sendMessage(Text.literal(msg), true);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // Fail chance roll
        double failChance = cfg.husbandryShearingFail.getForTier(playerTier);
        if (RANDOM.nextDouble() < failChance) {
            serverPlayer.sendMessage(Text.literal(cfg.messages.husbandryShearingFail), true);

            if (cfg.husbandryShearingDamageShearsOnFail) {
                stack.damage(1, serverPlayer, net.minecraft.entity.EquipmentSlot.MAINHAND);
            }

            PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // Success — grant XP, let vanilla handle shearing
        PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
    }
}
