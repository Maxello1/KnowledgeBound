package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Hooks into the stonecutter to:
 * 1. Require masonry tier 1+ to use it
 * 2. Apply a cutting damage chance (starts at 10%, decreases per tier)
 * 3. Grant masonry XP
 */
@Mixin(StonecutterScreenHandler.class)
public abstract class StonecutterScreenHandlerMixin extends ScreenHandler {

    private static final Random RANDOM = new Random();

    protected StonecutterScreenHandlerMixin(ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    /**
     * onButtonClick is called when the player selects a recipe in the stonecutter.
     * We intercept to check masonry tier and apply cutting effects.
     */
    @Inject(method = "onButtonClick", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onButtonClick(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        int masonryTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.MASONRY_ID);

        // require masonry tier 1 to use stonecutter at all
        if (masonryTier < 1) {
            serverPlayer.sendMessage(
                    Text.literal("You need Masonry Tier 1 to use the stonecutter.")
                            .formatted(Formatting.RED),
                    true
            );
            cir.setReturnValue(false);
            return;
        }

        // apply cutting damage chance
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        double cutChance = cfg.stonecutterCutChanceTier1 - ((masonryTier - 1) * cfg.stonecutterCutReductionPerTier);
        cutChance = Math.max(0.0, cutChance);

        if (RANDOM.nextDouble() < cutChance) {
            serverPlayer.damage(serverPlayer.getDamageSources().generic(), 2.0f);
            serverPlayer.sendMessage(
                    Text.literal("You cut yourself on the stonecutter!")
                            .formatted(Formatting.RED),
                    true
            );
        }

        // grant masonry xp
        PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.MASONRY_ID);
    }
}
