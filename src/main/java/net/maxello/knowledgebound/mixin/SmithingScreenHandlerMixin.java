package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/**
 * Gates real armor-trim operations behind Jeweller tier.
 *
 * <p>Gem socketing belongs to Teapot Cosmetics' Gem Table. This mixin therefore
 * compares the input and output trim components directly, which excludes
 * netherite upgrades and unrelated same-item smithing recipes.</p>
 */
@Mixin(SmithingScreenHandler.class)
public abstract class SmithingScreenHandlerMixin extends ForgingScreenHandler {

    public SmithingScreenHandlerMixin(ScreenHandlerType<?> type, int syncId,
                                       PlayerInventory playerInventory,
                                       ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    /**
     * After vanilla computes the smithing result, gate only a newly changed trim.
     */
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void knowledgebound$afterUpdateResult(CallbackInfo ci) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.jewellerEnabled || !cfg.jewellerSmithingEnabled) return;

        ItemStack result = this.output.getStack(0);
        if (result.isEmpty()) return;

        ItemStack baseInput = this.input.getStack(1);
        if (!isArmorTrimOperation(baseInput, result)) return;

        PlayerEntity player = this.player;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        int playerTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.JEWELLER_ID);
        int requiredTier = getRequiredTrimTier(cfg);
        if (playerTier < requiredTier) {
            this.output.setStack(0, ItemStack.EMPTY);
            String msg = cfg.messages.jewellerSmithingTierLow
                    .replace("{minTier}", String.valueOf(requiredTier));
            serverPlayer.sendMessage(Text.literal(msg), true);
        }
    }

    /**
     * Grant Jeweller XP when the player takes the smithing result.
     */
    @Inject(method = "onTakeOutput", at = @At("HEAD"))
    private void knowledgebound$onTakeOutput(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.jewellerEnabled || !cfg.jewellerSmithingEnabled) return;
        if (stack.isEmpty()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (!isArmorTrimOperation(this.input.getStack(1), stack)) return;

        PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.JEWELLER_ID);
    }

    private static boolean isArmorTrimOperation(ItemStack baseInput, ItemStack result) {
        if (baseInput.isEmpty() || result.isEmpty()) return false;
        if (!result.isOf(baseInput.getItem())) return false;

        var inputTrim = baseInput.get(DataComponentTypes.TRIM);
        var resultTrim = result.get(DataComponentTypes.TRIM);
        return resultTrim != null && !Objects.equals(inputTrim, resultTrim);
    }

    private static int getRequiredTrimTier(KnowledgeBoundConfig cfg) {
        int[] socketLimits = cfg.jewellerMaxGemsPerTier;
        if (socketLimits == null) return 1;

        for (int tier = 0; tier < socketLimits.length; tier++) {
            if (socketLimits[tier] > 0) {
                return tier;
            }
        }
        return 1;
    }
}


