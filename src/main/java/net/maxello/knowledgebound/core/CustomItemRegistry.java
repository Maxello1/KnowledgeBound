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
 * 
 * Think of this class as our little item factory. Since we don't want to define brand
 * new Item classes for every little thing (which can get messy with registries),
 * we're just creating customized ItemStacks built on top of vanilla items.
 * We heavily leverage Data Components to set custom names, model overrides (for resource packs),
 * and special rules like the food effects on Royal Honey.
 */
public final class CustomItemRegistry {

    private CustomItemRegistry() {}

    /** 
     * All known custom item IDs. 
     * If you want to add a new custom item, add its ID here first.
     */
    private static final List<String> ITEM_IDS = List.of("royal_honey", "cleaver");

    /** 
     * Hand out the list of all our custom IDs. Good for things like command auto-complete. 
     */
    public static List<String> allIds() {
        return ITEM_IDS;
    }

    /**
     * Create a custom item by its internal ID.
     * We just run through a switch and call the appropriate factory method.
     * If you ask for an ID we don't know, you get null back.
     */
    public static ItemStack create(String id) {
        return switch (id) {
            case "royal_honey" -> createRoyalHoney();
            case "cleaver" -> createCleaver();
            default -> null;
        };
    }

    /** 
     * Get a human-readable display name for the item ID.
     * Mostly used if we need to print what an item is without actually creating an ItemStack.
     */
    public static String displayName(String id) {
        return switch (id) {
            case "royal_honey" -> "Royal Honey";
            case "cleaver" -> "Butcher's Cleaver";
            default -> id;
        };
    }

    /**
     * Factory for the 'Royal Honey' item.
     * 
     * This takes a regular Honey Bottle (or whatever is in the config) and totally
     * overhauls it using Data Components. We give it a shiny name, an enchant glint,
     * and hook up custom food effects so eating it gives you buffs.
     */
    public static ItemStack createRoyalHoney() {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        KnowledgeBoundConfig.BetterHoneyConfig honeyConfig = cfg.betterHoney;

        try {
            // First, figure out what the base item is. Usually it's just 'minecraft:honey_bottle',
            // but we pull it from the config so admins can change it to an apple or something if they want.
            Identifier itemId = Identifier.of(honeyConfig.itemId);
            ItemStack royalHoney = new ItemStack(Registries.ITEM.get(itemId));

            // Set up the custom name and its color.
            Formatting color = Formatting.byName(honeyConfig.nameColor);
            if (color == null) color = Formatting.GOLD; // Fallback to gold if they typo'd the color in config
            royalHoney.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal(honeyConfig.customName).formatted(color));

            // Give it that shiny enchanted apple shimmer. Looks way cooler.
            royalHoney.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

            // Set the custom model data. This is crucial for resource packs so they can target
            // this specific item and give it a unique Royal Honey texture instead of the default bottle.
            royalHoney.set(DataComponentTypes.CUSTOM_MODEL_DATA,
                    new net.minecraft.component.type.CustomModelDataComponent(cfg.royalHoneyCustomModelData));

            // Now for the tricky part: applying potion effects when eaten.
            // We have to build a list of FoodComponent.StatusEffectEntry.
            // Note: We use FoodComponent, NOT PotionContents, because PotionContents only applies to potion items.
            List<FoodComponent.StatusEffectEntry> foodEffects = new ArrayList<>();
            for (KnowledgeBoundConfig.PotionEffectEntry entry : honeyConfig.effects) {
                Identifier effectId = Identifier.of(entry.effectId);
                Optional<RegistryEntry.Reference<StatusEffect>> effectEntry =
                        Registries.STATUS_EFFECT.getEntry(effectId);
                
                // If the effect actually exists in the registry, assemble it.
                if (effectEntry.isPresent()) {
                    foodEffects.add(new FoodComponent.StatusEffectEntry(
                            new StatusEffectInstance(
                                    effectEntry.get(),
                                    entry.durationTicks,
                                    entry.amplifier
                            ),
                            1.0f // 100% chance for the effect to apply. No RNG here.
                    ));
                }
            }

            // If we actually managed to load any effects, we override the item's food component.
            if (!foodEffects.isEmpty()) {
                // We create a fresh FoodComponent but try to keep the base stats 
                // somewhat similar to a normal honey bottle.
                royalHoney.set(DataComponentTypes.FOOD, new FoodComponent(
                        6,       // nutrition (same as vanilla honey bottle)
                        0.1f,    // saturation modifier
                        true,    // canAlwaysEat - meaning you can drink it even if your hunger bar is full
                        1.6f,    // time to eat in seconds
                        Optional.of(new ItemStack(Items.GLASS_BOTTLE)), // gives back a glass bottle after drinking
                        foodEffects
                ));
            }

            return royalHoney;
        } catch (Exception e) {
            // If anything blows up (like a bad ID in the config), we catch it here.
            // Better to log a warning and return a standard honey bottle than to crash the server.
            KnowledgeBound.LOGGER.warn("[KnowledgeBound] Failed to create Royal Honey item", e);
            return new ItemStack(Items.HONEY_BOTTLE); 
        }
    }

    /**
     * Factory for the 'Butcher's Cleaver' item.
     * 
     * This is basically a customized Iron Axe that gives better odds when dissecting
     * animals. It gets a red name, a specific custom model data ID, and some lore text.
     */
    public static ItemStack createCleaver() {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;

        try {
            // We start with a plain old iron axe.
            ItemStack cleaver = new ItemStack(Items.IRON_AXE);

            // Slap a spooky red custom name on it.
            cleaver.set(DataComponentTypes.CUSTOM_NAME,
                    Text.literal("Butcher's Cleaver").formatted(Formatting.RED));

            // Set the custom model data so resource packs can make it look like a cleaver
            // rather than a standard iron axe.
            cleaver.set(DataComponentTypes.CUSTOM_MODEL_DATA,
                    new net.minecraft.component.type.CustomModelDataComponent(cfg.slaughteringCleaverCustomModelData));

            // Add some descriptive lore so the player knows what this thing actually does.
            cleaver.set(DataComponentTypes.LORE,
                    new net.minecraft.component.type.LoreComponent(
                            java.util.List.of(
                                    Text.literal("A specialized tool for slaughtering").formatted(Formatting.GRAY),
                                    Text.literal("and dissecting animal corpses.").formatted(Formatting.GRAY),
                                    Text.literal(""), // blank line for spacing
                                    Text.literal("Better dissection chances than an axe.").formatted(Formatting.GREEN)
                            )
                    ));

            return cleaver;
        } catch (Exception e) {
            // As always, catch the error and fallback to the base item to prevent crashes.
            KnowledgeBound.LOGGER.warn("[KnowledgeBound] Failed to create Cleaver item", e);
            return new ItemStack(Items.IRON_AXE); 
        }
    }
}



