package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.*;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
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
                                      BlockPos pos, PlayerEntity player, Hand hand,
                                      BlockHitResult hit,
                                      CallbackInfoReturnable<ItemActionResult> cir) {
        if (world.isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        // intercept glass bottle (honey) and shears (honeycomb) interactions
        boolean isBottle = stack.isOf(Items.GLASS_BOTTLE);
        boolean isShears = stack.isOf(Items.SHEARS);
        if (!isBottle && !isShears) return;

        // check if the beehive has honey
        int honeyLevel = state.get(BeehiveBlock.HONEY_LEVEL);
        if (honeyLevel < 5) return;

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        int beekeepingTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.BEEKEEPING_ID);

        // fail chance check
        double failChance = cfg.beekeepingHarvestFail.getForTier(beekeepingTier);
        if (RANDOM.nextDouble() < failChance) {
            // anger the bees regardless of campfire
            String msgStr = cfg.messages.beehiveAngeredBees;
            serverPlayer.sendMessage(
                    Text.literal(msgStr),
                    true
            );

            // release bees stored inside the hive as angry
            if (world.getBlockEntity(pos) instanceof BeehiveBlockEntity beehiveEntity) {
                beehiveEntity.angerBees(serverPlayer, state, BeehiveBlockEntity.BeeState.EMERGENCY);
            }

            // also anger any nearby bees that are already outside
            List<net.minecraft.entity.passive.BeeEntity> nearbyBees = world.getEntitiesByClass(
                    net.minecraft.entity.passive.BeeEntity.class,
                    new net.minecraft.util.math.Box(pos).expand(10.0),
                    bee -> true
            );
            for (net.minecraft.entity.passive.BeeEntity bee : nearbyBees) {
                bee.setAngryAt(serverPlayer.getUuid());
                bee.setAngerTime(400 + RANDOM.nextInt(400)); // 20-40 seconds
                bee.setTarget(serverPlayer);
            }

            // grant xp even on failure (you're still learning)
            PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.BEEKEEPING_ID);

            // cancel the normal interaction - no honey/honeycomb
            cir.setReturnValue(ItemActionResult.SUCCESS);
            return;
        }

        // better honey only applies to glass bottle harvests
        if (isBottle) {
            double betterChance = 0.0;
            if (beekeepingTier > 0 && beekeepingTier <= cfg.betterHoneyChance.length) {
                betterChance = cfg.betterHoneyChance[beekeepingTier - 1];
            }

            if (RANDOM.nextDouble() < betterChance) {
                serverPlayer.server.execute(() -> giveBetterHoney(serverPlayer, cfg));
            }
        }

        // grant beekeeping xp
        PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.BEEKEEPING_ID);

        // let vanilla handle the rest (give normal honey/honeycomb, reset honey level, etc.)
    }

    private static void giveBetterHoney(ServerPlayerEntity player, KnowledgeBoundConfig cfg) {
        ItemStack betterHoney = CustomItemRegistry.createRoyalHoney();

        // try to put it in inventory, or drop it
        if (!player.getInventory().insertStack(betterHoney)) {
            player.dropItem(betterHoney, false);
        }

        String template = cfg.messages.royalHoneyHarvested;
        String msgStr = template.replace("{honeyName}", cfg.betterHoney.customName);
        player.sendMessage(
                Text.literal(msgStr),
                true
        );
    }
}
