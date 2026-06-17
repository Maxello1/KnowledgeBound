package net.maxello.knowledgebound;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * Manages all active supervised workstation jobs (smelting and cooking).
 * Jobs are NOT persisted across restarts — disconnecting/restart = fail.
 */
public final class SupervisedJobManager {

    private SupervisedJobManager() {}

    /** Active jobs keyed by "dimension:x,y,z" string for uniqueness across dimensions. */
    private static final Map<String, SupervisedJob> ACTIVE_JOBS = new HashMap<>();

    /** Maps player UUID to the furnace position they are currently supervising. */
    private static final Map<UUID, String> PLAYER_TO_JOB = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(SupervisedJobManager::tick);
        KnowledgeBound.LOGGER.info("[KnowledgeBound] SupervisedJobManager initialized.");
    }

    // --- Key helpers ---

    private static String posKey(ServerWorld world, BlockPos pos) {
        return world.getRegistryKey().getValue() + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    /**
     * Find the furnace position that a player is currently supervising.
     * Returns null if the player has no active job.
     */
    public static BlockPos findFurnacePosForPlayer(ServerPlayerEntity player) {
        String key = PLAYER_TO_JOB.get(player.getUuid());
        if (key == null) return null;
        SupervisedJob job = ACTIVE_JOBS.get(key);
        if (job == null) return null;
        return job.getFurnacePos();
    }

    // --- Public API ---

    /**
     * Check if a furnace at the given position is being supervised by any player.
     */
    public static boolean isSupervised(ServerWorld world, BlockPos pos) {
        return ACTIVE_JOBS.containsKey(posKey(world, pos));
    }

    /**
     * Get the active job at a position, or null.
     */
    public static SupervisedJob getJobAt(ServerWorld world, BlockPos pos) {
        return ACTIVE_JOBS.get(posKey(world, pos));
    }

    private static final Set<String> DEFAULT_METALLURGY_ITEMS = Set.of(
            "minecraft:raw_iron", "minecraft:raw_gold", "minecraft:raw_copper",
            "minecraft:iron_ore", "minecraft:gold_ore", "minecraft:copper_ore",
            "minecraft:deepslate_iron_ore", "minecraft:deepslate_gold_ore", "minecraft:deepslate_copper_ore",
            "minecraft:ancient_debris", "minecraft:clay_ball", "minecraft:clay"
    );

    /**
     * Check if a specific input item is a metallurgy item (for smelting supervision).
     */
    public static boolean isMetallurgyItem(Identifier itemId) {
        String id = itemId.toString();
        List<String> configItems = KnowledgeBoundConfig.INSTANCE.metallurgyItems;
        if (configItems != null && !configItems.isEmpty()) {
            return configItems.contains(id);
        }
        return DEFAULT_METALLURGY_ITEMS.contains(id);
    }

    /** Hardcoded vanilla food items that require cooking supervision. */
    private static final Set<String> DEFAULT_COOKING_ITEMS = Set.of(
            "minecraft:beef", "minecraft:porkchop", "minecraft:chicken",
            "minecraft:mutton", "minecraft:rabbit", "minecraft:cod",
            "minecraft:salmon", "minecraft:potato", "minecraft:kelp"
    );

    /**
     * Check if a specific input item is a cooking item.
     * Uses config list if populated, otherwise falls back to hardcoded defaults.
     */
    public static boolean isCookingItem(Identifier itemId) {
        String id = itemId.toString();
        List<String> configItems = KnowledgeBoundConfig.INSTANCE.cookingItems;
        if (configItems != null && !configItems.isEmpty()) {
            return configItems.contains(id);
        }
        return DEFAULT_COOKING_ITEMS.contains(id);
    }

    /**
     * Determine the job type for an input item, or null if not supervised.
     */
    public static SupervisedJob.JobType getJobTypeForItem(Identifier itemId) {
        if (isMetallurgyItem(itemId)) return SupervisedJob.JobType.SMELTING;
        if (isCookingItem(itemId)) return SupervisedJob.JobType.COOKING;
        return null;
    }

    /**
     * Start a new supervised job when a player places a tracked item in a furnace.
     */
    public static SupervisedJob startJob(ServerPlayerEntity player, ServerWorld world,
                                          BlockPos furnacePos, SupervisedJob.JobType jobType,
                                          Identifier inputItemId) {
        String key = posKey(world, furnacePos);

        // Don't start if another job is already active at this position
        if (ACTIVE_JOBS.containsKey(key)) return null;

        // Get recipe tier (from config, default 0)
        int recipeTier = getRecipeTier(jobType, inputItemId);

        // Check if player meets minimum tier
        Identifier knowledgeId = jobType == SupervisedJob.JobType.SMELTING
                ? KnowledgeRegistry.SMELTING_ID
                : KnowledgeRegistry.COOKING_ID;
        int playerTier = PlayerKnowledgeManager.getTier(player, knowledgeId);

        if (playerTier < recipeTier) {
            String msg = jobType == SupervisedJob.JobType.SMELTING
                    ? KnowledgeBoundConfig.INSTANCE.messages.smeltingTierLocked
                    : KnowledgeBoundConfig.INSTANCE.messages.smeltingTierLocked; // reuse for now
            msg = msg.replace("{minTier}", String.valueOf(recipeTier));
            player.sendMessage(net.minecraft.text.Text.literal(msg), true);
            return null;
        }

        SupervisedJob job = new SupervisedJob(
                player.getUuid(),
                world.getRegistryKey(),
                furnacePos,
                jobType,
                inputItemId,
                recipeTier,
                world.getServer().getTicks()
        );

        ACTIVE_JOBS.put(key, job);
        PLAYER_TO_JOB.put(player.getUuid(), key);

        KnowledgeBound.LOGGER.debug("[KnowledgeBound] Started {} job for {} at {}",
                jobType, player.getName().getString(), furnacePos);
        return job;
    }

    /**
     * Called when a player closes a furnace screen.
     */
    public static void onPlayerCloseScreen(ServerPlayerEntity player) {
        String key = PLAYER_TO_JOB.get(player.getUuid());
        if (key == null) return;

        SupervisedJob job = ACTIVE_JOBS.get(key);
        if (job == null || !job.isOwner(player.getUuid())) return;

        if (job.getState() == SupervisedJob.JobState.ACTIVE) {
            job.startGracePeriod(job.getConfigGraceTicks());
            KnowledgeBound.LOGGER.debug("[KnowledgeBound] Grace period started for {} at {}",
                    player.getName().getString(), job.getFurnacePos());
        }
    }

    /**
     * Called when a player opens a furnace screen. Resumes grace period if applicable.
     * Returns true if the player is allowed to open the furnace.
     */
    public static boolean onPlayerOpenScreen(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        String key = posKey(world, pos);
        SupervisedJob job = ACTIVE_JOBS.get(key);

        if (job == null) return true; // no active job, allow

        if (job.isOwner(player.getUuid())) {
            // Owner is returning
            if (job.getState() == SupervisedJob.JobState.GRACE_PERIOD) {
                job.resumeFromGrace();
                PLAYER_TO_JOB.put(player.getUuid(), key);
                KnowledgeBound.LOGGER.debug("[KnowledgeBound] {} resumed job at {}",
                        player.getName().getString(), pos);
            }
            return true;
        } else {
            // Different player — block access
            player.sendMessage(net.minecraft.text.Text.literal(
                    KnowledgeBoundConfig.INSTANCE.messages.furnaceBusy), true);
            return false;
        }
    }

    /**
     * Called when smelting/cooking completes at a furnace position.
     */
    public static void onSmeltComplete(ServerWorld world, BlockPos pos) {
        String key = posKey(world, pos);
        SupervisedJob job = ACTIVE_JOBS.get(key);
        if (job == null) return;

        job.markCompleted(job.getConfigCollectionTicks());
        KnowledgeBound.LOGGER.debug("[KnowledgeBound] Job completed at {}, collection window started", pos);
    }

    /**
     * Called when the player collects the result from a supervised furnace.
     * Performs the success roll and grants XP.
     */
    public static boolean onItemCollected(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        String key = posKey(world, pos);
        SupervisedJob job = ACTIVE_JOBS.get(key);
        if (job == null) return true; // not supervised, allow normal behavior

        if (!job.isOwner(player.getUuid())) return false; // wrong player

        // Perform success roll
        Identifier knowledgeId = job.getJobType() == SupervisedJob.JobType.SMELTING
                ? KnowledgeRegistry.SMELTING_ID
                : KnowledgeRegistry.COOKING_ID;
        int playerTier = PlayerKnowledgeManager.getTier(player, knowledgeId);
        int diff = playerTier - job.getRecipeTier();

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        KnowledgeBoundConfig.GatherFailConfig failConfig =
                job.getJobType() == SupervisedJob.JobType.SMELTING
                        ? cfg.smeltingFailChances
                        : cfg.cookingFailChances;
        double failChance = failConfig.getForTier(playerTier);

        boolean fail = new Random().nextDouble() < failChance;

        // Clean up the job
        ACTIVE_JOBS.remove(key);
        PLAYER_TO_JOB.remove(player.getUuid());

        if (fail) {
            String msg = job.getJobType() == SupervisedJob.JobType.SMELTING
                    ? cfg.messages.smeltingFail
                    : cfg.messages.cookingFail;
            player.sendMessage(net.minecraft.text.Text.literal(msg), true);
            return false; // consume the output (fail)
        }

        // Success — grant XP
        PlayerKnowledgeManager.grantMinuteIfAllowed(player, knowledgeId);
        String msg = job.getJobType() == SupervisedJob.JobType.SMELTING
                ? cfg.messages.smeltingSuccess
                : cfg.messages.cookingSuccess;
        player.sendMessage(net.minecraft.text.Text.literal(msg), true);
        return true; // allow collection
    }

    /**
     * Called when a player disconnects. Fail their active job.
     */
    public static void onPlayerDisconnect(ServerPlayerEntity player) {
        String key = PLAYER_TO_JOB.remove(player.getUuid());
        if (key == null) return;

        SupervisedJob job = ACTIVE_JOBS.remove(key);
        if (job != null) {
            handleJobFail(player, job);
        }
    }

    // --- Internal ---

    private static int getRecipeTier(SupervisedJob.JobType jobType, Identifier inputItemId) {
        Map<String, Integer> tiers = jobType == SupervisedJob.JobType.SMELTING
                ? KnowledgeBoundConfig.INSTANCE.smeltingRecipeTiers
                : new HashMap<>(); // cooking doesn't have per-recipe tiers by default
        return tiers.getOrDefault(inputItemId.toString(), 0);
    }

    private static void handleJobFail(ServerPlayerEntity player, SupervisedJob job) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;

        // Determine fail behavior
        String leaveBehaviour = job.getConfigLeaveBehaviour();
        String msg;

        if (job.getState() == SupervisedJob.JobState.COMPLETED) {
            // Collection window expired
            msg = job.getJobType() == SupervisedJob.JobType.SMELTING
                    ? cfg.messages.smeltingCollectionExpired
                    : cfg.messages.cookingCollectionExpired;
        } else {
            // Left unattended
            msg = job.getJobType() == SupervisedJob.JobType.SMELTING
                    ? cfg.messages.smeltingLeftUnattended
                    : cfg.messages.cookingLeftUnattended;
        }

        if (player != null) {
            player.sendMessage(net.minecraft.text.Text.literal(msg), true);
        }

        // Handle the furnace
        if ("FAIL".equalsIgnoreCase(leaveBehaviour)) {
            // Consume input, clear output — handled by the furnace mixin checking job state
            job.markFailed();
        } else {
            // RESET_PROGRESS — just reset cook time
            job.markFailed();
        }
    }

    private static void tick(MinecraftServer server) {
        if (ACTIVE_JOBS.isEmpty()) return;

        Iterator<Map.Entry<String, SupervisedJob>> it = ACTIVE_JOBS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SupervisedJob> entry = it.next();
            SupervisedJob job = entry.getValue();

            // Check if furnace still exists
            ServerWorld world = server.getWorld(job.getDimension());
            if (world != null) {
                BlockEntity be = world.getBlockEntity(job.getFurnacePos());
                if (!(be instanceof AbstractFurnaceBlockEntity)) {
                    // Furnace was broken or removed. Silently clean up the job.
                    PLAYER_TO_JOB.remove(job.getOwnerUuid());
                    it.remove();
                    continue;
                }
            }

            if (job.tick()) {
                // Job expired — handle failure
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(job.getOwnerUuid());
                handleJobFail(player, job);
                PLAYER_TO_JOB.remove(job.getOwnerUuid());
                it.remove();

                // Clear the furnace input/output if FAIL behavior
                clearFurnaceOnFail(server, job);
            }
        }
    }

    private static void clearFurnaceOnFail(MinecraftServer server, SupervisedJob job) {
        if (!"FAIL".equalsIgnoreCase(job.getConfigLeaveBehaviour())) return;

        ServerWorld world = server.getWorld(job.getDimension());
        if (world == null) return;

        BlockEntity be = world.getBlockEntity(job.getFurnacePos());
        if (be instanceof AbstractFurnaceBlockEntity furnace) {
            // Clear the input slot (index 0)
            furnace.setStack(0, ItemStack.EMPTY);
            // Clear the output slot (index 2)
            furnace.setStack(2, ItemStack.EMPTY);
            furnace.markDirty();
        }
    }
}
