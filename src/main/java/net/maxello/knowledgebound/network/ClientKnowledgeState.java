package net.maxello.knowledgebound.network;

import net.maxello.knowledgebound.KnowledgeBound;
import java.util.HashMap;
import java.util.Map;

/**
 * Client-side mirror of the player's knowledge state.
 * Because the actual XP data lives on the server, we need this class to cache the data 
 * when the server sends us an update via KnowledgeSyncPayload packets.
 * Each entry holds: [tier, currentMinutes, neededMinutes, maxTier]
 */
public final class ClientKnowledgeState {

    private ClientKnowledgeState() {}

    public static final Map<String, int[]> DATA = new HashMap<>();
    public static boolean hudVisible = false;

    public static void update(Map<String, int[]> incoming) {
        DATA.clear();
        DATA.putAll(incoming);
    }

    public static int getTier(String id) {
        int[] d = DATA.get(id);
        return d != null ? d[0] : 0;
    }

    public static int getCurrentMinutes(String id) {
        int[] d = DATA.get(id);
        return d != null ? d[1] : 0;
    }

    public static int getNeededMinutes(String id) {
        int[] d = DATA.get(id);
        return d != null ? d[2] : 0;
    }

    public static int getMaxTier(String id) {
        int[] d = DATA.get(id);
        return d != null ? d[3] : 5;
    }

    public static boolean isMaxTier(String id) {
        int[] d = DATA.get(id);
        return d != null && d[0] >= d[3];
    }
}


