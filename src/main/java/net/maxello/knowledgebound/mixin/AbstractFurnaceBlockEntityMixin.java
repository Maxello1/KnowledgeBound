package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Shadow int cookTime;
    @Shadow int cookTimeTotal;

    /**
     * After the vanilla tick runs, enforce supervision for metallurgy/cooking items.
     *
     * IMPORTANT: We check for existing jobs FIRST, because when vanilla finishes
     * cooking, it empties the input slot before our TAIL injection runs.
     * If we checked input first, we'd miss the completion event.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private static void knowledgebound$afterTick(World world, BlockPos pos, BlockState state,
                                                  AbstractFurnaceBlockEntity blockEntity,
                                                  CallbackInfo ci) {
        if (world.isClient()) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        AbstractFurnaceBlockEntityMixin accessor = (AbstractFurnaceBlockEntityMixin) (Object) blockEntity;

        // ── 1. Handle existing jobs FIRST (before checking input) ──
        // This ensures we detect completion even when vanilla already emptied the input.
        SupervisedJob existingJob = SupervisedJobManager.getJobAt(serverWorld, pos);
        if (existingJob != null) {
            switch (existingJob.getState()) {
                case ACTIVE -> {
                    // Detect completion: output appeared AND input was consumed (or cookTime reset)
                    ItemStack output = blockEntity.getStack(2);
                    ItemStack input = blockEntity.getStack(0);
                    if (!output.isEmpty() && (input.isEmpty() || accessor.cookTime == 0)) {
                        // Cooking/smelting just finished — start collection window
                        SupervisedJobManager.onSmeltComplete(serverWorld, pos);
                    }
                }
                case GRACE_PERIOD -> {
                    // Player closed the UI — freeze/roll back cook progress
                    accessor.cookTime = Math.max(0, accessor.cookTime - 2);
                }
                case COMPLETED -> {
                    // Waiting for player to collect — don't allow further smelting
                    accessor.cookTime = 0;
                }
                case FAILED -> {
                    // Job failed — reset everything
                    accessor.cookTime = 0;
                }
            }
            return; // Job exists — don't try to start a new one
        }

        // ── 2. No existing job — check if input needs supervision ──
        ItemStack input = blockEntity.getStack(0);
        if (input.isEmpty()) return;

        Identifier inputId = Registries.ITEM.getId(input.getItem());
        SupervisedJob.JobType jobType = SupervisedJobManager.getJobTypeForItem(inputId);
        if (jobType == null) return; // Not a supervised item, vanilla handles it

        // Check if the relevant feature is enabled
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (jobType == SupervisedJob.JobType.SMELTING && !cfg.smeltingEnabled) return;
        if (jobType == SupervisedJob.JobType.COOKING && !cfg.cookingEnabled) {
            KnowledgeBound.LOGGER.info("[KB-DEBUG] Cooking disabled in config, skipping supervision for {}", inputId);
            return;
        }

        // Find a player currently viewing this furnace
        ServerPlayerEntity viewer = findPlayerViewingFurnace(serverWorld, pos);

        if (viewer != null) {
            // Start a supervised job for this player
            KnowledgeBound.LOGGER.info("[KB-DEBUG] Starting {} job for {} at {} (item: {})",
                    jobType, viewer.getName().getString(), pos, inputId);
            SupervisedJob job = SupervisedJobManager.startJob(viewer, serverWorld, pos, jobType, inputId);
            if (job == null) {
                // Failed to start (e.g. tier too low or job already exists) — block cooking/smelting
                KnowledgeBound.LOGGER.info("[KB-DEBUG] Failed to start {} job at {} — blocking cookTime", jobType, pos);
                accessor.cookTime = 0;
            }
            // Job started successfully — cooking/smelting will proceed on next tick
        } else {
            // No one is watching — block cooking/smelting entirely
            accessor.cookTime = 0;
        }
    }

    /**
     * Find a player currently viewing a furnace at the given position.
     */
    private static ServerPlayerEntity findPlayerViewingFurnace(ServerWorld world, BlockPos pos) {
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (player.currentScreenHandler instanceof net.minecraft.screen.AbstractFurnaceScreenHandler) {
                if (player.getWorld() == world
                        && player.getBlockPos().isWithinDistance(pos, 8)) {
                    return player;
                }
            }
        }
        return null;
    }

    /**
     * Prevent external insertion (hoppers, etc.) of supervised items.
     */
    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$blockExternalInsert(int slot, ItemStack stack, Direction dir,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (slot != 0) return;
        if (stack.isEmpty()) return;
        if (dir == null) return; // null direction means player interaction, allow

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        SupervisedJob.JobType jobType = SupervisedJobManager.getJobTypeForItem(itemId);
        if (jobType != null) {
            KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
            if (jobType == SupervisedJob.JobType.SMELTING && cfg.smeltingEnabled) {
                cir.setReturnValue(false);
            } else if (jobType == SupervisedJob.JobType.COOKING && cfg.cookingEnabled) {
                cir.setReturnValue(false);
            }
        }
    }
}
