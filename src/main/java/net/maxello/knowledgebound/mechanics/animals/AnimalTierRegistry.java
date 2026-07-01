package net.maxello.knowledgebound.mechanics.animals;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * This class keeps track of which animals require which Husbandry tier to ride.
 * 
 * Instead of hardcoding "horses require tier 1, pigs require tier 2", we load this
 * dynamically from the mod's configuration file during initialization. This lets
 * server admins decide exactly how hard it should be to ride specific animals or
 * custom modded creatures.
 */
public final class AnimalTierRegistry {

    private static final Map<EntityType<?>, Integer> TIER_MAP = new HashMap<>();

    private AnimalTierRegistry() {}

    public static void init() {
        TIER_MAP.clear();
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;

        // Loop through all the animal tiers defined in the config file.
        for (Map.Entry<String, Integer> entry : cfg.husbandryAnimalTiers.entrySet()) {
            try {
                // Convert the string ID (like "minecraft:horse") into an Identifier,
                // and then look it up in the vanilla entity registry.
                Identifier id = Identifier.of(entry.getKey());
                EntityType<?> type = Registries.ENTITY_TYPE.get(id);
                
                // If it actually exists in the game, we record its required tier.
                if (type != null) {
                    TIER_MAP.put(type, entry.getValue());
                }
            } catch (Exception e) {
                // If they typo'd something in the config, log a warning but don't crash.
                KnowledgeBound.LOGGER.warn("[KnowledgeBound] Invalid husbandryAnimalTiers id: {}", entry.getKey());
            }
        }

        KnowledgeBound.LOGGER.info("[KnowledgeBound] AnimalTierRegistry loaded {} animal tiers.", TIER_MAP.size());
    }

    /**
     * Get the required Husbandry tier for the given entity type.
     * Returns 0 if the entity type is not registered (default = no restriction).
     */
    public static int getRequiredTier(EntityType<?> type) {
        return TIER_MAP.getOrDefault(type, 0);
    }

    /**
     * Get the required Husbandry tier for the given entity.
     */
    public static int getRequiredTier(Entity entity) {
        return getRequiredTier(entity.getType());
    }

    /**
     * Check if the given entity type has a registered tier.
     */
    public static boolean isRegistered(EntityType<?> type) {
        return TIER_MAP.containsKey(type);
    }
}



