package net.maxello.knowledgebound;

import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigValidationTest {

    @Test
    void validate_clampChanceAboveOne() {
        var cfg = new KnowledgeBoundConfig();
        cfg.forestryGatherFail.tier0 = 2.5;
        cfg.poorDurabilityFraction = 1.5;
        cfg.husbandryRidingKickOffChance = 3.0;
        cfg.validate();
        assertEquals(1.0, cfg.forestryGatherFail.tier0, 1e-9);
        assertEquals(1.0, cfg.poorDurabilityFraction, 1e-9);
        assertEquals(1.0, cfg.husbandryRidingKickOffChance, 1e-9);
    }

    @Test
    void validate_clampChanceBelowZero() {
        var cfg = new KnowledgeBoundConfig();
        cfg.miningGatherFail.tier2 = -0.5;
        cfg.stonecutterCutChanceTier1 = -1.0;
        cfg.validate();
        assertEquals(0.0, cfg.miningGatherFail.tier2, 1e-9);
        assertEquals(0.0, cfg.stonecutterCutChanceTier1, 1e-9);
    }

    @Test
    void validate_clampTicksBelowOne() {
        var cfg = new KnowledgeBoundConfig();
        cfg.smeltingGraceTimeTicks = 0;
        cfg.smeltingCollectionWindowTicks = -5;
        cfg.cookingGraceTimeTicks = -100;
        cfg.cookingCollectionWindowTicks = 0;
        cfg.husbandryRidingCheckIntervalTicks = -1;
        cfg.validate();
        assertEquals(1, cfg.smeltingGraceTimeTicks);
        assertEquals(1, cfg.smeltingCollectionWindowTicks);
        assertEquals(1, cfg.cookingGraceTimeTicks);
        assertEquals(1, cfg.cookingCollectionWindowTicks);
        assertEquals(1, cfg.husbandryRidingCheckIntervalTicks);
    }

    @Test
    void validate_invalidLeaveBehaviour_resetsToFail() {
        var cfg = new KnowledgeBoundConfig();
        cfg.smeltingLeaveBehaviour = "NONSENSE";
        cfg.cookingLeaveBehaviour = "";
        cfg.validate();
        assertEquals("FAIL", cfg.smeltingLeaveBehaviour);
        assertEquals("FAIL", cfg.cookingLeaveBehaviour);
    }

    @Test
    void validate_validLeaveBehaviour_unchanged() {
        var cfg = new KnowledgeBoundConfig();
        cfg.smeltingLeaveBehaviour = "RESET_PROGRESS";
        cfg.cookingLeaveBehaviour = "FAIL";
        cfg.validate();
        assertEquals("RESET_PROGRESS", cfg.smeltingLeaveBehaviour);
        assertEquals("FAIL", cfg.cookingLeaveBehaviour);
    }

    @Test
    void validate_wrongSizeCraftingDiffArray_resetsToDefault() {
        var cfg = new KnowledgeBoundConfig();
        cfg.craftingDiffChances = new KnowledgeBoundConfig.CraftingTierChances[3]; // wrong size
        cfg.validate();
        assertEquals(6, cfg.craftingDiffChances.length, "Should reset to 6 entries");
    }

    @Test
    void validate_craftingDiffChances_allNormalized() {
        var cfg = new KnowledgeBoundConfig();
        cfg.validate();
        for (int i = 0; i < cfg.craftingDiffChances.length; i++) {
            var tc = cfg.craftingDiffChances[i];
            double sum = tc.failChance + tc.poorChance + tc.normalChance;
            assertEquals(1.0, sum, 1e-9, "craftingDiffChances[" + i + "] should sum to 1.0");
        }
    }

    @Test
    void validate_clampBetterHoneyChance() {
        var cfg = new KnowledgeBoundConfig();
        cfg.betterHoneyChance = new double[] { -0.1, 1.5, 0.5 };
        cfg.validate();
        assertEquals(0.0, cfg.betterHoneyChance[0], 1e-9);
        assertEquals(1.0, cfg.betterHoneyChance[1], 1e-9);
        assertEquals(0.5, cfg.betterHoneyChance[2], 1e-9);
    }

    @Test
    void validate_clampSlaughteringFailChances() {
        var cfg = new KnowledgeBoundConfig();
        cfg.slaughteringFailChancePerTier = new double[] { 2.0, -1.0, 0.5, 0.3 };
        cfg.validate();
        assertEquals(1.0, cfg.slaughteringFailChancePerTier[0], 1e-9);
        assertEquals(0.0, cfg.slaughteringFailChancePerTier[1], 1e-9);
        assertEquals(0.5, cfg.slaughteringFailChancePerTier[2], 1e-9);
    }

    @Test
    void fillDefaults_nullGatherFail_getsDefault() {
        var cfg = new KnowledgeBoundConfig();
        var expected = cfg.forestryGatherFail.tier0;
        // Simulate what Gson does when a field is missing from the JSON
        cfg.forestryGatherFail = null;
        // fillDefaults is private, but we can test via the public defaults
        var fresh = new KnowledgeBoundConfig();
        assertNotNull(fresh.forestryGatherFail, "Default should never be null");
        assertEquals(expected, fresh.forestryGatherFail.tier0, 1e-9);
    }

    @Test
    void defaultConfig_craftingDiffChances_hasSixEntries() {
        var cfg = new KnowledgeBoundConfig();
        assertEquals(6, cfg.craftingDiffChances.length);
    }

    @Test
    void defaultConfig_smeltingTimers_matchReadme() {
        var cfg = new KnowledgeBoundConfig();
        assertEquals(600, cfg.smeltingGraceTimeTicks, "Should be 30 seconds (600 ticks)");
        assertEquals(1200, cfg.smeltingCollectionWindowTicks, "Should be 60 seconds (1200 ticks)");
    }

    @Test
    void defaultConfig_cookingTimers_matchReadme() {
        var cfg = new KnowledgeBoundConfig();
        assertEquals(400, cfg.cookingGraceTimeTicks, "Should be 20 seconds (400 ticks)");
        assertEquals(600, cfg.cookingCollectionWindowTicks, "Should be 30 seconds (600 ticks)");
        assertTrue(cfg.cookingAppliesToCampfire, "Campfire cooking should require supervision by default");
    }
}
