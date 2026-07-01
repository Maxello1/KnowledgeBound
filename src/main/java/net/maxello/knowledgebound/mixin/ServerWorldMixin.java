package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true)
    private void knowledgebound$onSpawnEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        KnowledgeBoundConfig cfg = KnowledgeBoundConfig.INSTANCE;

        // The config allows us to completely wipe out certain mobs from the world.
        // If a specific mob type is set to be blocked, we simply say "nope" right as it tries to spawn,
        // and cancel the entire spawning event.
        if (cfg.blockVillagerSpawns && entity instanceof VillagerEntity) {
            cir.setReturnValue(false);
            return;
        }
        if (cfg.blockIronGolemSpawns && entity instanceof IronGolemEntity) {
            cir.setReturnValue(false);
            return;
        }
        if (cfg.blockSnowGolemSpawns && entity instanceof SnowGolemEntity) {
            cir.setReturnValue(false);
            return;
        }
        if (cfg.blockZombieVillagerSpawns && entity instanceof ZombieVillagerEntity) {
            cir.setReturnValue(false);
            return;
        }
        if (cfg.blockWanderingTraderSpawns && entity instanceof WanderingTraderEntity) {
            cir.setReturnValue(false);
            return;
        }
        if (cfg.blockPillagerSpawns && entity instanceof PillagerEntity) {
            cir.setReturnValue(false);
            return;
        }

        // We also have a special check for copper golems.
        // Since copper golems might come from different mods, we don't check the Java class instance.
        // Instead, we just check if its registry ID string has "copper_golem" in it. Safe and easy!
        if (cfg.blockCopperGolemSpawns) {
            Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());
            if (entityId.getPath().contains("copper_golem")) {
                cir.setReturnValue(false);
                return;
            }
        }

        // Finally, if modpack makers want to block any other random entities, they can just 
        // throw the registry ID into the "blockedMobSpawns" list in the config.
        if (!cfg.blockedMobSpawns.isEmpty()) {
            String entityIdStr = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
            if (cfg.blockedMobSpawns.contains(entityIdStr)) {
                cir.setReturnValue(false);
            }
        }
    }
}


