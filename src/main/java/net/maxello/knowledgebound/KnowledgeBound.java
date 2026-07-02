package net.maxello.knowledgebound;
import net.maxello.knowledgebound.commands.KnowledgeCommands;
import net.maxello.knowledgebound.network.KnowledgeSyncPayload;
import net.maxello.knowledgebound.mechanics.jobs.SupervisedJobManager;
import net.maxello.knowledgebound.mechanics.jobs.SlaughteringManager;
import net.maxello.knowledgebound.mechanics.animals.AnimalTierRegistry;
import net.maxello.knowledgebound.mechanics.animals.HusbandryEvents;
import net.maxello.knowledgebound.mechanics.gathering.KnowledgeEvents;
import net.maxello.knowledgebound.mechanics.gathering.OreRespawnManager;
import net.maxello.knowledgebound.mechanics.combat.ArmorRestrictionHandler;
import net.maxello.knowledgebound.mechanics.crafting.CraftingRuleRegistry;
import net.maxello.knowledgebound.config.ConfigGuiCategory;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This is the main entry point for the KnowledgeBound mod.
// When the server or single-player world starts up, this is where all of our systems get initialized.
public class KnowledgeBound implements ModInitializer {

    // Just a standard mod ID we'll use throughout the codebase to register our stuff under the right namespace.
    public static final String MOD_ID = "knowledgebound";
    // Setting up our logger so we can print messages to the server console. Always good for debugging!
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[KnowledgeBound] Initializing…");

        // We need to let the network system know about our custom payload for syncing knowledge data.
        // If we don't register this, the server won't be able to send knowledge updates to the client.
        PayloadTypeRegistry.playS2C().register(KnowledgeSyncPayload.ID, KnowledgeSyncPayload.CODEC);



        // It's super important we load the config first!
        // A lot of the registries below rely on config values (like whether certain features are enabled),
        // so if we load this too late, they might initialize with the wrong settings.
        KnowledgeBoundConfig.load();

        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        LOGGER.info("[KnowledgeBound] Config loaded — smeltingEnabled={}, cookingEnabled={}, metallurgyItems={}, cookingItems={}",
                cfg.smeltingEnabled, cfg.cookingEnabled,
                cfg.metallurgyItems != null ? cfg.metallurgyItems.size() : "NULL",
                cfg.cookingItems != null ? cfg.cookingItems.size() : "NULL");

        // Now we just go through and wake up all of our manager classes.
        // Think of this as flipping the power switches on for each separate feature of the mod.
        
        // First up, the core knowledge definitions and crafting rules.
        KnowledgeRegistry.init();
        CraftingRuleRegistry.init();
        
        // Next, the player manager which handles the actual XP and leveling logic.
        PlayerKnowledgeManager.init();
        
        // Then we hook up all the event listeners for when players try to gather, craft, or interact.
        KnowledgeEvents.init();
        KnowledgeCommands.init();
        ArmorRestrictionHandler.init();
        
        // Jobs like smelting and cooking that take time and need to be supervised.
        SupervisedJobManager.init();
        
        // Custom ore respawning mechanic.
        OreRespawnManager.init();
        
        // GUI stuff for the admin config menu.
        ConfigGuiCategory.init();
        
        // And finally, all the animal-related systems (husbandry tiers, milking, slaughtering).
        AnimalTierRegistry.init();
        HusbandryEvents.init();
        SlaughteringManager.init();
    }
}

