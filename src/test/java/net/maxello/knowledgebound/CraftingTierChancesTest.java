package net.maxello.knowledgebound;

import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CraftingTierChancesTest {

    @Test
    void normalize_alreadyNormalized_unchanged() {
        var tc = new KnowledgeBoundConfig.CraftingTierChances(0.10, 0.15, 0.75);
        tc.normalize();
        assertEquals(0.10, tc.failChance,   1e-9);
        assertEquals(0.15, tc.poorChance,   1e-9);
        assertEquals(0.75, tc.normalChance, 1e-9);
    }

    @Test
    void normalize_unnormalizedValues_sumToOne() {
        var tc = new KnowledgeBoundConfig.CraftingTierChances(2.0, 2.0, 6.0);
        tc.normalize();
        assertEquals(1.0, tc.failChance + tc.poorChance + tc.normalChance, 1e-9);
        assertEquals(0.2, tc.failChance,   1e-9);
        assertEquals(0.2, tc.poorChance,   1e-9);
        assertEquals(0.6, tc.normalChance, 1e-9);
    }

    @Test
    void normalize_allZero_fallsBackToNormal() {
        var tc = new KnowledgeBoundConfig.CraftingTierChances(0.0, 0.0, 0.0);
        tc.normalize();
        assertEquals(0.0, tc.failChance,   1e-9);
        assertEquals(0.0, tc.poorChance,   1e-9);
        assertEquals(1.0, tc.normalChance, 1e-9);
    }

    @Test
    void normalize_onlyFail_normalChanceBecomesZero() {
        var tc = new KnowledgeBoundConfig.CraftingTierChances(1.0, 0.0, 0.0);
        tc.normalize();
        assertEquals(1.0, tc.failChance,   1e-9);
        assertEquals(0.0, tc.poorChance,   1e-9);
        assertEquals(0.0, tc.normalChance, 1e-9);
    }

    @Test
    void getCraftingChancesForDiff_extremeNegative_mapsToIndex0() {
        var cfg = new KnowledgeBoundConfig();
        var tc = cfg.getCraftingChancesForDiff(-5);
        assertEquals(1.00, tc.failChance,   1e-9, "diff <= -3 should be 100% fail");
        assertEquals(0.00, tc.poorChance,   1e-9);
        assertEquals(0.00, tc.normalChance, 1e-9);
    }

    @Test
    void getCraftingChancesForDiff_diffMinusTwo_highFail() {
        var cfg = new KnowledgeBoundConfig();
        var tc = cfg.getCraftingChancesForDiff(-2);
        assertEquals(0.85, tc.failChance,   1e-9);
        assertEquals(0.12, tc.poorChance,   1e-9);
        assertEquals(0.03, tc.normalChance, 1e-9);
    }

    @Test
    void getCraftingChancesForDiff_diffMinusOne_challenging() {
        var cfg = new KnowledgeBoundConfig();
        var tc = cfg.getCraftingChancesForDiff(-1);
        assertEquals(0.45, tc.failChance,   1e-9);
        assertEquals(0.35, tc.poorChance,   1e-9);
        assertEquals(0.20, tc.normalChance, 1e-9);
    }

    @Test
    void getCraftingChancesForDiff_diffZero_atLevel() {
        var cfg = new KnowledgeBoundConfig();
        var tc = cfg.getCraftingChancesForDiff(0);
        assertEquals(0.10, tc.failChance,   1e-9);
        assertEquals(0.15, tc.poorChance,   1e-9);
        assertEquals(0.75, tc.normalChance, 1e-9);
    }

    @Test
    void getCraftingChancesForDiff_diffPlusOne_easy() {
        var cfg = new KnowledgeBoundConfig();
        var tc = cfg.getCraftingChancesForDiff(1);
        assertEquals(0.00, tc.failChance,   1e-9);
        assertEquals(0.08, tc.poorChance,   1e-9);
        assertEquals(0.92, tc.normalChance, 1e-9);
    }

    @Test
    void getCraftingChancesForDiff_diffPlusTwo_trivial() {
        var cfg = new KnowledgeBoundConfig();
        var tc = cfg.getCraftingChancesForDiff(2);
        assertEquals(0.00, tc.failChance,   1e-9);
        assertEquals(0.00, tc.poorChance,   1e-9);
        assertEquals(1.00, tc.normalChance, 1e-9);
    }

    @Test
    void getCraftingChancesForDiff_largePlusDiff_trivial() {
        var cfg = new KnowledgeBoundConfig();
        var tc = cfg.getCraftingChancesForDiff(10);
        assertEquals(1.00, tc.normalChance, 1e-9);
    }
}

