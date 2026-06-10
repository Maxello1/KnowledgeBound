package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.*;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

/**
 * Hooks into beehive interaction to apply beekeeping mechanics:
 * - fail chance (angers bees even with campfire)
 * - better honey chance
 * - XP granting
 */
@Mixin(BeehiveBlock.class)
public abstract class BeehiveMixin {

    private static final Random RANDOM = new Random();

    @Inject(method = "onUseWithItem", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onUse(ItemStack stack, BlockState state, World world,
                                      BlockPos pos, PlayerEntity player, BlockHitResult hit,
                                      CallbackInfoReturnable<ActionResult> cir) {
        if (world.isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        // only intercept when using a glass bottle (honey harvesting)
        if (!stack.isOf(Items.GLASS_BOTTLE)) return;

        // check if the beehive has honey
        int honeyLevel = state.get(BeehiveBlock.HONEY_LEVEL);
        if (honeyLevel < 5) return;

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        int beekeepingTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.BEEKEEPING_ID);

        // fail chance check
        double failChance = cfg.beekeepingHarvestFail.getForTier(beekeepingTier);
        if (RANDOM.nextDouble() < failChance) {
            // anger the bees regardless of campfire
            serverPlayer.sendMessage(
                    Text.literal("Your clumsy handling angered the bees!")
                            .formatted(Formatting.RED),
                    true
            );

            // release bees as angry
            if (world.getBlockEntity(pos) instanceof BeehiveBlockEntity beehiveEntity) {
                beehiveEntity.angerBees(serverPlayer, state, BeehiveBlockEntity.BeeState.EMERGENCY);
            }

            // grant xp even on failure (you're still learning)
            PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.BEEKEEPING_ID);

            // cancel the normal interaction - no honey
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // success - check for better honey
        double betterChance = 0.0;
        if (beekeepingTier > 0 && beekeepingTier <= cfg.betterHoneyChance.length) {
            betterChance = cfg.betterHoneyChance[beekeepingTier - 1];
        }

        if (RANDOM.nextDouble() < betterChance) {
            // give better honey instead - let vanilla handle the normal interaction
            // but schedule giving the better honey on next tick
            serverPlayer.server.execute(() -> giveBetterHoney(serverPlayer, cfg));
        }

        // grant beekeeping xp
        PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.BEEKEEPING_ID);

        // let vanilla handle the rest (give normal honey, reset honey level, etc.)
    }

    private static void giveBetterHoney(ServerPlayerEntity player, KnowledgeBoundConfig cfg) {
        KnowledgeBoundConfig.BetterHoneyConfig honeyConfig = cfg.betterHoney;

        try {
            Identifier itemId = Identifier.of(honeyConfig.itemId);
            ItemStack betterHoney = new ItemStack(Registries.ITEM.get(itemId));

            // custom name with color
            Formatting color = Formatting.byName(honeyConfig.nameColor);
            if (color == null) color = Formatting.GOLD;
            betterHoney.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(honeyConfig.customName).formatted(color));

            // apply potion effects via components
            List<StatusEffectInstance> effects = new ArrayList<>();
            for (KnowledgeBoundConfig.PotionEffectEntry entry : honeyConfig.effects) {
                Identifier effectId = Identifier.of(entry.effectId);
                Optional<RegistryEntry.Reference<StatusEffect>> effectEntry =
                        Registries.STATUS_EFFECT.getEntry(effectId);
                if (effectEntry.isPresent()) {
                    effects.add(new StatusEffectInstance(
                            effectEntry.get(),
                            entry.durationTicks,
                            entry.amplifier
                    ));
                }
            }

            if (!effects.isEmpty()) {
                // set potion contents on the item
                PotionContentsComponent potionContents = new PotionContentsComponent(
                        Optional.empty(), Optional.empty(), effects
                );
                betterHoney.set(DataComponentTypes.POTION_CONTENTS, potionContents);
            }

            // try to put it in inventory, or drop it
            if (!player.getInventory().insertStack(betterHoney)) {
                player.dropItem(betterHoney, false);
            }

            player.sendMessage(
                    Text.literal("You harvested some " + honeyConfig.customName + "!")
                            .formatted(Formatting.GOLD),
                    true
            );
        } catch (Exception e) {
            KnowledgeBound.LOGGER.warn("[KnowledgeBound] Failed to create better honey item", e);
        }
    }
}
