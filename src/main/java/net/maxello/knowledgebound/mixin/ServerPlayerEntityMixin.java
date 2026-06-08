package net.maxello.knowledgebound.mixin;

import com.mojang.authlib.GameProfile;
import net.maxello.knowledgebound.PlayerKnowledgeManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
@SuppressWarnings("unused") // mixin class used reflectively
public abstract class ServerPlayerEntityMixin extends PlayerEntity {

    // Required dummy constructor for mixin into PlayerEntity subclass
    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    @SuppressWarnings("unused")
    private void knowledgebound$writeKnowledge(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        PlayerKnowledgeManager.writeToNbt(self, nbt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    @SuppressWarnings("unused")
    private void knowledgebound$readKnowledge(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        PlayerKnowledgeManager.readFromNbt(self, nbt);
    }
}
