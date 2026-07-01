package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;

import net.maxello.knowledgebound.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gates smithing table operations (armor trims, gem socketing) behind Jeweller tier.
 *
 * After vanilla computes the smithing result, this checks:
 * 1. Whether the result has trims/gems applied
 * 2. Whether the player's Jeweller tier allows that many trims/gems
 *
 * If the player is over their tier limit, the result slot is cleared.
 */
@Mixin(SmithingScreenHandler.class)
public abstract class SmithingScreenHandlerMixin extends ForgingScreenHandler {

    public SmithingScreenHandlerMixin(ScreenHandlerType<?> type, int syncId,
                                       PlayerInventory playerInventory,
                                       ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    /**
     * After vanilla computes the smithing result, check if the operation
     * involves trims/gems and gate it behind Jeweller tier.
     */
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void knowledgebound$afterUpdateResult(CallbackInfo ci) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.jewellerEnabled || !cfg.jewellerSmithingEnabled) return;

        ItemStack result = this.output.getStack(0);
        KnowledgeBound.LOGGER.info("[KB DEBUG] updateResult: result={}", result.getItem().toString());
        if (result.isEmpty()) return;

        // Check if this is a trim/gem operation by comparing the result to the base input
        ItemStack template = this.input.getStack(0);  // template/smithing template
        ItemStack baseInput = this.input.getStack(1); // base item
        ItemStack addition = this.input.getStack(2);  // material/gem
        KnowledgeBound.LOGGER.info("[KB DEBUG] baseInput={}, template={}, addition={}", baseInput.getItem().toString(), template.getItem().toString(), addition.getItem().toString());

        // Only gate trim operations — skip netherite upgrades
        // Netherite upgrades change the item type (iron → netherite), trims don't
        if (!result.getItem().equals(baseInput.getItem())) {
            // Item type changed — this is a netherite upgrade, not a trim
            KnowledgeBound.LOGGER.info("[KB DEBUG] Item type changed, skipping trim check.");
            return;
        }

        // This is a trim/gem operation (item type didn't change)
        // Count how many trims/gems are already on the BASE item
        int existingCount = getGemOrTrimCount(baseInput);
        int newCount = existingCount + 1; // the result adds one more
        KnowledgeBound.LOGGER.info("[KB DEBUG] existingCount={}, newCount={}", existingCount, newCount);

        // Check player tier
        PlayerEntity player = this.player;
        KnowledgeBound.LOGGER.info("[KB DEBUG] player={}, isServerPlayer={}", player != null ? player.getName().getString() : "null", player instanceof ServerPlayerEntity);
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        int playerTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.JEWELLER_ID);
        int maxAllowed = playerTier < cfg.jewellerMaxGemsPerTier.length
                ? cfg.jewellerMaxGemsPerTier[playerTier]
                : 3;
        KnowledgeBound.LOGGER.info("[KB DEBUG] playerTier={}, maxAllowed={}", playerTier, maxAllowed);

        if (newCount > maxAllowed) {
            KnowledgeBound.LOGGER.info("[KB DEBUG] GATING: newCount {} > maxAllowed {}. Clearing slot.", newCount, maxAllowed);
            // Player can't apply this many — clear the result
            this.output.setStack(0, ItemStack.EMPTY);

            // Determine required tier for this count
            int requiredTier = 0;
            for (int t = 0; t < cfg.jewellerMaxGemsPerTier.length; t++) {
                if (cfg.jewellerMaxGemsPerTier[t] >= newCount) {
                    requiredTier = t;
                    break;
                }
            }

            String msg = cfg.messages.jewellerSmithingTierLow
                    .replace("{minTier}", String.valueOf(requiredTier));
            serverPlayer.sendMessage(Text.literal(msg), true);
            return;
        }

        // Allowed — tag the result with the updated gem/trim count for tracking
        NbtCompound nbt = new NbtCompound();
        NbtComponent existing = result.get(DataComponentTypes.CUSTOM_DATA);
        if (existing != null) {
            nbt = existing.copyNbt();
        }
        nbt.putInt("knowledgebound_gem_count", newCount);
        result.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        // XP is granted when the player takes the result (handled by the take listener below)
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

        PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.JEWELLER_ID);
    }

    /**
     * Count gems/trims on an item by reading the custom NBT tag.
     * Falls back to checking vanilla trim components if no custom tag exists.
     */
    private static int getGemOrTrimCount(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        // First check our custom tracking tag
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            if (nbt.contains("knowledgebound_gem_count")) {
                return nbt.getInt("knowledgebound_gem_count");
            }
        }

        // Fallback: check if item has an armor trim component (vanilla 1.21.1)
        if (stack.get(DataComponentTypes.TRIM) != null) {
            return 1; // vanilla only supports 1 trim per item
        }

        return 0;
    }
}


