package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.mechanics.combat.CombatFailHelper;
import net.maxello.knowledgebound.core.KnowledgeRegistry;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Exactly like our BowItemMixin, we want to punish players who try to use high-tier 
 * crossbows without the proper Ranged Combat knowledge. 
 */
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {

    // Same deal as the bow - we use ThreadLocals to quickly stash the outcome of their 
    // skill roll so the shoot method right after it can apply any accuracy penalties.
    @Unique
    private static final ThreadLocal<CombatFailHelper.CombatOutcome> knowledgebound$lastOutcome =
            ThreadLocal.withInitial(() -> CombatFailHelper.CombatOutcome.NORMAL);

    @Unique
    private static final ThreadLocal<ServerPlayerEntity> knowledgebound$currentPlayer =
            new ThreadLocal<>();

    /**
     * We intercept whenever the player clicks with the crossbow in their hand.
     * This fires both when they are trying to load it, AND when they are firing it.
     * We only want to punish them when they actually try to pull the trigger on a loaded bow!
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onCrossbowUse(World world, PlayerEntity user, Hand hand,
                                               CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        // Server side only, naturally.
        if (!(user instanceof ServerPlayerEntity player)) return;
        if (world.isClient()) return;

        ItemStack stack = user.getStackInHand(hand);

        // This is crucial. If the crossbow isn't fully charged, they're just drawing it back.
        // We don't care about drawing, we only care about firing.
        if (!CrossbowItem.isCharged(stack)) return;

        // Roll to see how badly they mess up
        CombatFailHelper.CombatOutcome outcome = CombatFailHelper.rollCombatOutcome(
                player, KnowledgeRegistry.RANGED_COMBAT_ID, stack);

        knowledgebound$lastOutcome.set(outcome);
        knowledgebound$currentPlayer.set(player);

        if (outcome == CombatFailHelper.CombatOutcome.FAIL) {
            // Massive failure! The kickback was too much or they just have slippery fingers.
            // We force them to drop the loaded crossbow on the ground.
            player.getServer().execute(() -> {
                CombatFailHelper.dropWeapon(player);
            });
            // We used to cancel the event here, but letting it fire into the dirt 
            // while dropping the weapon is way more immersive and fun.
        }
    }

    /**
     * If they managed to hang onto the crossbow, we now decide how accurate the shot is.
     * We inject directly into where vanilla sets the velocity of the projectile.
     */
    @ModifyArg(
            method = "shoot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/ProjectileEntity;setVelocity(DDDFF)V"
            ),
            index = 4 // divergence parameter (it's the 5th argument: 3 doubles, 1 float, 1 float... so index 4)
    )
    private float knowledgebound$modifyCrossbowDivergence(float originalDivergence) {
        ServerPlayerEntity player = knowledgebound$currentPlayer.get();
        if (player == null) return originalDivergence;

        // Did they roll a POOR outcome? Then they get a nasty accuracy penalty.
        boolean isPoor = knowledgebound$lastOutcome.get() == CombatFailHelper.CombatOutcome.POOR;
        // The second parameter here is 'true', indicating this is a crossbow shot, 
        // which might have different penalty balancing than a regular bow.
        float penalty = CombatFailHelper.getAccuracyPenalty(player, true, isPoor);

        // Always clean up ThreadLocals when you're done! 
        knowledgebound$currentPlayer.remove();
        knowledgebound$lastOutcome.remove();

        net.maxello.knowledgebound.KnowledgeBound.LOGGER.debug("Crossbow fired! Penalty added: {}", penalty);

        // Slap the extra divergence on top of the vanilla base.
        return originalDivergence + penalty;
    }
}


