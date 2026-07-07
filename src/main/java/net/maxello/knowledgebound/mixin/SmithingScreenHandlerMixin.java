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
        KnowledgeBound.LOGGER.debug("Smithing updateResult: result={}", result.getItem().toString());
        // If there's no result, there's nothing for us to gate.
        if (result.isEmpty()) return;

        // Let's grab the actual ingredients the player put into the smithing table.
        ItemStack template = this.input.getStack(0);  // The smithing template (like armor trim or netherite upgrade)
        ItemStack baseInput = this.input.getStack(1); // The base piece of armor/tool
        ItemStack addition = this.input.getStack(2);  // The material (like diamond, redstone, or a modded gem)
        KnowledgeBound.LOGGER.debug("Smithing baseInput={}, template={}, addition={}", baseInput.getItem().toString(), template.getItem().toString(), addition.getItem().toString());

        // We only want to gate *trimming* operations, not straight-up upgrades.
        // If you're upgrading an iron chestplate to netherite, the actual item type changes.
        // If you're just adding a trim to it, the item stays the same. We use that to tell them apart!
        if (!result.getItem().equals(baseInput.getItem())) {
            // The item changed types, so this is an upgrade. We don't care about it here.
            KnowledgeBound.LOGGER.debug("Smithing item type changed, skipping trim check.");
            return;
        }

        // Okay, so it IS a trim or a gem socket. 
        // We need to count how many trims this item ALREADY has on it.
        int existingCount = getGemOrTrimCount(baseInput);
        int newCount = existingCount + 1; // Since they are applying one right now, we add 1.
        KnowledgeBound.LOGGER.debug("Smithing existingCount={}, newCount={}", existingCount, newCount);

        // Make sure we're dealing with a player on the server side.
        PlayerEntity player = this.player;
        KnowledgeBound.LOGGER.debug("Smithing player={}, isServerPlayer={}", player != null ? player.getName().getString() : "null", player instanceof ServerPlayerEntity);
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        // Check their current Jeweller knowledge tier.
        int playerTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.JEWELLER_ID);
        // Find out what their max allowed trims is. If their tier is higher than the config array,
        // we just cap it at a generous 3 as a fallback.
        int maxAllowed = playerTier < cfg.jewellerMaxGemsPerTier.length
                ? cfg.jewellerMaxGemsPerTier[playerTier]
                : 3;
        KnowledgeBound.LOGGER.debug("Smithing playerTier={}, maxAllowed={}", playerTier, maxAllowed);

        // Are they trying to bite off more than they can chew?
        if (newCount > maxAllowed) {
            KnowledgeBound.LOGGER.debug("Smithing GATING: newCount {} > maxAllowed {}. Clearing slot.", newCount, maxAllowed);
            // Too many trims for their skill level! We straight up wipe the result slot.
            // The vanilla game won't let them click on anything to craft it.
            this.output.setStack(0, ItemStack.EMPTY);

            // Let's figure out what tier they ACTUALLY need to do this, so we can tell them.
            int requiredTier = 0;
            for (int t = 0; t < cfg.jewellerMaxGemsPerTier.length; t++) {
                if (cfg.jewellerMaxGemsPerTier[t] >= newCount) {
                    requiredTier = t;
                    break;
                }
            }

            // Send a nice formatted message letting them know they need to study more.
            String msg = cfg.messages.jewellerSmithingTierLow
                    .replace("{minTier}", String.valueOf(requiredTier));
            serverPlayer.sendMessage(Text.literal(msg), true);
            return;
        }

        // They pass the test! We allow it.
        // We just need to make sure we tag the newly crafted item with the updated trim count.
        // That way, we can check it again if they decide to add another one later.
        NbtCompound nbt = new NbtCompound();
        NbtComponent existing = result.get(DataComponentTypes.CUSTOM_DATA);
        if (existing != null) {
            nbt = existing.copyNbt();
        }
        nbt.putInt("knowledgebound_gem_count", newCount);
        result.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        // Note: We don't give them XP here yet. We give it when they actually take the item out!
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


