package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.mechanics.animals.AnimalTierRegistry;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;

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
        // Run all this on the server side where the real logic lives.
        if (player.getWorld().isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        // Check if our mod's husbandry rules are actually turned on right now.
        if (!cfg.husbandryEnabled || !cfg.husbandryShearingEnabled) return;

        // Ensure the player is holding shears before we bother checking their skill.
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Items.SHEARS)) return;

        SheepEntity self = (SheepEntity) (Object) this;
        // If the sheep is already naked or is a baby, let vanilla handle it (it won't do anything).
        if (self.isBaby() || self.isSheared()) return;

        int requiredTier = AnimalTierRegistry.getRequiredTier(self);
        int playerTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);

        // If the player's husbandry level isn't high enough...
        if (playerTier < requiredTier) {
            String msg = cfg.messages.husbandryShearingTierLow
                    .replace("{minTier}", String.valueOf(requiredTier));
            serverPlayer.sendMessage(Text.literal(msg), true);
            // Cancel the vanilla interaction completely so they can't shear it.
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // Now for the fun part: rolling the dice to see if they mess up.
        double failChance = cfg.husbandryShearingFail.getForTier(playerTier);
        if (RANDOM.nextDouble() < failChance) {
            // Whoops, they failed. Let them know they messed up.
            serverPlayer.sendMessage(Text.literal(cfg.messages.husbandryShearingFail), true);

            // If the config says we should punish them, we chip off some durability from their shears.
            // Be careful not to do this if they are in creative mode, obviously!
            if (cfg.husbandryShearingDamageShearsOnFail) {
                stack.damage(1, serverPlayer, net.minecraft.entity.EquipmentSlot.MAINHAND);
            }

            // Even on a failure, they learn a little bit about how NOT to shear a sheep.
            PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // They succeeded! We give them their knowledge XP.
        // We leave the actual shearing and wool dropping to vanilla Minecraft by not cancelling the event.
        PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.HUSBANDRY_ID);
    }
}


