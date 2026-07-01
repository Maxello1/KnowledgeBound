package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.CustomItemRegistry;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;

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
 * Here we hook into how players interact with Beehives to sprinkle in some fun Beekeeping mechanics.
 * Specifically, we want a chance for the player to fumble the harvest and piss off the bees (even if they
 * were smart and put a campfire underneath!). But to balance it out, if they use a bottle, they have a chance 
 * to score some fancy "Royal Honey" if their tier is high enough.
 */
@Mixin(BeehiveBlock.class)
public abstract class BeehiveMixin {

    private static final Random RANDOM = new Random();

    // We inject at HEAD because if we wait too long, vanilla will already have given them the honey
    // and reset the hive. We want to potentially interrupt that whole process.
    @Inject(method = "onUseWithItem", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onUse(ItemStack stack, BlockState state, World world,
                                      BlockPos pos, PlayerEntity player, Hand hand,
                                      BlockHitResult hit,
                                      CallbackInfoReturnable<ItemActionResult> cir) {
        // Everything fun happens on the server side
        if (world.isClient()) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        // We only care about people trying to actually harvest the hive. 
        // Bottles get you honey, shears get you honeycomb. Anything else is ignored.
        boolean isBottle = stack.isOf(Items.GLASS_BOTTLE);
        boolean isShears = stack.isOf(Items.SHEARS);
        if (!isBottle && !isShears) return;

        // Make sure the hive is actually full. A full hive is honey level 5.
        // If it's not full, let vanilla handle it (which basically just does nothing).
        int honeyLevel = state.get(BeehiveBlock.HONEY_LEVEL);
        if (honeyLevel < 5) return;

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        int beekeepingTier = PlayerKnowledgeManager.getTier(serverPlayer, KnowledgeRegistry.BEEKEEPING_ID);

        // ── 1. The Fail Chance ──
        // Based on their beekeeping tier, they might just completely mess up the harvest.
        double failChance = cfg.beekeepingHarvestFail.getForTier(beekeepingTier);
        if (RANDOM.nextDouble() < failChance) {
            // Uh oh, they blew it. We tell them they made a mistake.
            String msgStr = cfg.messages.beehiveAngeredBees;
            serverPlayer.sendMessage(
                    Text.literal(msgStr),
                    true
            );

            // Now we gotta trigger the swarm. 
            // First, we force the hive block entity to spit out all the bees it's holding, 
            // and we flag them as an EMERGENCY so they come out angry.
            if (world.getBlockEntity(pos) instanceof BeehiveBlockEntity beehiveEntity) {
                beehiveEntity.angerBees(serverPlayer, state, BeehiveBlockEntity.BeeState.EMERGENCY);
            }

            // But wait, there's more! If there are bees already buzzing around outside near the hive, 
            // we want them to join the attack too. We scan a 10 block radius for any loose bees.
            List<net.minecraft.entity.passive.BeeEntity> nearbyBees = world.getEntitiesByClass(
                    net.minecraft.entity.passive.BeeEntity.class,
                    new net.minecraft.util.math.Box(pos).expand(10.0),
                    bee -> true
            );
            for (net.minecraft.entity.passive.BeeEntity bee : nearbyBees) {
                // Lock onto the clumsy player and set an anger timer (20-40 seconds)
                bee.setAngryAt(serverPlayer.getUuid());
                bee.setAngerTime(400 + RANDOM.nextInt(400)); 
                bee.setTarget(serverPlayer);
            }

            // We're nice though. Even though they failed and are currently running for their lives, 
            // they still learn from the experience, so we grant them some XP.
            PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.BEEKEEPING_ID);

            // Cancel the normal interaction — they don't get the honey, they just get stung.
            cir.setReturnValue(ItemActionResult.SUCCESS);
            return;
        }

        // ── 2. The Royal Honey Chance ──
        // They didn't fail! If they used a bottle (because you can't get fancy honey with shears),
        // we roll the dice to see if they get an upgraded drop.
        if (isBottle) {
            double betterChance = 0.0;
            // The chance for better honey scales directly with their tier.
            if (beekeepingTier > 0 && beekeepingTier <= cfg.betterHoneyChance.length) {
                betterChance = cfg.betterHoneyChance[beekeepingTier - 1];
            }

            if (RANDOM.nextDouble() < betterChance) {
                // Jackpot! We execute this via the server thread just to make sure 
                // it drops properly and doesn't desync their inventory.
                serverPlayer.server.execute(() -> giveBetterHoney(serverPlayer, cfg));
            }
        }

        // They successfully harvested the hive, so give them their hard-earned XP.
        PlayerKnowledgeManager.grantMinuteIfAllowed(serverPlayer, KnowledgeRegistry.BEEKEEPING_ID);

        // We don't cancel the event here! We step back and let vanilla run its course.
        // Vanilla will handle giving them the regular honey bottle or honeycomb, resetting the honey level,
        // and damaging the shears or taking the bottle.
    }

    /**
     * Helper to actually spawn the Royal Honey item and shove it into the player's hands.
     */
    private static void giveBetterHoney(ServerPlayerEntity player, KnowledgeBoundConfig cfg) {
        ItemStack betterHoney = CustomItemRegistry.createRoyalHoney();

        // We try to neatly tuck it into their inventory first.
        // If their bags are totally full, we just violently throw it on the ground at their feet.
        if (!player.getInventory().insertStack(betterHoney)) {
            player.dropItem(betterHoney, false);
        }

        // Give 'em a little pop-up message so they know they got lucky.
        String template = cfg.messages.royalHoneyHarvested;
        String msgStr = template.replace("{honeyName}", cfg.betterHoney.customName);
        player.sendMessage(
                Text.literal(msgStr),
                true
        );
    }
}


