package net.maxello.knowledgebound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GatherFailConfigTest {

    private final KnowledgeBoundConfig.GatherFailConfig cfg =
            new KnowledgeBoundConfig.GatherFailConfig(0.40, 0.25, 0.10, 0.05, 0.02);

    @Test
    void getForTier_tier0_returnsHighestFail() {
        assertEquals(0.40, cfg.getForTier(0), 1e-9);
    }

    @Test
    void getForTier_tier4_returnsLowestFail() {
        assertEquals(0.02, cfg.getForTier(4), 1e-9);
    }

    @Test
    void getForTier_negativeTier_clampedToZero() {
        assertEquals(0.40, cfg.getForTier(-5), 1e-9);
    }

    @Test
    void getForTier_tierAboveMax_clampedToFour() {
        assertEquals(0.02, cfg.getForTier(99), 1e-9);
    }

    @Test
    void getForTier_allIntermediateTiers_returnsCorrectValues() {
        assertEquals(0.25, cfg.getForTier(1), 1e-9);
        assertEquals(0.10, cfg.getForTier(2), 1e-9);
        assertEquals(0.05, cfg.getForTier(3), 1e-9);
    }

    @Test
    void defaultForestryConfig_hasFiveDescendingTiers() {
        var forestry = new KnowledgeBoundConfig().forestryGatherFail;
        double prev = Double.MAX_VALUE;
        for (int t = 0; t <= 4; t++) {
            double chance = forestry.getForTier(t);
            assertTrue(chance <= prev, "Fail chance should decrease as tier increases (tier " + t + ")");
            prev = chance;
        }
    }
}
