package net.maxello.knowledgebound.mechanics.jobs;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;
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
 * The meat and potatoes of the Slaughtering job.
 * 
 * Instead of just hitting a cow and getting beef, the player has to use a
 * special "Butcher's Cleaver". Doing so prevents the normal drops and instead
 * spawns a "corpse" entity that lies on the ground.
 * 
 * The player then right-clicks the corpse to dissect it. Depending on their
 * Slaughtering tier, they might ruin the meat completely (getting rotten flesh),
 * do an okay job, or perfectly carve it to get double/triple the normal drops.
 * 
 * We also suppress normal loot drops randomly if they kill animals without a cleaver,
 * forcing them to engage with the system.
 */
public final class SlaughteringManager {

    private SlaughteringManager() {}

    private static final Random RANDOM = new Random();

    /** 
     * We have to track if the fatal blow was a critical hit. 
     * Crits ruin the meat, so we don't spawn a dissectable corpse if they crit.
     * This ThreadLocal is updated via a mixin right before damage is applied.
     */
    public static final ThreadLocal<Boolean> LAST_ATTACK_WAS_CRIT = ThreadLocal.withInitial(() -> false);

    /** NBT/scoreboard tag used to identify corpse entities. */
    public static final String CORPSE_TAG = "kb_corpse";

    /** 
     * We use these tags to attach data directly to the corpse entity. 
     * This ensures the corpse still knows what to drop even if the server restarts.
     */
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
        // This fires right after a mob dies, but before it despawns.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
            if (!cfg.slaughteringEnabled) return;

            // Only care about kills by players
            if (!(damageSource.getAttacker() instanceof ServerPlayerEntity player)) return;
            if (!(entity.getWorld() instanceof ServerWorld serverWorld)) return;

            // Wait, is the thing that just died ALREADY a corpse?
            // (Sometimes other mods or weird vanilla mechanics try to re-kill dead things)
            if (entity.getCommandTags().contains(CORPSE_TAG)) return;

            // Are they using the correct tool?
            ItemStack held = player.getMainHandStack();
            if (!isCleaver(held)) return;

            // Is this mob on the whitelist/blacklist? (e.g. no dissecting zombies usually)
            Identifier mobId = Registries.ENTITY_TYPE.getId(entity.getType());
            if (!isMobAllowedForSlaughtering(mobId, cfg)) return;

            // Check critical hit — ruins the corpse
            boolean wasCrit = LAST_ATTACK_WAS_CRIT.get();
            LAST_ATTACK_WAS_CRIT.set(false); // Reset it for the next attack

            if (wasCrit) {
                // They swung too hard and destroyed the usable meat. 
                player.sendMessage(Text.literal(cfg.messages.slaughteringCorpseRuined), true);
                return; // No corpse spawned — they get whatever vanilla loot manages to drop
            }

            // All checks passed! Spawn the corpse.
            spawnCorpse(entity, serverWorld, cfg);
            player.sendMessage(Text.literal(cfg.messages.slaughteringCorpseSpawned), true);
        });
    }

    // --------------------------------------------------
    //  Non-cleaver loot suppression (random chance)
    // --------------------------------------------------

    /**
     * We don't actually register an event here anymore.
     * The logic for suppressing loot when a player just slaps a cow with a sword
     * is handled entirely inside shouldSuppressLoot(), which is called by our mixin.
     */
    private static void registerNonCleaverLootSuppression() {
    }

    /**
     * Minecraft asks "should I drop loot?" "should I drop equipment?" "should I drop XP?"
     * as three separate calls during a mob's death sequence. 
     * We use these thread-locals to ensure we only roll the dice ONCE per death,
     * so it doesn't randomly drop XP but no items, etc.
     */
    private static final ThreadLocal<Boolean> NON_CLEAVER_SUPPRESS_ROLL = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Integer> NON_CLEAVER_ROLL_ENTITY_ID = ThreadLocal.withInitial(() -> -1);

    // --------------------------------------------------
    //  Corpse spawning
    // --------------------------------------------------

    private static void spawnCorpse(LivingEntity original, ServerWorld world, KnowledgeBoundConfig cfg) {
        EntityType<?> type = original.getType();
        Identifier mobId = Registries.ENTITY_TYPE.getId(type);

        // We create a literal duplicate of the entity that just died.
        Entity corpse = type.create(world);
        if (corpse == null) {
            KnowledgeBound.LOGGER.warn("[KnowledgeBound] Could not create corpse entity for {}", mobId);
            return;
        }

        // Spiders have weird hitboxes and rendering when lying flat.
        // We lift them slightly so they don't clip through the floor.
        double yOffset = 0.0;
        if (type == EntityType.CAVE_SPIDER) {
            yOffset = 0.2;
        } else if (original instanceof net.minecraft.entity.mob.SpiderEntity) {
            yOffset = 0.4;
        }

        // Plop it right where the mob died.
        corpse.setPosition(original.getX(), original.getY() + yOffset, original.getZ());
        corpse.setYaw(original.getYaw());

        // Make sure a baby cow spawns a baby corpse, not a giant adult one.
        if (original instanceof MobEntity origMob && corpse instanceof MobEntity corpseMob) {
            corpseMob.setBaby(origMob.isBaby());
        }

        // If it was a pink sheep, make the corpse pink too.
        if (original instanceof net.minecraft.entity.passive.SheepEntity origSheep && corpse instanceof net.minecraft.entity.passive.SheepEntity corpseSheep) {
            corpseSheep.setColor(origSheep.getColor());
            corpseSheep.setSheared(origSheep.isSheared());
        }

        // Here is the magic: we make the entity functionally "dead" but still present in the world.
        // deathTime = 19 is exactly the frame where a dying mob is lying completely flat on its side
        // right before vanilla despawns it. We freeze it on this frame.
        if (corpse instanceof LivingEntity living) {
            living.setHealth(0.0F);
            living.deathTime = 19;
            living.setSilent(true);
            living.setInvulnerable(true);
            living.setNoGravity(true);
            living.setVelocity(Vec3d.ZERO);
        }
        
        // Strip out its AI so it doesn't try to pathfind or attack players while dead!
        if (corpse instanceof MobEntity mob) {
            mob.setAiDisabled(true);
            mob.setNoDrag(true);
            mob.setPersistent(); // Don't let vanilla despawn logic touch this
        }

        // Figure out when this corpse should rot away automatically.
        long despawnWorldTime = world.getTime() + cfg.slaughteringCorpseDespawnTicks;
        RegistryKey<LootTable> lootTableKey = original.getType().getLootTableId();
        String lootStr = lootTableKey != null ? lootTableKey.getValue().toString() : "";

        // Attach our metadata directly to the entity's tags.
        corpse.addCommandTag(CORPSE_TAG);
        corpse.addCommandTag(TAG_DESPAWN_TIME_PREFIX + despawnWorldTime);
        if (!lootStr.isEmpty()) {
            corpse.addCommandTag(TAG_LOOT_TABLE_PREFIX + lootStr);
        }

        // Give it a neat name plate that only shows when looking directly at it.
        String mobName = type.getName().getString();
        corpse.setCustomName(Text.literal(mobName + " Corpse").formatted(Formatting.GRAY));
        corpse.setCustomNameVisible(false);

        // Put it in the world!
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

            // Are they clicking one of our custom corpses?
            if (!entity.getCommandTags().contains(CORPSE_TAG)) return ActionResult.PASS;

            // Check what tool they are using to carve it up
            ItemStack held = player.getStackInHand(hand);
            boolean isAxe = held.getItem() instanceof AxeItem;
            boolean isCleaver = isCleaver(held);

            if (!isAxe && !isCleaver) {
                serverPlayer.sendMessage(Text.literal(cfg.messages.slaughteringNeedTool), true);
                return ActionResult.FAIL;
            }

            // Actually roll the dice and generate the loot
            performDissection(serverPlayer, entity, isCleaver, cfg);

            // Cancel further processing so they don't accidentally ride the dead horse or milk the dead cow
            return ActionResult.SUCCESS;
        });
    }

    private static void performDissection(ServerPlayerEntity player, Entity corpse,
                                           boolean usedCleaver, KnowledgeBoundConfig cfg) {
        if (!(corpse.getWorld() instanceof ServerWorld serverWorld)) return;

        int slaughterTier = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.SLAUGHTERING_ID);

        // --- Fail check (tier-dependent) ---
        // If they are a low level, there's a high chance they just butcher the carving
        // and ruin the meat.
        double failChance = getFailChance(slaughterTier, cfg);
        double failRoll = RANDOM.nextDouble();

        if (failRoll < failChance) {
            handleFailedDissection(player, corpse, serverWorld, cfg);
            return;
        }

        // --- Success path: determine dissection quality ---
        // Cleavers have significantly better odds of a "perfect" cut than normal axes.
        double[] chances = usedCleaver
                ? cfg.slaughteringCleaverDissectionChances
                : cfg.slaughteringAxeDissectionChances;

        double roll = RANDOM.nextDouble();
        int quality; // 0=poor, 1=normal, 2=excellent
        String message;

        double poorChance = chances.length > 0 ? chances[0] : 0.2;
        double normalChance = chances.length > 1 ? chances[1] : 0.5;

        if (roll < poorChance) {
            quality = 0;
            message = cfg.messages.slaughteringDissectPoor;
        } else if (roll < poorChance + normalChance) {
            quality = 1;
            message = cfg.messages.slaughteringDissectNormal;
        } else {
            quality = 2; // excellent is whatever is left of the 100%
            message = cfg.messages.slaughteringDissectExcellent;
        }

        // Poor cut = maybe 0.5x loot. Excellent = maybe 2.5x loot.
        double lootMultiplier = cfg.slaughteringLootMultipliers[quality];

        // Fetch the loot table string we saved on the entity earlier
        RegistryKey<LootTable> lootTableKey = getCorpseLootTable(corpse);
        if (lootTableKey != null) {
            dropCorpseLoot(serverWorld, corpse.getPos(), lootTableKey, lootMultiplier, player);
        }

        // Audio feedback
        if (quality == 2) {
            serverWorld.playSound(null, corpse.getBlockPos(),
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5F, 1.2F);
        } else {
            serverWorld.playSound(null, corpse.getBlockPos(),
                    SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.8F, 0.8F);
        }

        // Visual feedback
        serverWorld.spawnParticles(ParticleTypes.SMOKE,
                corpse.getX(), corpse.getY() + 0.5, corpse.getZ(),
                10, 0.3, 0.3, 0.3, 0.02);

        // Take durability off their tool
        ItemStack held = player.getMainHandStack();
        if (!held.isEmpty()) {
            held.damage(1, player, LivingEntity.getSlotForHand(player.getActiveHand()));
        }

        // Give them XP for trying
        PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.SLAUGHTERING_ID);

        // Tell them how they did
        player.sendMessage(Text.literal(message), true);

        // Cleanup the entity
        corpse.discard();
    }

    /**
     * Gets the chance to completely fail a dissection from the config.
     * e.g. Tier 0 = 80% fail. Tier 3 = 5% fail.
     */
    private static double getFailChance(int tier, KnowledgeBoundConfig cfg) {
        double[] failChances = cfg.slaughteringFailChancePerTier;
        if (failChances == null || failChances.length == 0) {
            return 0.80; 
        }
        int index = Math.min(tier, failChances.length - 1);
        return failChances[index];
    }

    /**
     * Handle a failed dissection — drops 1 rotten flesh, plays an ugly sound, destroys corpse.
     */
    private static void handleFailedDissection(ServerPlayerEntity player, Entity corpse,
                                                ServerWorld serverWorld, KnowledgeBoundConfig cfg) {
        
        // Toss out a single piece of rotten flesh as a consolation prize
        ItemStack rottenFlesh = new ItemStack(Items.ROTTEN_FLESH, 1);
        net.minecraft.entity.ItemEntity itemEntity = new net.minecraft.entity.ItemEntity(
                serverWorld, corpse.getX(), corpse.getY() + 0.5, corpse.getZ(), rottenFlesh);
        itemEntity.setVelocity(
                (RANDOM.nextDouble() - 0.5) * 0.2,
                0.2,
                (RANDOM.nextDouble() - 0.5) * 0.2);
        serverWorld.spawnEntity(itemEntity);

        // Splat noise
        serverWorld.playSound(null, corpse.getBlockPos(),
                SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 0.5F);

        serverWorld.spawnParticles(ParticleTypes.SMOKE,
                corpse.getX(), corpse.getY() + 0.5, corpse.getZ(),
                15, 0.4, 0.3, 0.4, 0.03);

        ItemStack held = player.getMainHandStack();
        if (!held.isEmpty()) {
            held.damage(1, player, LivingEntity.getSlotForHand(player.getActiveHand()));
        }

        // Even failing teaches you something!
        PlayerKnowledgeManager.grantMinuteIfAllowed(player, KnowledgeRegistry.SLAUGHTERING_ID);

        player.sendMessage(Text.literal(cfg.messages.slaughteringDissectFail), true);
        corpse.discard();
    }

    /**
     * Tells Minecraft to actually generate the loot table for this mob,
     * multiplies the amounts based on the dissection quality, and spits them out.
     */
    private static void dropCorpseLoot(ServerWorld world, Vec3d pos,
                                        RegistryKey<LootTable> lootTableKey, double multiplier,
                                        ServerPlayerEntity player) {
        try {
            LootTable lootTable = world.getServer().getReloadableRegistries()
                    .getLootTable(lootTableKey);

            // We have to build a context object to trick Minecraft into thinking
            // the player just killed the mob normally, otherwise things like Looting enchantments
            // or player-only drops won't work correctly.
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
                // Apply our quality multiplier to the stack count
                int newCount = Math.max(1, (int) Math.round(stack.getCount() * multiplier));
                stack.setCount(newCount);

                // Chuck the item onto the ground
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
     * Reads the loot table identifier string we stored on the entity's tags.
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
     * Checks if the corpse has rotted away and should be removed.
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
     * Cleanly despawn an expired corpse with some decay smoke.
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
     * A cleaver is just an Iron Axe that has its custom name set exactly to "Butcher's Cleaver".
     * A resource pack handles turning it into a giant knife model.
     */
    public static boolean isCleaver(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() != Items.IRON_AXE) return false;

        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (customName == null) return false;
        return customName.getString().equals("Butcher's Cleaver");
    }

    /**
     * This is the master switch that controls whether a dying mob drops standard vanilla items.
     * Called by our LivingEntityMixin.
     */
    public static boolean shouldSuppressLoot(LivingEntity entity, net.minecraft.entity.damage.DamageSource damageSource) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;
        if (!cfg.slaughteringEnabled) return false;

        // The corpse itself should never drop vanilla loot if it gets "killed" somehow.
        if (entity.getCommandTags().contains(CORPSE_TAG)) return true;

        // Was it killed by a player?
        if (!(damageSource.getAttacker() instanceof ServerPlayerEntity player)) return false;

        Identifier mobId = Registries.ENTITY_TYPE.getId(entity.getType());
        if (!isMobAllowedForSlaughtering(mobId, cfg)) return false;

        ItemStack held = player.getMainHandStack();
        if (isCleaver(held)) {
            // If they used a cleaver, we ALWAYS suppress vanilla drops so we can spawn the corpse instead.
            // UNLESS it was a crit, in which case the meat is ruined and we just let it drop vanilla items.
            boolean wasCrit = LAST_ATTACK_WAS_CRIT.get();
            return !wasCrit;
        }

        // They killed an animal without a cleaver. We roll a chance to see if they get ANY loot at all.
        // We use the ThreadLocal ID system to make sure we don't roll separately for drops vs XP.
        int entityId = entity.getId();
        if (NON_CLEAVER_ROLL_ENTITY_ID.get() != entityId) {
            boolean suppress = RANDOM.nextDouble() >= cfg.slaughteringNonCleaverLootChance;
            NON_CLEAVER_SUPPRESS_ROLL.set(suppress);
            NON_CLEAVER_ROLL_ENTITY_ID.set(entityId);
        }
        return NON_CLEAVER_SUPPRESS_ROLL.get();
    }

    /**
     * Is this a chicken/cow/pig or is it a zombie that we shouldn't be butchering?
     */
    private static boolean isMobAllowedForSlaughtering(Identifier mobId, KnowledgeBoundConfig cfg) {
        String mobIdStr = mobId.toString();

        if (cfg.slaughteringAllMobsByDefault) {
            return !cfg.slaughteringMobBlacklist.contains(mobIdStr);
        } else {
            return cfg.slaughteringMobWhitelist.contains(mobIdStr);
        }
    }
}



