package net.maxello.knowledgebound;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

/**
 * Manages the slaughtering system:
 * - Detects kills with a cleaver and spawns corpse entities
 * - Handles right-click dissection of corpses with fail/poor/normal/excellent outcomes
 * - Stores persistent metadata on corpse entities for server restart safety
 * - Supports non-cleaver partial loot drop chance
 * - Awards Slaughtering knowledge XP
 */
public final class SlaughteringManager {

    private SlaughteringManager() {}

    private static final Random RANDOM = new Random();

    /** Stores whether the last attack on this thread was a critical hit. */
    public static final ThreadLocal<Boolean> LAST_ATTACK_WAS_CRIT = ThreadLocal.withInitial(() -> false);

    /** NBT/scoreboard tag used to identify corpse entities. */
    public static final String CORPSE_TAG = "kb_corpse";

    /** Persistent tag prefixes stored directly on the entity commandTags for restart safety. */
    private static final String TAG_DESPAWN_TIME_PREFIX = "kb_despawn:";
    private static final String TAG_LOOT_TABLE_PREFIX = "kb_loot:";

    public static void init() {
        registerDeathListener();
        registerDissectionInteraction();
        registerNonCleaverLootSuppression();
        KnowledgeBound.LOGGER.info("[KnowledgeBound] SlaughteringManager initialized.");
    }

    // --------------------------------------------------
    //  Death listener — spawn corpse on valid kill
    // --------------------------------------------------

    private static void registerDeathListener() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
            if (!cfg.slaughteringEnabled) return;

            // Only care about kills by players
            if (!(damageSource.getAttacker() instanceof ServerPlayerEntity player)) return;
            if (!(entity.getWorld() instanceof ServerWorld serverWorld)) return;

            // Skip if the entity is already a corpse
            if (entity.getCommandTags().contains(CORPSE_TAG)) return;

            // Check if the player is using a cleaver
            ItemStack held = player.getMainHandStack();
            if (!isCleaver(held)) return;

            // Check if this mob type is allowed for slaughtering
            Identifier mobId = Registries.ENTITY_TYPE.getId(entity.getType());
            if (!isMobAllowedForSlaughtering(mobId, cfg)) return;

            // Check critical hit — ruins the corpse
            boolean wasCrit = LAST_ATTACK_WAS_CRIT.get();
            LAST_ATTACK_WAS_CRIT.set(false);

            if (wasCrit) {
                player.sendMessage(Text.literal(cfg.messages.slaughteringCorpseRuined), true);
                return; // No corpse spawned — loot is lost
            }

            // Spawn the corpse entity
            spawnCorpse(entity, serverWorld, cfg);
            player.sendMessage(Text.literal(cfg.messages.slaughteringCorpseSpawned), true);
        });
    }

    // --------------------------------------------------
    //  Non-cleaver loot suppression (random chance)
    // --------------------------------------------------

    /**
     * Registers listener for non-cleaver mob kills by players.
     * When slaughtering is enabled and a player kills an allowed mob WITHOUT a cleaver,
     * there is only a configurable chance (default 30%) to drop vanilla loot.
     */
    private static void registerNonCleaverLootSuppression() {
        // This is handled in shouldSuppressLoot() via the LivingEntityMixin drop injection.
        // The random roll is done per-entity via a ThreadLocal to maintain consistency
        // across the multiple drop method calls for the same entity death.
    }

    /**
     * ThreadLocal to store the non-cleaver loot roll result for the current entity death.
     * This ensures the same roll result is used across dropLoot, dropEquipment, and dropXp calls.
     */
    private static final ThreadLocal<Boolean> NON_CLEAVER_SUPPRESS_ROLL = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Integer> NON_CLEAVER_ROLL_ENTITY_ID = ThreadLocal.withInitial(() -> -1);

    // --------------------------------------------------
    //  Corpse spawning
    // --------------------------------------------------

    private static void spawnCorpse(LivingEntity original, ServerWorld world, KnowledgeBoundConfig cfg) {
        EntityType<?> type = original.getType();
        Identifier mobId = Registries.ENTITY_TYPE.getId(type);

        // Create a new entity of the same type
        Entity corpse = type.create(world);
        if (corpse == null) {
            KnowledgeBound.LOGGER.warn("[KnowledgeBound] Could not create corpse entity for {}", mobId);
            return;
        }

        // Spider positioning offset calculation to prevent model clipping underground when rotated sideways
        double yOffset = 0.0;
        if (type == EntityType.CAVE_SPIDER) {
            yOffset = 0.2;
        } else if (original instanceof net.minecraft.entity.mob.SpiderEntity) {
            yOffset = 0.4;
        }

        // Position at the death location with height adjustment
        corpse.setPosition(original.getX(), original.getY() + yOffset, original.getZ());
        corpse.setYaw(original.getYaw());

        // Copy baby state for all mob types so baby corpses stay baby-sized
        if (original instanceof MobEntity origMob && corpse instanceof MobEntity corpseMob) {
            corpseMob.setBaby(origMob.isBaby());
        }

        // Copy sheep state if applicable so wool matches original mob
        if (original instanceof net.minecraft.entity.passive.SheepEntity origSheep && corpse instanceof net.minecraft.entity.passive.SheepEntity corpseSheep) {
            corpseSheep.setColor(origSheep.getColor());
            corpseSheep.setSheared(origSheep.isSheared());
        }

        // Make it completely inert and set dead state so client renders it lying flat on its side
        if (corpse instanceof LivingEntity living) {
            living.setHealth(0.0F);
            living.deathTime = 19;
            living.setSilent(true);
            living.setInvulnerable(true);
            living.setNoGravity(true);
            living.setVelocity(Vec3d.ZERO);
        }
        if (corpse instanceof MobEntity mob) {
            mob.setAiDisabled(true);
            mob.setNoDrag(true);
            mob.setPersistent();
        }

        // Calculate absolute world tick timestamp for restart-safe despawning
        long despawnWorldTime = world.getTime() + cfg.slaughteringCorpseDespawnTicks;
        RegistryKey<LootTable> lootTableKey = original.getType().getLootTableId();
        String lootStr = lootTableKey != null ? lootTableKey.getValue().toString() : "";

        // Attach persistent metadata via commandTags
        corpse.addCommandTag(CORPSE_TAG);
        corpse.addCommandTag(TAG_DESPAWN_TIME_PREFIX + despawnWorldTime);
        if (!lootStr.isEmpty()) {
            corpse.addCommandTag(TAG_LOOT_TABLE_PREFIX + lootStr);
        }

        // Custom name — only visible when player looks directly at the entity
        String mobName = type.getName().getString();
        corpse.setCustomName(Text.literal(mobName + " Corpse").formatted(Formatting.GRAY));
        corpse.setCustomNameVisible(false);

        // Spawn into the world
        world.spawnEntity(corpse);

        KnowledgeBound.LOGGER.debug("[KnowledgeBound] Spawned corpse for {} at ({}, {}, {})",
                mobId, corpse.getX(), corpse.getY(), corpse.getZ());
    }

    // --------------------------------------------------
    //  Dissection interaction (right-click corpse)
    // --------------------------------------------------

    private static void registerDissectionInteraction() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

            KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
            if (!cfg.slaughteringEnabled) return ActionResult.PASS;

            // Only interact with corpses
            if (!entity.getCommandTags().contains(CORPSE_TAG)) return ActionResult.PASS;

            // Check what the player is holding
            ItemStack held = player.getStackInHand(hand);
            boolean isAxe = held.getItem() instanceof AxeItem;
            boolean isCleaver = isCleaver(held);

            if (!isAxe && !isCleaver) {
                serverPlayer.sendMessage(Text.literal(cfg.messages.slaughteringNeedTool), true);
                return ActionResult.FAIL;
            }

            // Perform dissection
            performDissection(serverPlayer, entity, isCleaver, cfg);

            return ActionResult.SUCCESS;
        });
    }

    private static void performDissection(ServerPlayerEntity player, Entity corpse,
                                           boolean usedCleaver, KnowledgeBoundConfig cfg) {
        if (!(corpse.getWorld() instanceof ServerWorld serverWorld)) return;

        // Determine slaughtering tier for fail chance
        int slaughterTier = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.SLAUGHTERING_ID);

        // --- Fail check (tier-dependent) ---
        double failChance = getFailChance(slaughterTier, cfg);
        double failRoll = RANDOM.nextDouble();

        if (failRoll < failChance) {
            // Failed dissection — drop rotten flesh as consolation and destroy corpse
            handleFailedDissection(player, corpse, serverWorld, cfg);
            return;
        }

        // --- Success path: determine dissection quality (poor/normal/excellent) ---
        double[] chances = usedCleaver
                ? cfg.slaughteringCleaverDissectionChances
                : cfg.slaughteringAxeDissectionChances;

        // Roll for quality among poor/normal/excellent
        double roll = RANDOM.nextDouble();
        int quality; // 0=poor, 1=normal, 2=excellent
        String message;

        double poorChance = chances.length > 0 ? chances[0] : 0.2;
        double normalChance = chances.length > 1 ? chances[1] : 0.5;
        // excellent is the remainder

        if (roll < poorChance) {
            quality = 0;
            message = cfg.messages.slaughteringDissectPoor;
        } else if (roll < poorChance + normalChance) {
            quality = 1;
            message = cfg.messages.slaughteringDissectNormal;
        } else {
            quality = 2;
            message = cfg.messages.slaughteringDissectExcellent;
        }

        // Get loot multiplier
        double lootMultiplier = cfg.slaughteringLootMultipliers[quality];

        // Drop loot based on original mob's loot table stored in tags
        RegistryKey<LootTable> lootTableKey = getCorpseLootTable(corpse);
        if (lootTableKey != null) {
            dropCorpseLoot(serverWorld, corpse.getPos(), lootTableKey, lootMultiplier, player);
        }

        // Play appropriate sounds
        if (quality == 2) {
            serverWorld.playSound(null, corpse.getBlockPos(),
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5F, 1.2F);
        } else {
            serverWorld.playSound(null, corpse.getBlockPos(),
                    SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.8F, 0.8F);
        }

        // Spawn particles
        serverWorld.spawnParticles(ParticleTypes.SMOKE,
                corpse.getX(), corpse.getY() + 0.5, corpse.getZ(),
                10, 0.3, 0.3, 0.3, 0.02);

        // Damage the tool
        ItemStack held = player.getMainHandStack();
        if (!held.isEmpty()) {
            held.damage(1, player, LivingEntity.getSlotForHand(player.getActiveHand()));
        }

        // Grant Slaughtering XP BEFORE sending the outcome message
        // so the XP action bar text doesn't overwrite the dissection result
        PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.SLAUGHTERING_ID);

        // Send outcome message LAST so it stays visible on the action bar
        player.sendMessage(Text.literal(message), true);

        // Remove the corpse
        corpse.discard();
    }

    /**
     * Gets the fail chance for the given slaughtering tier from config.
     * Tier 0 (beginner) = 80%, Tier 1 = 50%, Tier 2 = 20%, Tier 3 = 5% by default.
     */
    private static double getFailChance(int tier, KnowledgeBoundConfig cfg) {
        double[] failChances = cfg.slaughteringFailChancePerTier;
        if (failChances == null || failChances.length == 0) {
            return 0.80; // fallback for beginner
        }
        int index = Math.min(tier, failChances.length - 1);
        return failChances[index];
    }

    /**
     * Handle a failed dissection — drops 1 rotten flesh, plays failure sound, destroys corpse.
     */
    private static void handleFailedDissection(ServerPlayerEntity player, Entity corpse,
                                                ServerWorld serverWorld, KnowledgeBoundConfig cfg) {
        // Drop rotten flesh as consolation
        ItemStack rottenFlesh = new ItemStack(Items.ROTTEN_FLESH, 1);
        net.minecraft.entity.ItemEntity itemEntity = new net.minecraft.entity.ItemEntity(
                serverWorld, corpse.getX(), corpse.getY() + 0.5, corpse.getZ(), rottenFlesh);
        itemEntity.setVelocity(
                (RANDOM.nextDouble() - 0.5) * 0.2,
                0.2,
                (RANDOM.nextDouble() - 0.5) * 0.2);
        serverWorld.spawnEntity(itemEntity);

        // Play failure sound
        serverWorld.playSound(null, corpse.getBlockPos(),
                SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 0.5F);

        // Spawn red/angry particles
        serverWorld.spawnParticles(ParticleTypes.SMOKE,
                corpse.getX(), corpse.getY() + 0.5, corpse.getZ(),
                15, 0.4, 0.3, 0.4, 0.03);

        // Damage the tool
        ItemStack held = player.getMainHandStack();
        if (!held.isEmpty()) {
            held.damage(1, player, LivingEntity.getSlotForHand(player.getActiveHand()));
        }

        // Grant Slaughtering XP even on fail (practice makes perfect)
        PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.SLAUGHTERING_ID);

        // Send failure message LAST so it stays visible on the action bar
        player.sendMessage(Text.literal(cfg.messages.slaughteringDissectFail), true);

        // Remove the corpse
        corpse.discard();
    }

    private static void dropCorpseLoot(ServerWorld world, Vec3d pos,
                                        RegistryKey<LootTable> lootTableKey, double multiplier,
                                        ServerPlayerEntity player) {
        try {
            // Use the server's loot manager to generate loot
            LootTable lootTable = world.getServer().getReloadableRegistries()
                    .getLootTable(lootTableKey);

            net.minecraft.loot.context.LootContextParameterSet params =
                    new net.minecraft.loot.context.LootContextParameterSet.Builder(world)
                            .add(net.minecraft.loot.context.LootContextParameters.THIS_ENTITY, player)
                            .add(net.minecraft.loot.context.LootContextParameters.ORIGIN, pos)
                            .add(net.minecraft.loot.context.LootContextParameters.DAMAGE_SOURCE,
                                    world.getDamageSources().playerAttack(player))
                            .add(net.minecraft.loot.context.LootContextParameters.ATTACKING_ENTITY, player)
                            .build(net.minecraft.loot.context.LootContextTypes.ENTITY);

            List<ItemStack> loot = lootTable.generateLoot(params);

            for (ItemStack stack : loot) {
                // Apply multiplier
                int newCount = Math.max(1, (int) Math.round(stack.getCount() * multiplier));
                stack.setCount(newCount);

                // Spawn loot item in the world
                net.minecraft.entity.ItemEntity itemEntity = new net.minecraft.entity.ItemEntity(
                        world, pos.x, pos.y + 0.5, pos.z, stack);
                itemEntity.setVelocity(
                        (RANDOM.nextDouble() - 0.5) * 0.2,
                        0.2,
                        (RANDOM.nextDouble() - 0.5) * 0.2);
                world.spawnEntity(itemEntity);
            }
        } catch (Exception e) {
            KnowledgeBound.LOGGER.warn("[KnowledgeBound] Failed to generate loot for corpse dissection", e);
        }
    }

    // --------------------------------------------------
    //  Corpse Metadata & Persistence Helpers
    // --------------------------------------------------

    /**
     * Retrieve the loot table registry key stored in entity tags.
     */
    public static RegistryKey<LootTable> getCorpseLootTable(Entity corpse) {
        for (String tag : corpse.getCommandTags()) {
            if (tag.startsWith(TAG_LOOT_TABLE_PREFIX)) {
                String idStr = tag.substring(TAG_LOOT_TABLE_PREFIX.length());
                return RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(idStr));
            }
        }
        return corpse.getType().getLootTableId();
    }

    /**
     * Check if a corpse entity has passed its restart-safe despawn world tick timestamp.
     */
    public static boolean isCorpseExpired(Entity corpse) {
        for (String tag : corpse.getCommandTags()) {
            if (tag.startsWith(TAG_DESPAWN_TIME_PREFIX)) {
                try {
                    long expireTime = Long.parseLong(tag.substring(TAG_DESPAWN_TIME_PREFIX.length()));
                    return corpse.getWorld().getTime() >= expireTime;
                } catch (NumberFormatException ignored) {}
            }
        }
        return false;
    }

    /**
     * Cleanly despawn an expired corpse with decay particles.
     */
    public static void despawnCorpse(Entity corpse) {
        if (corpse.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE,
                    corpse.getX(), corpse.getY() + 0.5, corpse.getZ(),
                    20, 0.5, 0.3, 0.5, 0.02);
        }
        corpse.discard();
    }

    // --------------------------------------------------
    //  Utility methods
    // --------------------------------------------------

    /**
     * Check if the item is a Butcher's Cleaver (Iron Axe with our custom model data).
     */
    public static boolean isCleaver(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() != Items.IRON_AXE) return false;

        // Check for custom name "Butcher's Cleaver"
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (customName == null) return false;
        return customName.getString().equals("Butcher's Cleaver");
    }

    /**
     * Check if standard vanilla loot drops should be suppressed for a dying mob.
     * 
     * Three cases:
     * 1. Corpse entities (kb_corpse) — ALWAYS suppress to prevent double drops.
     * 2. Cleaver kills on allowed mobs (non-crit) — suppress because loot comes from dissection.
     * 3. Non-cleaver kills on allowed mobs — suppress with (1 - nonCleaverLootChance) probability.
     */
    public static boolean shouldSuppressLoot(LivingEntity entity, net.minecraft.entity.damage.DamageSource damageSource) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.slaughteringEnabled) return false;

        // Corpse entities themselves MUST ALWAYS suppress drops when spawned
        if (entity.getCommandTags().contains(CORPSE_TAG)) return true;

        // Only care about kills by players
        if (!(damageSource.getAttacker() instanceof ServerPlayerEntity player)) return false;

        // Check if this mob type is allowed for slaughtering
        Identifier mobId = Registries.ENTITY_TYPE.getId(entity.getType());
        if (!isMobAllowedForSlaughtering(mobId, cfg)) return false;

        // Check if the player is using a cleaver
        ItemStack held = player.getMainHandStack();
        if (isCleaver(held)) {
            // Cleaver kill — check critical hit
            // If it WAS a crit, do NOT suppress standard drops (ruined corpse = vanilla drops)
            boolean wasCrit = LAST_ATTACK_WAS_CRIT.get();
            return !wasCrit;
        }

        // Non-cleaver kill on an allowed mob — random chance to drop loot
        // Use a per-entity roll cached in ThreadLocal so dropLoot/dropEquipment/dropXp
        // all get the same result for the same entity death
        int entityId = entity.getId();
        if (NON_CLEAVER_ROLL_ENTITY_ID.get() != entityId) {
            // New entity — roll the dice
            boolean suppress = RANDOM.nextDouble() >= cfg.slaughteringNonCleaverLootChance;
            NON_CLEAVER_SUPPRESS_ROLL.set(suppress);
            NON_CLEAVER_ROLL_ENTITY_ID.set(entityId);
        }
        return NON_CLEAVER_SUPPRESS_ROLL.get();
    }

    /**
     * Check if a mob type is allowed for slaughtering based on config.
     */
    private static boolean isMobAllowedForSlaughtering(Identifier mobId, KnowledgeBoundConfig cfg) {
        String mobIdStr = mobId.toString();

        if (cfg.slaughteringAllMobsByDefault) {
            // All mobs allowed EXCEPT those in the blacklist
            return !cfg.slaughteringMobBlacklist.contains(mobIdStr);
        } else {
            // Only mobs in the whitelist are allowed
            return cfg.slaughteringMobWhitelist.contains(mobIdStr);
        }
    }
}
