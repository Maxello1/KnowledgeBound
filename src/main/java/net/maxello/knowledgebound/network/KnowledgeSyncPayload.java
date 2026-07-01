package net.maxello.knowledgebound.network;

import net.maxello.knowledgebound.KnowledgeBound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-to-Client packet carrying the full knowledge state for one player.
 * We send this over the network whenever a player's stats change, so their 
 * client-side HUD stays perfectly in sync with the server.
 * Each knowledge entry is just an array of integers: [tier, currentMinutes, neededMinutes, maxTier]
 */
public record KnowledgeSyncPayload(Map<String, int[]> knowledgeData) implements CustomPayload {

    public static final Id<KnowledgeSyncPayload> ID =
            new Id<>(Identifier.of(KnowledgeBound.MOD_ID, "sync_knowledge"));

    public static final PacketCodec<PacketByteBuf, KnowledgeSyncPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeInt(payload.knowledgeData().size());
                        for (Map.Entry<String, int[]> e : payload.knowledgeData().entrySet()) {
                            buf.writeString(e.getKey());
                            int[] d = e.getValue();
                            buf.writeInt(d[0]); // tier
                            buf.writeInt(d[1]); // currentMinutes
                            buf.writeInt(d[2]); // neededMinutes (0 = max tier)
                            buf.writeInt(d[3]); // maxTier
                        }
                    },
                    buf -> {
                        int size = buf.readInt();
                        Map<String, int[]> map = new HashMap<>();
                        for (int i = 0; i < size; i++) {
                            String id = buf.readString();
                            int tier    = buf.readInt();
                            int current = buf.readInt();
                            int needed  = buf.readInt();
                            int max     = buf.readInt();
                            map.put(id, new int[]{tier, current, needed, max});
                        }
                        return new KnowledgeSyncPayload(map);
                    }
            );

    @Override
    public Id<KnowledgeSyncPayload> getId() {
        return ID;
    }
}


