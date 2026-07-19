package net.maxello.knowledgebound.api;

import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.KnowledgeRegistry;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Stable, server-side integration surface for Jeweller progression.
 *
 * <p>Callers remain responsible for item validation, socket metadata, messages,
 * and for granting XP only after their transaction has completed.</p>
 */
public final class JewellerApi {

    private JewellerApi() {
    }

    public static boolean isEnabled() {
        return KnowledgeBoundConfig.INSTANCE.jewellerEnabled;
    }

    public static int getTier(ServerPlayerEntity player) {
        return PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.JEWELLER_ID);
    }

    public static int getMaximumSockets(ServerPlayerEntity player) {
        int[] limits = KnowledgeBoundConfig.INSTANCE.jewellerMaxGemsPerTier;
        if (limits == null || limits.length == 0) {
            return 0;
        }

        int tier = Math.max(0, getTier(player));
        return Math.max(0, limits[Math.min(tier, limits.length - 1)]);
    }

    public static JewellerCheckResult canApply(
            ServerPlayerEntity player,
            int requiredTier,
            int currentSockets
    ) {
        int playerTier = getTier(player);
        int normalizedRequiredTier = Math.max(0, requiredTier);
        int normalizedSockets = Math.max(0, currentSockets);
        int maximumSockets = getMaximumSockets(player);

        if (!isEnabled()) {
            return new JewellerCheckResult(
                    false,
                    FailureReason.SYSTEM_DISABLED,
                    playerTier,
                    normalizedRequiredTier,
                    normalizedSockets,
                    maximumSockets
            );
        }
        if (playerTier < normalizedRequiredTier) {
            return new JewellerCheckResult(
                    false,
                    FailureReason.TIER_TOO_LOW,
                    playerTier,
                    normalizedRequiredTier,
                    normalizedSockets,
                    maximumSockets
            );
        }
        if (normalizedSockets >= maximumSockets) {
            return new JewellerCheckResult(
                    false,
                    FailureReason.SOCKET_LIMIT_REACHED,
                    playerTier,
                    normalizedRequiredTier,
                    normalizedSockets,
                    maximumSockets
            );
        }

        return new JewellerCheckResult(
                true,
                FailureReason.ALLOWED,
                playerTier,
                normalizedRequiredTier,
                normalizedSockets,
                maximumSockets
        );
    }

    public static void grantSuccessfulJewelingXp(ServerPlayerEntity player) {
        if (isEnabled()) {
            PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.JEWELLER_ID);
        }
    }

    public enum FailureReason {
        ALLOWED,
        SYSTEM_DISABLED,
        TIER_TOO_LOW,
        SOCKET_LIMIT_REACHED
    }

    public record JewellerCheckResult(
            boolean allowed,
            FailureReason reason,
            int playerTier,
            int requiredTier,
            int currentSockets,
            int maximumSockets
    ) {
    }
}
