package net.maxello.knowledgebound;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class KnowledgeBoundConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static KnowledgeBoundConfig INSTANCE = new KnowledgeBoundConfig();

    /** Multiplier for minutes required per tier (1.0 = default, 2.0 = twice as slow, 0.5 = twice as fast). */
    public double minutesMultiplier = 1.0;

    /** Extra block IDs that should count for Forestry XP (e.g. "mytreesmod:ancient_log"). */
    public List<String> extraForestryBlocks = new ArrayList<>();
    public List<String> extraMiningBlocks = new ArrayList<>();
    public List<String> extraDiggingBlocks = new ArrayList<>();
    public List<String> extraFarmingBlocks = new ArrayList<>();

    /** Extra item IDs that should use the wooden-tool rule. */
    public List<String> extraToolItems = new ArrayList<>();
    /** Extra item IDs that should use the armor rule. */
    public List<String> extraArmorItems = new ArrayList<>();
    /** Reserved for future weapon rules, but already in config. */
    public List<String> extraWeaponItems = new ArrayList<>();

    public static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path path = configDir.resolve("knowledgebound.json");

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                INSTANCE = GSON.fromJson(reader, KnowledgeBoundConfig.class);
                KnowledgeBound.LOGGER.info("[KnowledgeBound] Loaded config from {}", path);
            } catch (IOException e) {
                KnowledgeBound.LOGGER.error("[KnowledgeBound] Failed to load config, using defaults.", e);
            }
        } else {
            // Create default config file
            try {
                Files.createDirectories(configDir);
                try (Writer writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(INSTANCE, writer);
                }
                KnowledgeBound.LOGGER.info("[KnowledgeBound] Created default config at {}", path);
            } catch (IOException e) {
                KnowledgeBound.LOGGER.error("[KnowledgeBound] Failed to write default config.", e);
            }
        }
    }
}
