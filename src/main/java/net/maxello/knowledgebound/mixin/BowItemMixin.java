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

@Mixin(BowItem.class)
public class BowItemMixin {

    @Unique
    private static final ThreadLocal<CombatFailHelper.CombatOutcome> knowledgebound$lastOutcome =
            ThreadLocal.withInitial(() -> CombatFailHelper.CombatOutcome.NORMAL);

    @Unique
    private static final ThreadLocal<ServerPlayerEntity> knowledgebound$currentPlayer =
            new ThreadLocal<>();

    @Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onBowRelease(ItemStack stack, World world, LivingEntity user,
                                              int remainingUseTicks, CallbackInfo ci) {
        if (!(user instanceof ServerPlayerEntity player)) {
            knowledgebound$lastOutcome.set(CombatFailHelper.CombatOutcome.NORMAL);
            return;
        }

        CombatFailHelper.CombatOutcome outcome = CombatFailHelper.rollCombatOutcome(
                player, KnowledgeRegistry.RANGED_COMBAT_ID, stack);

        knowledgebound$lastOutcome.set(outcome);
        knowledgebound$currentPlayer.set(player);

        if (outcome == CombatFailHelper.CombatOutcome.FAIL) {
            player.getServer().execute(() -> {
                CombatFailHelper.dropWeapon(player);
            });
            // We no longer cancel the arrow shot
        }
    }

    /**
     * Modify the divergence argument of ProjectileEntity.setVelocity(Entity, FFFFF)
     * inside the BowItem.shoot() method (called by shootAll from onStoppedUsing).
     * In 1.21.1, the call chain is: onStoppedUsing → shootAll → shoot → setVelocity.
     * The divergence is the 6th parameter (index 5).
     */
    @ModifyArg(
            method = "shoot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/ProjectileEntity;setVelocity(Lnet/minecraft/entity/Entity;FFFFF)V"
            ),
            index = 5 // divergence parameter (last float)
    )
    private float knowledgebound$modifyBowDivergence(float originalDivergence) {
        ServerPlayerEntity player = knowledgebound$currentPlayer.get();
        if (player == null) return originalDivergence;

        boolean isPoor = knowledgebound$lastOutcome.get() == CombatFailHelper.CombatOutcome.POOR;
        float penalty = CombatFailHelper.getAccuracyPenalty(player, false, isPoor);

        // Clean up ThreadLocals
        knowledgebound$currentPlayer.remove();
        knowledgebound$lastOutcome.remove();

        KnowledgeBound.LOGGER.info("[KB DEBUG] Bow fired! Penalty added: {}", penalty);

        return originalDivergence + penalty;
    }
}

