package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.CombatFailHelper;
import net.maxello.knowledgebound.KnowledgeRegistry;
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

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {

    @Unique
    private static final ThreadLocal<CombatFailHelper.CombatOutcome> knowledgebound$lastOutcome =
            ThreadLocal.withInitial(() -> CombatFailHelper.CombatOutcome.NORMAL);

    @Unique
    private static final ThreadLocal<ServerPlayerEntity> knowledgebound$currentPlayer =
            new ThreadLocal<>();

    /**
     * Intercept crossbow use — if the crossbow is loaded and ready to fire,
     * perform the combat fail roll. On FAIL, drop the crossbow (still loaded)
     * and cancel the shot.
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onCrossbowUse(World world, PlayerEntity user, Hand hand,
                                               CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (!(user instanceof ServerPlayerEntity player)) return;
        if (world.isClient()) return;

        ItemStack stack = user.getStackInHand(hand);

        // Only intercept if the crossbow is loaded (about to fire)
        if (!CrossbowItem.isCharged(stack)) return;

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
     * Modify projectile divergence for accuracy penalty.
     * In 1.21.1, CrossbowItem.shoot() calls ProjectileEntity.setVelocity(DDDFF)
     * where the last float (index 4) is the divergence.
     */
    @ModifyArg(
            method = "shoot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/ProjectileEntity;setVelocity(DDDFF)V"
            ),
            index = 4 // divergence parameter (last float, after 3 doubles and 1 float)
    )
    private float knowledgebound$modifyCrossbowDivergence(float originalDivergence) {
        ServerPlayerEntity player = knowledgebound$currentPlayer.get();
        if (player == null) return originalDivergence;

        boolean isPoor = knowledgebound$lastOutcome.get() == CombatFailHelper.CombatOutcome.POOR;
        float penalty = CombatFailHelper.getAccuracyPenalty(player, true, isPoor);

        // Clean up ThreadLocals
        knowledgebound$currentPlayer.remove();
        knowledgebound$lastOutcome.remove();

        net.maxello.knowledgebound.KnowledgeBound.LOGGER.info("[KB DEBUG] Crossbow fired! Penalty added: {}", penalty);

        return originalDivergence + penalty;
    }
}
