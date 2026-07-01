package net.maxello.knowledgebound.mechanics.jobs;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Represents an active supervised workstation job (smelting or cooking).
 * The player must keep the furnace UI open to supervise the job.
 */
public class SupervisedJob {

    public enum JobType {
        SMELTING, COOKING
    }

    public enum JobState {
        /** Player is viewing the furnace UI and the job is progressing. */
        ACTIVE,
        /** Player closed the UI — grace timer is counting down. */
        GRACE_PERIOD,
        /** Smelting/cooking completed — waiting for player to collect the result. */
        COMPLETED,
        /** Job has been failed or cancelled. */
        FAILED
    }

    private final UUID ownerUuid;
    private final RegistryKey<World> dimension;
    private final BlockPos furnacePos;
    private final JobType jobType;
    private final Identifier inputItemId;
    private final int recipeTier;

    private JobState state;
    private int graceTicksRemaining;
    private int collectionTicksRemaining;
    private final long startedAtTick;

    public SupervisedJob(UUID ownerUuid, RegistryKey<World> dimension, BlockPos furnacePos,
                          JobType jobType, Identifier inputItemId, int recipeTier,
                          long startedAtTick) {
        this.ownerUuid = ownerUuid;
        this.dimension = dimension;
        this.furnacePos = furnacePos;
        this.jobType = jobType;
        this.inputItemId = inputItemId;
        this.recipeTier = recipeTier;
        this.state = JobState.ACTIVE;
        this.graceTicksRemaining = 0;
        this.collectionTicksRemaining = 0;
        this.startedAtTick = startedAtTick;
    }

    // --- Getters ---

    public UUID getOwnerUuid() { return ownerUuid; }
    public RegistryKey<World> getDimension() { return dimension; }
    public BlockPos getFurnacePos() { return furnacePos; }
    public JobType getJobType() { return jobType; }
    public Identifier getInputItemId() { return inputItemId; }
    public int getRecipeTier() { return recipeTier; }
    public JobState getState() { return state; }
    public int getGraceTicksRemaining() { return graceTicksRemaining; }
    public int getCollectionTicksRemaining() { return collectionTicksRemaining; }
    public long getStartedAtTick() { return startedAtTick; }

    // --- State transitions ---

    /**
     * Called when the owner closes the furnace UI.
     * Starts the grace period timer.
     */
    public void startGracePeriod(int graceTicks) {
        this.state = JobState.GRACE_PERIOD;
        this.graceTicksRemaining = graceTicks;
    }

    /**
     * Called when the owner reopens the same furnace within the grace period.
     */
    public void resumeFromGrace() {
        this.state = JobState.ACTIVE;
        this.graceTicksRemaining = 0;
    }

    /**
     * Called when the furnace finishes smelting/cooking.
     * Starts the collection window timer.
     */
    public void markCompleted(int collectionTicks) {
        this.state = JobState.COMPLETED;
        this.collectionTicksRemaining = collectionTicks;
    }

    /**
     * Called when the job fails (grace expired, collection expired, disconnect, etc.).
     */
    public void markFailed() {
        this.state = JobState.FAILED;
    }

    /**
     * Tick the job timers. Returns true if the job should be removed (failed).
     */
    public boolean tick() {
        switch (state) {
            case GRACE_PERIOD -> {
                graceTicksRemaining--;
                if (graceTicksRemaining <= 0) {
                    markFailed();
                    return true;
                }
            }
            case COMPLETED -> {
                collectionTicksRemaining--;
                if (collectionTicksRemaining <= 0) {
                    markFailed();
                    return true;
                }
            }
            case FAILED -> {
                return true;
            }
            default -> {
                // ACTIVE — no timer to tick
            }
        }
        return false;
    }

    /**
     * Check if a given player is the owner of this job.
     */
    public boolean isOwner(UUID playerUuid) {
        return ownerUuid.equals(playerUuid);
    }

    /**
     * Get the appropriate config values for this job type.
     */
    public int getConfigGraceTicks() {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        return jobType == JobType.SMELTING
                ? cfg.smeltingGraceTimeTicks
                : cfg.cookingGraceTimeTicks;
    }

    public int getConfigCollectionTicks() {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        return jobType == JobType.SMELTING
                ? cfg.smeltingCollectionWindowTicks
                : cfg.cookingCollectionWindowTicks;
    }

    public String getConfigLeaveBehaviour() {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        return jobType == JobType.SMELTING
                ? cfg.smeltingLeaveBehaviour
                : cfg.cookingLeaveBehaviour;
    }
}



