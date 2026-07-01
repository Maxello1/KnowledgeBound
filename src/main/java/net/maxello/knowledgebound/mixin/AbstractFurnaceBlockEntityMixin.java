package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.util.KbIdHelper;
import net.maxello.knowledgebound.mechanics.jobs.SupervisedJob;
import net.maxello.knowledgebound.mechanics.jobs.SupervisedJobManager;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;

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

    // We're grabbing these vanilla fields so we can manipulate the furnace's progress.
    // By forcing cookTime to 0 later down the line, we basically tell the furnace to freeze in its tracks.
    @Shadow int cookTime;
    @Shadow int cookTimeTotal;

    /**
     * This fires right after the furnace does its normal vanilla tick update.
     * We have to do it at the TAIL (end of the tick) because we need to catch the exact moment
     * an item finishes smelting. If we did it at the start, we wouldn't see the result of the tick!
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private static void knowledgebound$afterTick(World world, BlockPos pos, BlockState state,
                                                  AbstractFurnaceBlockEntity blockEntity,
                                                  CallbackInfo ci) {
        // We only care about what happens on the server side where the real logic and jobs live.
        if (world.isClient()) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        AbstractFurnaceBlockEntityMixin accessor = (AbstractFurnaceBlockEntityMixin) (Object) blockEntity;

        // ── 1. Check if there's already a job running here ──
        // It's super important we do this BEFORE checking the input slot. 
        // Why? Because when the vanilla furnace finishes cooking an item, it instantly removes the item 
        // from the input slot. If we checked the input slot first, we might think the furnace 
        // is empty and completely miss the fact that it just successfully finished a job!
        SupervisedJob existingJob = SupervisedJobManager.getJobAt(serverWorld, pos);
        if (existingJob != null) {
            switch (existingJob.getState()) {
                case ACTIVE -> {
                    // How do we know it actually finished? There's an output item present, and either the input 
                    // is gone, or the cook time reset to 0. 
                    ItemStack output = blockEntity.getStack(2);
                    ItemStack input = blockEntity.getStack(0);
                    if (!output.isEmpty() && (input.isEmpty() || accessor.cookTime == 0)) {
                        // Boom, it just finished smelting/cooking! Let's kick off the collection window
                        // so the player can grab their shiny new item before it fails.
                        SupervisedJobManager.onSmeltComplete(serverWorld, pos);
                    }
                }
                case GRACE_PERIOD -> {
                    // The player walked away or closed the furnace UI. We don't want to completely ruin 
                    // their progress instantly, so we slowly dial back the cook time by 2 ticks. 
                    // It gives them a brief window to run back and re-open it.
                    accessor.cookTime = Math.max(0, accessor.cookTime - 2);
                }
                case COMPLETED -> {
                    // The job is totally done, and we're just waiting for the player to grab the output.
                    // We lock the cook time at 0 so it doesn't try to automatically start the next item 
                    // in the stack without them watching.
                    accessor.cookTime = 0;
                }
                case FAILED -> {
                    // Something went wrong (they let it sit too long, closed it too long, etc). 
                    // Just reset the progress entirely. Better luck next time!
                    accessor.cookTime = 0;
                }
            }
            // Since we're currently tracking a job for this furnace, we bail out early so we 
            // don't accidentally try to spin up a brand new one while this one is still resolving.
            return; 
        }

        // ── 2. No job is running, let's see if we need to start one ──
        // First things first, is there actually something in the furnace to cook?
        ItemStack input = blockEntity.getStack(0);
        if (input.isEmpty()) return;

        // Figure out if the item trying to cook is something we even care about supervising.
        Identifier inputId = Identifier.of(net.maxello.knowledgebound.util.KbIdHelper.getKbId(input));
        SupervisedJob.JobType jobType = SupervisedJobManager.getJobTypeForItem(inputId);
        // If it's just normal vanilla stuff that isn't gated by our mod, we don't interfere. Let it cook!
        if (jobType == null) return; 

        // Double check our mod config to make sure this specific mechanic is actually turned on.
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (jobType == SupervisedJob.JobType.SMELTING && !cfg.smeltingEnabled) return;
        if (jobType == SupervisedJob.JobType.COOKING && !cfg.cookingEnabled) {
            KnowledgeBound.LOGGER.info("[KB-DEBUG] Cooking disabled in config, skipping supervision for {}", inputId);
            return;
        }

        // Okay, it's a supervised item. We need to find if there's an actual player 
        // actively staring at the furnace UI right now.
        ServerPlayerEntity viewer = findPlayerViewingFurnace(serverWorld, pos);

        if (viewer != null) {
            // We found someone! Let's try to start a new supervised job for them.
            KnowledgeBound.LOGGER.info("[KB-DEBUG] Starting {} job for {} at {} (item: {})",
                    jobType, viewer.getName().getString(), pos, inputId);
            SupervisedJob job = SupervisedJobManager.startJob(viewer, serverWorld, pos, jobType, inputId);
            if (job == null) {
                // Uh oh, we couldn't start the job. This usually means their tier isn't high enough yet,
                // or maybe a weird edge case where a job already existed in memory. Either way, we freeze the furnace 
                // so they don't get free items they haven't earned yet.
                KnowledgeBound.LOGGER.info("[KB-DEBUG] Failed to start {} job at {} — blocking cookTime", jobType, pos);
                accessor.cookTime = 0;
            }
            // If it did start successfully, we just let the furnace keep ticking normally, nothing else to do.
        } else {
            // Nobody is looking at the furnace, so it doesn't get to cook supervised items. 
            // We block the cook time so you can't just leave gated items cooking while you go mine.
            accessor.cookTime = 0;
        }
    }

    /**
     * Simple helper to figure out if any player is actively looking at this specific furnace.
     */
    private static ServerPlayerEntity findPlayerViewingFurnace(ServerWorld world, BlockPos pos) {
        // Just loop through everyone on the server and check their open screen handler.
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (player.currentScreenHandler instanceof net.minecraft.screen.AbstractFurnaceScreenHandler) {
                // Make sure they're in the same dimension and actually somewhat close to the furnace.
                // An 8 block radius is plenty generous.
                if (player.getWorld() == world
                        && player.getBlockPos().isWithinDistance(pos, 8)) {
                    return player;
                }
            }
        }
        return null;
    }

    /**
     * We don't want players bypassing the whole "supervision" mechanic by just throwing a hopper 
     * on top of the furnace. This completely blocks external things from piping in supervised items.
     */
    @Inject(method = "canInsert", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$blockExternalInsert(int slot, ItemStack stack, Direction dir,
                                                     CallbackInfoReturnable<Boolean> cir) {
        // We only care about the top slot (input), because fuel goes in the side/bottom.
        if (slot != 0) return;
        if (stack.isEmpty()) return;
        // If the direction is null, it usually means a player put it in manually through the UI, which is fine.
        if (dir == null) return; 

        // Check if the item they're trying to automate is gated behind a tier.
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        SupervisedJob.JobType jobType = SupervisedJobManager.getJobTypeForItem(itemId);
        
        if (jobType != null) {
            KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
            // If the mechanic is enabled in the config, hard-cancel the insertion. 
            // Hoppers get denied! You gotta cook this by hand.
            if (jobType == SupervisedJob.JobType.SMELTING && cfg.smeltingEnabled) {
                cir.setReturnValue(false);
            } else if (jobType == SupervisedJob.JobType.COOKING && cfg.cookingEnabled) {
                cir.setReturnValue(false);
            }
        }
    }
}


