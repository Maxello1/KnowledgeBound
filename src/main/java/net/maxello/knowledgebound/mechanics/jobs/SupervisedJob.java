package net.maxello.knowledgebound.mechanics.jobs;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * A data object representing a single active "supervised" workstation job.
 * 
 * "Supervising" means a player has to physically keep the furnace/smoker UI open
 * while an item is cooking. If they walk away or close the screen, the job enters
 * a "grace period". If the grace period expires before they return, the item burns up!
 * 
 * We also have a "collection window". Once the item finishes cooking, the player
 * has a few seconds to take it out before it cools down and ruins the item.
 */
public class SupervisedJob {

    public enum JobType {
        SMELTING, COOKING
    }

    public enum JobState {
        /** The player is actively staring at the furnace UI. Everything is fine. */
        ACTIVE,
        /** The player closed the UI! The grace timer is counting down. Hurry back! */
        GRACE_PERIOD,
        /** The item is done cooking. The player must grab it from the output slot now. */
        COMPLETED,
        /** The player messed up, walked away, or took too long. The job is ruined. */
        FAILED
    }

    // Who owns this job? We use UUIDs so we don't accidentally hold a memory leak to a disconnected player.
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
     * Oh no, the player closed the furnace screen!
     * Start the countdown timer. If this hits 0, they fail the job.
     */
    public void startGracePeriod(int graceTicks) {
        this.state = JobState.GRACE_PERIOD;
        this.graceTicksRemaining = graceTicks;
    }

    /**
     * Phew, they reopened the furnace screen in time.
     * Clear the timer and go back to normal.
     */
    public void resumeFromGrace() {
        this.state = JobState.ACTIVE;
        this.graceTicksRemaining = 0;
    }

    /**
     * Ding! The furnace finished cooking the item.
     * Start the collection countdown. They better grab it quick.
     */
    public void markCompleted(int collectionTicks) {
        this.state = JobState.COMPLETED;
        this.collectionTicksRemaining = collectionTicks;
    }

    /**
     * Welp, they failed. The job is marked dead and the manager will clean it up.
     */
    public void markFailed() {
        this.state = JobState.FAILED;
    }

    /**
     * Ticks down the timers every server tick.
     * Returns true if the job just failed and needs to be deleted.
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
                // ACTIVE — The player is staring at the screen, so no timer is ticking.
            }
        }
        return false;
    }

    /**
     * Simple check to make sure the person trying to interact with the furnace
     * is the actual person who started the job. (No stealing other people's smelting jobs!)
     */
    public boolean isOwner(UUID playerUuid) {
        return ownerUuid.equals(playerUuid);
    }

    // --- Config Helpers ---

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



