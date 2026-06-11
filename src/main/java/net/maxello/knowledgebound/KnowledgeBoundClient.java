package net.maxello.knowledgebound;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KnowledgeBoundClient implements ClientModInitializer {

    public static final Logger CLIENT_LOGGER = LoggerFactory.getLogger("KnowledgeBoundClient");

    private static KeyBinding hudToggleKey;

    @Override
    public void onInitializeClient() {
        CLIENT_LOGGER.info("[KnowledgeBound] Client initializing…");

        // Receive knowledge state from server and update client-side cache
        ClientPlayNetworking.registerGlobalReceiver(KnowledgeSyncPayload.ID, (payload, ctx) ->
                ClientKnowledgeState.update(payload.knowledgeData())
        );

        // Keybind to toggle the HUD overlay (default: K)
        hudToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.knowledgebound.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "key.categories.knowledgebound"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (hudToggleKey.wasPressed()) {
                ClientKnowledgeState.hudVisible = !ClientKnowledgeState.hudVisible;
            }
        });

        // HUD overlay rendered every frame when visible
        HudRenderCallback.EVENT.register((context, tickCounter) ->
                KnowledgeHudRenderer.render(context)
        );
    }
}
