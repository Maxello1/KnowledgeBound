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

/**
 * If you hand a sword to a toddler, they probably aren't going to do a ton of damage with it.
 * This mixin scales the actual damage dealt by a player based on their Melee or Ranged combat tier.
 * It forces players to actually level up their combat skills before they can start one-shotting zombies.
 */
@Mixin(LivingEntity.class)
public abstract class DamageScalingMixin {

    /**
     * We grab the `amount` variable right at the start of the `damage` method. 
     * This is the raw damage number before armor and enchantments usually apply.
     */
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float knowledgebound$scaleDamage(float amount, DamageSource source) {
        // If they aren't even doing damage, just let it pass through. No need to multiply zero.
        if (amount <= 0.0f) return amount;

        // We only want to handicap players. Zombies, skeletons, and TNT all do normal damage.
        if (!(source.getAttacker() instanceof ServerPlayerEntity player)) {
            return amount;
        }

        int tier;
        // Check how they dealt the damage. Did they shoot an arrow/throw a trident?
        // Or did they just whack it with a stick?
        if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            tier = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.RANGED_COMBAT_ID);
        } else {
            tier = PlayerKnowledgeManager.getTier(player, KnowledgeRegistry.MELEE_COMBAT_ID);
        }

        // We have an array in the config that defines the damage multiplier for each tier.
        // E.g. [0.2, 0.5, 0.8, 1.0] meaning tier 0 does 20% damage, tier 3 does 100%.
        double[] scale = KnowledgeBoundConfig.INSTANCE.combatDamageScale;
        
        // Make sure we don't go out of bounds if they somehow have a massive tier level.
        int index = Math.min(tier, scale.length - 1);
        double multiplier = (index >= 0 && index < scale.length) ? scale[index] : 1.0;

        // Apply our multiplier to the original damage amount and return it back to vanilla.
        return (float) (amount * multiplier);
    }
}


