package net.maxello.knowledgebound.core;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Registry of custom KnowledgeBound items that can be created on demand.
 * Used by both the BeehiveMixin (for Royal Honey drops) and the /kb give command.
 */
public final class CustomItemRegistry {

    private CustomItemRegistry() {}

    /** All known custom item IDs. */
    private static final List<String> ITEM_IDS = List.of("royal_honey", "cleaver");

    /** Get all registered custom item IDs. */
    public static List<String> allIds() {
        return ITEM_IDS;
    }

    /**
     * Create a custom item by its KnowledgeBound ID.
     * Returns null if the ID is unknown.
     */
    public static ItemStack create(String id) {
        return switch (id) {
            case "royal_honey" -> createRoyalHoney();
            case "cleaver" -> createCleaver();
            default -> null;
        };
    }

    /** Get a human-readable name for a custom item ID. */
    public static String displayName(String id) {
        return switch (id) {
            case "royal_honey" -> "Royal Honey";
            case "cleaver" -> "Butcher's Cleaver";
            default -> id;
        };
    }

    /**
     * Creates a Royal Honey item with enchant glint and food effects from config.
     */
    public static ItemStack createRoyalHoney() {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        KnowledgeBoundConfig.BetterHoneyConfig honeyConfig = cfg.betterHoney;

        try {
            Identifier itemId = Identifier.of(honeyConfig.itemId);
            ItemStack royalHoney = new ItemStack(Registries.ITEM.get(itemId));

            // custom name with color
            Formatting color = Formatting.byName(honeyConfig.nameColor);
            if (color == null) color = Formatting.GOLD;
            royalHoney.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(honeyConfig.customName).formatted(color));

            // add enchantment glint (shimmer effect like enchanted golden apple)
            royalHoney.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

            // custom model data so resource packs can give it a unique texture
            royalHoney.set(DataComponentTypes.CUSTOM_MODEL_DATA,
                    new net.minecraft.component.type.CustomModelDataComponent(cfg.royalHoneyCustomModelData));

            // apply potion effects via FoodComponent (not PotionContents — that only works on potions)
            List<FoodComponent.StatusEffectEntry> foodEffects = new ArrayList<>();
            for (KnowledgeBoundConfig.PotionEffectEntry entry : honeyConfig.effects) {
                Identifier effectId = Identifier.of(entry.effectId);
                Optional<RegistryEntry.Reference<StatusEffect>> effectEntry =
                        Registries.STATUS_EFFECT.getEntry(effectId);
                if (effectEntry.isPresent()) {
                    foodEffects.add(new FoodComponent.StatusEffectEntry(
                            new StatusEffectInstance(
                                    effectEntry.get(),
                                    entry.durationTicks,
                                    entry.amplifier
                            ),
                            1.0f // 100% chance to apply
                    ));
                }
            }

            if (!foodEffects.isEmpty()) {
                // Create a new FoodComponent with the same base stats as honey_bottle
                // but with our custom effects added
                royalHoney.set(DataComponentTypes.FOOD, new FoodComponent(
                        6,       // nutrition (same as honey bottle)
                        0.1f,    // saturation modifier
                        true,    // canAlwaysEat
                        1.6f,    // eatSeconds
                        Optional.of(new ItemStack(Items.GLASS_BOTTLE)), // usingConvertsTo
                        foodEffects
                ));
            }

            return royalHoney;
        } catch (Exception e) {
            KnowledgeBound.LOGGER.warn("[KnowledgeBound] Failed to create Royal Honey item", e);
            return new ItemStack(Items.HONEY_BOTTLE); // fallback
        }
    }

    /**
     * Creates a Butcher's Cleaver item based on an Iron Axe with custom model data.
     */
    public static ItemStack createCleaver() {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;

        try {
            ItemStack cleaver = new ItemStack(Items.IRON_AXE);

            // Custom name
            cleaver.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("Butcher's Cleaver").formatted(Formatting.RED));

            // Custom model data for resource pack textures
            cleaver.set(DataComponentTypes.CUSTOM_MODEL_DATA,
                    new net.minecraft.component.type.CustomModelDataComponent(cfg.slaughteringCleaverCustomModelData));

            // Add lore
            cleaver.set(DataComponentTypes.LORE,
                    new net.minecraft.component.type.LoreComponent(
                            java.util.List.of(
                                    Text.literal("A specialized tool for slaughtering").formatted(Formatting.GRAY),
                                    Text.literal("and dissecting animal corpses.").formatted(Formatting.GRAY),
                                    Text.literal(""),
                                    Text.literal("Better dissection chances than an axe.").formatted(Formatting.GREEN)
                            )
                    ));

            return cleaver;
        } catch (Exception e) {
            KnowledgeBound.LOGGER.warn("[KnowledgeBound] Failed to create Cleaver item", e);
            return new ItemStack(Items.IRON_AXE); // fallback
        }
    }
}



