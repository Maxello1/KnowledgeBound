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
 * Handles Husbandry event hooks:
 * - Riding reliability & XP via server tick
 *
 * Milking and shearing are handled by MilkingMixin and ShearingMixin respectively.
 */
public final class HusbandryEvents {

    private static final Random RANDOM = new Random();

    /** Tracks last tick a riding XP was granted per player UUID. */
    private static final Map<UUID, Long> LAST_RIDING_XP_TICK = new HashMap<>();

    /** Tracks the last tick a riding kick-off check was performed per player. */
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
        if (!cfg.husbandryEnabled || !cfg.husbandryRidingEnabled) return;

        long currentTick = server.getTicks();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Entity vehicle = player.getVehicle();
            if (vehicle == null) {
                // Player not riding — clean up tracking
                LAST_RIDING_CHECK_TICK.remove(player.getUuid());
                LAST_RIDING_XP_TICK.remove(player.getUuid());
                continue;
            }

            // Only track animal mounts registered in the tier system
            if (!AnimalTierRegistry.isRegistered(vehicle.getType())) continue;

            int requiredTier = AnimalTierRegistry.getRequiredTier(vehicle);
            int playerTier = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.HUSBANDRY_ID);

            // --- Kick-off check for low-tier riders ---
            // Scale chance by how far below the required tier the player is
            int tierDiff = requiredTier - playerTier;
            if (cfg.husbandryRidingUnreliableBelowTier && tierDiff > 0) {
                Long lastCheck = LAST_RIDING_CHECK_TICK.get(player.getUuid());
                if (lastCheck == null || currentTick - lastCheck >= cfg.husbandryRidingCheckIntervalTicks) {
                    LAST_RIDING_CHECK_TICK.put(player.getUuid(), currentTick);

                    // Kick-off chance scales with tier difference:
                    // tierDiff=1 → 1x base, tierDiff=2 → 2x base, etc.
                    double scaledChance = cfg.husbandryRidingKickOffChance * tierDiff;
                    if (RANDOM.nextDouble() < scaledChance) {
                        player.stopRiding();
                        player.sendMessage(Text.literal(cfg.messages.husbandryRidingKickedOff), true);
                        continue;
                    }
                }
            }

            // --- Riding XP (every 1200 ticks = 60 seconds) ---
            Long lastXpTick = LAST_RIDING_XP_TICK.get(player.getUuid());
            if (lastXpTick == null || currentTick - lastXpTick >= 1200) {
                LAST_RIDING_XP_TICK.put(player.getUuid(), currentTick);
                PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.HUSBANDRY_ID);
            }
        }
    }
}



