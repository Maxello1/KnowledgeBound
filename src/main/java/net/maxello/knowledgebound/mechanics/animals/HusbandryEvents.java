package net.maxello.knowledgebound.mechanics.animals;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Handles the server tick events related to Husbandry.
 * 
 * The main thing this does is track players who are riding animals.
 * If they are riding an animal that requires a higher tier than they have, 
 * it periodically rolls a chance to kick them off (bucking them). 
 * It also grants them slow passive XP over time for successfully riding animals.
 *
 * Note: Milking and shearing are handled separately by mixins because they are triggered
 * by direct player interaction, not passive ticks.
 */
public final class HusbandryEvents {

    private static final Random RANDOM = new Random();

    /** 
     * We keep track of the exact server tick a player last received riding XP.
     * This prevents us from spamming them with XP every single tick.
     */
    private static final Map<UUID, Long> LAST_RIDING_XP_TICK = new HashMap<>();

    /** 
     * Similar to the XP tracker, this tracks when we last checked if a player
     * should be kicked off their mount. 
     */
    private static final Map<UUID, Long> LAST_RIDING_CHECK_TICK = new HashMap<>();

    private HusbandryEvents() {}

    public static void init() {
        registerRidingTick();
        KnowledgeBound.LOGGER.info("[KnowledgeBound] HusbandryEvents initialized.");
    }

    // --------------------------------------------------
    //  Riding reliability & XP (Server tick)
    // --------------------------------------------------

    private static void registerRidingTick() {
        ServerTickEvents.END_SERVER_TICK.register(HusbandryEvents::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        
        // If husbandry riding mechanics are disabled entirely, just bail out early.
        if (!cfg.husbandryEnabled || !cfg.husbandryRidingEnabled) return;

        long currentTick = server.getTicks();

        // Loop through every player currently on the server.
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Entity vehicle = player.getVehicle();
            if (vehicle == null) {
                // If they aren't riding anything, clear out their tracking data so we don't leak memory.
                LAST_RIDING_CHECK_TICK.remove(player.getUuid());
                LAST_RIDING_XP_TICK.remove(player.getUuid());
                continue;
            }

            // If they are in a boat or minecart, or an unregistered custom mount, we don't care.
            if (!AnimalTierRegistry.isRegistered(vehicle.getType())) continue;

            int requiredTier = AnimalTierRegistry.getRequiredTier(vehicle);
            int playerTier = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.HUSBANDRY_ID);

            // --- Kick-off check for low-tier riders ---
            // We scale the chance of getting kicked off based on how far below the required tier the player is.
            int tierDiff = requiredTier - playerTier;
            if (cfg.husbandryRidingUnreliableBelowTier && tierDiff > 0) {
                Long lastCheck = LAST_RIDING_CHECK_TICK.get(player.getUuid());
                
                // Only run the random check every X ticks (defined in config) so we aren't rolling RNG constantly.
                if (lastCheck == null || currentTick - lastCheck >= cfg.husbandryRidingCheckIntervalTicks) {
                    LAST_RIDING_CHECK_TICK.put(player.getUuid(), currentTick);

                    // If you're 1 tier below, it's 1x the base chance. If you're 2 tiers below, it's 2x. 
                    // Basically, don't try to ride a max tier horse if you've never ridden a pig.
                    double scaledChance = cfg.husbandryRidingKickOffChance * tierDiff;
                    if (RANDOM.nextDouble() < scaledChance) {
                        // The mount bucks them off!
                        player.stopRiding();
                        player.sendMessage(Text.literal(cfg.messages.husbandryRidingKickedOff), true);
                        continue;
                    }
                }
            }

            // --- Riding XP ---
            // If they managed to stay on the mount, we reward them with passive XP.
            // 1200 ticks = 60 seconds (assuming a solid 20 TPS).
            Long lastXpTick = LAST_RIDING_XP_TICK.get(player.getUuid());
            if (lastXpTick == null || currentTick - lastXpTick >= 1200) {
                LAST_RIDING_XP_TICK.put(player.getUuid(), currentTick);
                PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.HUSBANDRY_ID);
            }
        }
    }
}



