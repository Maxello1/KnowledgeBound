package net.maxello.knowledgebound;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnowledgeBoundClient implements ClientModInitializer {
    public static final Logger CLIENT_LOGGER = LoggerFactory.getLogger("KnowledgeBoundClient");

    @Override
    public void onInitializeClient() {
        CLIENT_LOGGER.info("[KnowledgeBound] Client initializing…");
        // TODO: client HUD / debug overlay for knowledge tiers later
    }
}
