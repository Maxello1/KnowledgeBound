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

        // Copper golem check (safe for modded — uses registry ID string match)
        if (cfg.blockCopperGolemSpawns) {
            Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());
            if (entityId.getPath().contains("copper_golem")) {
                cir.setReturnValue(false);
                return;
            }
        }

        // Generic blocklist from config
        if (!cfg.blockedMobSpawns.isEmpty()) {
            String entityIdStr = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
            if (cfg.blockedMobSpawns.contains(entityIdStr)) {
                cir.setReturnValue(false);
            }
        }
    }
}


