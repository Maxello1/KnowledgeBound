package net.maxello.knowledgebound;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Central registry mapping entity types to their required Husbandry tier.
 * Populated from config on init().
 */
public final class AnimalTierRegistry {

    private static final Map<EntityType<?>, Integer> TIER_MAP = new HashMap<>();

    private AnimalTierRegistry() {}

    public static void init() {
        TIER_MAP.clear();
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;

        for (Map.Entry<String, Integer> entry : cfg.husbandryAnimalTiers.entrySet()) {
            try {
                Identifier id = Identifier.of(entry.getKey());
                EntityType<?> type = Registries.ENTITY_TYPE.get(id);
                if (type != null) {
                    TIER_MAP.put(type, entry.getValue());
                }
            } catch (Exception e) {
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
