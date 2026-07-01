package net.maxello.knowledgebound.mechanics.jobs;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;
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
 * The big boss that keeps track of everyone staring at furnaces.
 * 
 * When a player puts an iron ore into a furnace, this manager creates a `SupervisedJob`
 * and starts watching them. It checks if they walk away, checks if they grab the item
 * in time, and ultimately rolls the dice to see if they successfully smelted the item
 * or if they ruined it.
 * 
 * Note: Jobs are not saved to the hard drive. If the server crashes or restarts,
 * all currently smelting items are considered abandoned and will fail.
 */
public final class SupervisedJobManager {

    private SupervisedJobManager() {}

    /** 
     * All the jobs currently happening on the server.
     * We use a string like "minecraft:overworld:10,64,-20" to make sure we don't
     * confuse a furnace in the nether with one in the overworld at the same coords.
     */
    private static final Map<String, SupervisedJob> ACTIVE_JOBS = new HashMap<>();

    /** 
     * A quick lookup table to find out which furnace a specific player is currently using. 
     * You can only supervise one furnace at a time!
     */
    private static final Map<UUID, String> PLAYER_TO_JOB = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(SupervisedJobManager::tick);
        KnowledgeBound.LOGGER.info("[KnowledgeBound] SupervisedJobManager initialized.");
    }

    // --- Key helpers ---

    /** Makes our unique string identifier for a specific block in a specific dimension. */
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
     * Is someone currently babysitting this furnace?
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
     * Is this item something that requires the Smelting job?
     * e.g. Iron ore yes, Cobblestone no.
     */
    public static boolean isMetallurgyItem(Identifier itemId) {
        String id = itemId.toString();
        List<String> configItems = KnowledgeBoundConfig.INSTANCE.metallurgyItems;
        if (configItems != null && !configItems.isEmpty()) {
            return configItems.contains(id);
        }
        return DEFAULT_METALLURGY_ITEMS.contains(id);
    }

    private static final Set<String> DEFAULT_COOKING_ITEMS = Set.of(
            "minecraft:beef", "minecraft:porkchop", "minecraft:chicken",
            "minecraft:mutton", "minecraft:rabbit", "minecraft:cod",
            "minecraft:salmon", "minecraft:potato", "minecraft:kelp"
    );

    /**
     * Is this item something that requires the Cooking job?
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
     * Figure out what kind of job this item triggers, or null if it's just a normal
     * item that anyone can smelt without paying attention (like sand into glass).
     */
    public static SupervisedJob.JobType getJobTypeForItem(Identifier itemId) {
        if (isMetallurgyItem(itemId)) return SupervisedJob.JobType.SMELTING;
        if (isCookingItem(itemId)) return SupervisedJob.JobType.COOKING;
        return null;
    }

    /**
     * A player just put a tracked item into a furnace. Let's start the timer!
     */
    public static SupervisedJob startJob(ServerPlayerEntity player, ServerWorld world,
                                          BlockPos furnacePos, SupervisedJob.JobType jobType,
                                          Identifier inputItemId) {
        String key = posKey(world, furnacePos);

        // Can't start a job if someone else is already smelting something here!
        if (ACTIVE_JOBS.containsKey(key)) return null;

        // Figure out how hard this item is to smelt.
        int recipeTier = getRecipeTier(jobType, inputItemId);

        // Does the player even know how to do this?
        Identifier knowledgeId = jobType == SupervisedJob.JobType.SMELTING
                ? KnowledgeRegistry.SMELTING_ID
                : KnowledgeRegistry.COOKING_ID;
        int playerTier = PlayerKnowledgeManager.getTier(player, knowledgeId);

        if (playerTier < recipeTier) {
            // Nope, they are too low level.
            String msg = jobType == SupervisedJob.JobType.SMELTING
                    ? KnowledgeBoundConfig.INSTANCE.messages.smeltingTierLocked
                    : KnowledgeBoundConfig.INSTANCE.messages.smeltingTierLocked; // reuse for now
            msg = msg.replace("{minTier}", String.valueOf(recipeTier));
            player.sendMessage(net.minecraft.text.Text.literal(msg), true);
            return null;
        }

        // Everything looks good, create the job.
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
     * The player closed the furnace UI. 
     * We don't fail them immediately, because maybe they just quickly checked their inventory.
     * But we do start the countdown!
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
     * Called when a player opens a furnace screen. 
     * If they were in the grace period, they are saved!
     * Returns true if they are allowed to open it, false if it's someone else's furnace.
     */
    public static boolean onPlayerOpenScreen(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        String key = posKey(world, pos);
        SupervisedJob job = ACTIVE_JOBS.get(key);

        if (job == null) return true; // It's just a normal furnace, let them open it.

        if (job.isOwner(player.getUuid())) {
            // It's their furnace! Welcome back.
            if (job.getState() == SupervisedJob.JobState.GRACE_PERIOD) {
                job.resumeFromGrace();
                PLAYER_TO_JOB.put(player.getUuid(), key);
                KnowledgeBound.LOGGER.debug("[KnowledgeBound] {} resumed job at {}",
                        player.getName().getString(), pos);
            }
            return true;
        } else {
            // Hey, get out of there, that's not yours!
            player.sendMessage(net.minecraft.text.Text.literal(
                    KnowledgeBoundConfig.INSTANCE.messages.furnaceBusy), true);
            return false;
        }
    }

    /**
     * The little white arrow in the furnace UI reached 100%. 
     * The item is done, but they have to pull it out quickly!
     */
    public static void onSmeltComplete(ServerWorld world, BlockPos pos) {
        String key = posKey(world, pos);
        SupervisedJob job = ACTIVE_JOBS.get(key);
        if (job == null) return;

        job.markCompleted(job.getConfigCollectionTicks());
        KnowledgeBound.LOGGER.debug("[KnowledgeBound] Job completed at {}, collection window started", pos);
    }

    /**
     * They clicked the output slot to grab their shiny new ingot or steak!
     * Now we roll the dice to see if they actually did a good job, or if they
     * messed up the temperature and ruined it at the last second.
     */
    public static boolean onItemCollected(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        String key = posKey(world, pos);
        SupervisedJob job = ACTIVE_JOBS.get(key);
        if (job == null) return true; // not supervised, allow normal behavior

        if (!job.isOwner(player.getUuid())) return false; // wrong player

        // Calculate their chances based on their tier vs the recipe tier.
        Identifier knowledgeId = job.getJobType() == SupervisedJob.JobType.SMELTING
                ? KnowledgeRegistry.SMELTING_ID
                : KnowledgeRegistry.COOKING_ID;
        int playerTier = PlayerKnowledgeManager.getTier(player, knowledgeId);
        
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        KnowledgeBoundConfig.GatherFailConfig failConfig =
                job.getJobType() == SupervisedJob.JobType.SMELTING
                        ? cfg.smeltingFailChances
                        : cfg.cookingFailChances;
        
        double failChance = failConfig.getForTier(playerTier);
        boolean fail = new Random().nextDouble() < failChance;

        // Either way, the job is over now. Clean up.
        ACTIVE_JOBS.remove(key);
        PLAYER_TO_JOB.remove(player.getUuid());

        if (fail) {
            // Oh no, they burned it!
            String msg = job.getJobType() == SupervisedJob.JobType.SMELTING
                    ? cfg.messages.smeltingFail
                    : cfg.messages.cookingFail;
            player.sendMessage(net.minecraft.text.Text.literal(msg), true);
            return false; // returning false tells our mixin to delete the item from their mouse
        }

        // Success! Give them their item and some XP.
        PlayerKnowledgeManager.grantMinuteIfAllowed(player, knowledgeId);
        String msg = job.getJobType() == SupervisedJob.JobType.SMELTING
                ? cfg.messages.smeltingSuccess
                : cfg.messages.cookingSuccess;
        player.sendMessage(net.minecraft.text.Text.literal(msg), true);
        
        return true; // allow collection
    }

    /**
     * If they log out while smelting, the job is instantly abandoned and ruined.
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

    /**
     * Looks up how hard a specific item is to smelt.
     */
    private static int getRecipeTier(SupervisedJob.JobType jobType, Identifier inputItemId) {
        Map<String, Integer> tiers = jobType == SupervisedJob.JobType.SMELTING
                ? KnowledgeBoundConfig.INSTANCE.smeltingRecipeTiers
                : new HashMap<>(); // cooking doesn't have per-recipe tiers by default
        return tiers.getOrDefault(inputItemId.toString(), 0);
    }

    /**
     * Punish the player for failing a job (walking away, logging out, taking too long).
     */
    private static void handleJobFail(ServerPlayerEntity player, SupervisedJob job) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;

        // Figure out what message to show them based on how they failed.
        String leaveBehaviour = job.getConfigLeaveBehaviour();
        String msg;

        if (job.getState() == SupervisedJob.JobState.COMPLETED) {
            // Collection window expired (they didn't grab the item fast enough)
            msg = job.getJobType() == SupervisedJob.JobType.SMELTING
                    ? cfg.messages.smeltingCollectionExpired
                    : cfg.messages.cookingCollectionExpired;
        } else {
            // Left unattended (they walked away or closed the UI for too long)
            msg = job.getJobType() == SupervisedJob.JobType.SMELTING
                    ? cfg.messages.smeltingLeftUnattended
                    : cfg.messages.cookingLeftUnattended;
        }

        if (player != null) {
            player.sendMessage(net.minecraft.text.Text.literal(msg), true);
        }

        // Mark the internal state so the mixin knows to delete the items.
        if ("FAIL".equalsIgnoreCase(leaveBehaviour)) {
            // We are going to literally delete the raw materials inside the furnace.
            job.markFailed();
        } else {
            // We are just going to reset the cooking progress back to 0%.
            job.markFailed();
        }
    }

    /**
     * The main loop that runs every tick.
     * We use this to tick down the timers for all active jobs and see if anyone failed.
     */
    private static void tick(MinecraftServer server) {
        if (ACTIVE_JOBS.isEmpty()) return;

        Iterator<Map.Entry<String, SupervisedJob>> it = ACTIVE_JOBS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SupervisedJob> entry = it.next();
            SupervisedJob job = entry.getValue();

            // First, make sure the furnace didn't literally get blown up by a creeper.
            ServerWorld world = server.getWorld(job.getDimension());
            if (world != null) {
                BlockEntity be = world.getBlockEntity(job.getFurnacePos());
                if (!(be instanceof AbstractFurnaceBlockEntity)) {
                    // Furnace is gone! Just silently forget the job.
                    PLAYER_TO_JOB.remove(job.getOwnerUuid());
                    it.remove();
                    continue;
                }
            }

            // Tick the timers!
            if (job.tick()) {
                // Returns true if the timer hit 0 and they failed.
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(job.getOwnerUuid());
                handleJobFail(player, job);
                PLAYER_TO_JOB.remove(job.getOwnerUuid());
                it.remove();

                // If the config says we should destroy the items on failure, do it now.
                clearFurnaceOnFail(server, job);
            }
        }
    }

    /**
     * Empties out the furnace completely as punishment for walking away.
     */
    private static void clearFurnaceOnFail(MinecraftServer server, SupervisedJob job) {
        if (!"FAIL".equalsIgnoreCase(job.getConfigLeaveBehaviour())) return;

        ServerWorld world = server.getWorld(job.getDimension());
        if (world == null) return;

        BlockEntity be = world.getBlockEntity(job.getFurnacePos());
        if (be instanceof AbstractFurnaceBlockEntity furnace) {
            // Clear the raw material slot
            furnace.setStack(0, ItemStack.EMPTY);
            // Clear the finished output slot
            furnace.setStack(2, ItemStack.EMPTY);
            furnace.markDirty();
        }
    }
}



