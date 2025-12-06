package net.maxello.knowledgebound;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnowledgeBound implements ModInitializer {

    public static final String MOD_ID = "knowledgebound";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[KnowledgeBound] Initializing…");

        // Load config before registries
        KnowledgeBoundConfig.load();

        KnowledgeRegistry.init();
        CraftingRuleRegistry.init();
        PlayerKnowledgeManager.init();
        KnowledgeEvents.init();
        KnowledgeCommands.init();
    }
}
