package net.maxello.knowledgebound.mixin;
import net.maxello.knowledgebound.mechanics.combat.CombatFailHelper;
import net.maxello.knowledgebound.core.KnowledgeRegistry;

import net.maxello.knowledgebound.KnowledgeBound;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * We're messing with bows here so players can't just pick up a high-tier bow
 * and instantly be Legolas. If their Ranged Combat tier is too low, we punish them
 * by making their arrows wildly inaccurate, or even having them straight up drop the bow.
 */
@Mixin(BowItem.class)
public class BowItemMixin {

    // Because the vanilla code doesn't easily let us pass data between the method that handles
    // releasing the bow and the method that actually spawns the arrow entity, we use ThreadLocals.
    // This safely stores the outcome of their "skill check" for a split second so the next method can read it.
    @Unique
    private static final ThreadLocal<CombatFailHelper.CombatOutcome> knowledgebound$lastOutcome =
            ThreadLocal.withInitial(() -> CombatFailHelper.CombatOutcome.NORMAL);

    @Unique
    private static final ThreadLocal<ServerPlayerEntity> knowledgebound$currentPlayer =
            new ThreadLocal<>();

    /**
     * This fires the exact moment the player lets go of the mouse button to fire the bow.
     * We want to do our skill check right here before the arrow is even created.
     */
    @Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onBowRelease(ItemStack stack, World world, LivingEntity user,
                                              int remainingUseTicks, CallbackInfo ci) {
        // Mobs can use bows too, but we only care about real players on the server.
        if (!(user instanceof ServerPlayerEntity player)) {
            knowledgebound$lastOutcome.set(CombatFailHelper.CombatOutcome.NORMAL);
            return;
        }

        // Roll the dice! Are they going to shoot normally, shoot poorly, or completely fumble?
        CombatFailHelper.CombatOutcome outcome = CombatFailHelper.rollCombatOutcome(
                player, KnowledgeRegistry.RANGED_COMBAT_ID, stack);

        // Save the outcome and the player in our ThreadLocals so the next method below can use them.
        knowledgebound$lastOutcome.set(outcome);
        knowledgebound$currentPlayer.set(player);

        if (outcome == CombatFailHelper.CombatOutcome.FAIL) {
            // Critical fail! They are so inexperienced they just drop the bow entirely.
            // We schedule this on the server thread to make sure it drops cleanly.
            player.getServer().execute(() -> {
                CombatFailHelper.dropWeapon(player);
            });
            // Note: We deliberately do NOT cancel the event here anymore.
            // We want the arrow to still shoot (even if wildly inaccurate) while they drop the bow, 
            // because it's hilarious and feels more natural than the bow just vanishing and doing nothing.
        }
    }

    /**
     * This is where we actually ruin their aim. 
     * Vanilla spawns the arrow and sets its velocity. One of the parameters it passes to setVelocity 
     * is the "divergence" (how much the arrow wobbles off course). 
     * We're grabbing that specific parameter right as it's passed in, and making it much worse.
     */
    @ModifyArg(
            method = "shoot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/ProjectileEntity;setVelocity(Lnet/minecraft/entity/Entity;FFFFF)V"
            ),
            index = 5 // divergence parameter (it's the 6th argument, so index 5)
    )
    private float knowledgebound$modifyBowDivergence(float originalDivergence) {
        // Grab the player we stored just a microsecond ago
        ServerPlayerEntity player = knowledgebound$currentPlayer.get();
        if (player == null) return originalDivergence;

        // Did they roll a POOR outcome? If so, we calculate a heavy penalty to their aim.
        boolean isPoor = knowledgebound$lastOutcome.get() == CombatFailHelper.CombatOutcome.POOR;
        float penalty = CombatFailHelper.getAccuracyPenalty(player, false, isPoor);

        // Always clean up your ThreadLocals! Memory leaks are bad.
        knowledgebound$currentPlayer.remove();
        knowledgebound$lastOutcome.remove();

        KnowledgeBound.LOGGER.info("[KB DEBUG] Bow fired! Penalty added: {}", penalty);

        // Add our penalty on top of whatever the vanilla divergence was going to be.
        return originalDivergence + penalty;
    }
}

