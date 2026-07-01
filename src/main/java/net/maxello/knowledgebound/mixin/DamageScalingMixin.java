package net.maxello.knowledgebound.mixin;

import net.maxello.knowledgebound.KnowledgeBound;
import net.maxello.knowledgebound.config.KnowledgeBoundConfig;
import net.maxello.knowledgebound.core.PlayerKnowledgeManager;
import net.maxello.knowledgebound.core.KnowledgeRegistry;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class DamageScalingMixin {

    /**
     * Scales incoming damage based on the attacker's combat knowledge tier.
     * Low-tier players deal reduced damage; max-tier players deal full damage.
     */
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float knowledgebound$scaleDamage(float amount, DamageSource source) {
        if (amount <= 0.0f) return amount;

        // Only scale damage dealt by players
        if (!(source.getAttacker() instanceof ServerPlayerEntity player)) {
            return amount;
        }

        int tier;
        if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            tier = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.RANGED_COMBAT_ID);
        } else {
            tier = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.MELEE_COMBAT_ID);
        }

        double[] scale = KnowledgeBoundConfig.INSTANCE.combatDamageScale;
        int index = Math.min(tier, scale.length - 1);
        double multiplier = (index >= 0 && index < scale.length) ? scale[index] : 1.0;

        return (float) (amount * multiplier);
    }
}


