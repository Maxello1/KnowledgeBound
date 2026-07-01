package net.maxello.knowledgebound;
import net.maxello.knowledgebound.network.ClientKnowledgeState;
import net.maxello.knowledgebound.network.KnowledgeSyncPayload;
import net.maxello.knowledgebound.gui.KnowledgeHudRenderer;

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

// This class is specifically for the client-side setup of the mod.
// Things like rendering the HUD, registering keybinds, and receiving data from the server go in here.
// We don't want to load this stuff on a dedicated server because a server doesn't have a screen or keyboard!
public class KnowledgeBoundClient implements ClientModInitializer {

    public static final Logger CLIENT_LOGGER = LoggerFactory.getLogger("KnowledgeBoundClient");

    // The key that players will press to show or hide their knowledge progression on the screen.
    private static KeyBinding hudToggleKey;

    @Override
    public void onInitializeClient() {
        CLIENT_LOGGER.info("[KnowledgeBound] Client initializing…");

        // When the server tells us about the player's current knowledge levels, we catch that message here.
        // We just take the payload data and shove it into our client-side state manager so the HUD can read it.
        ClientPlayNetworking.registerGlobalReceiver(KnowledgeSyncPayload.ID, (payload, ctx) ->
                ClientKnowledgeState.update(payload.knowledgeData())
        );

        // Let's hook up our custom keybind. We default it to 'K' for Knowledge, but players can change it in their controls menu.
        // We're registering it in a specific category so it's easy to find.
        hudToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.knowledgebound.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "key.categories.knowledgebound"
        ));

        // Every time the game ticks on the client, we check if the player just pressed our toggle key.
        // We use a while loop with wasPressed() to catch it properly if they mash the button multiple times in a single tick.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (hudToggleKey.wasPressed()) {
                // Just flip the visibility boolean back and forth.
                ClientKnowledgeState.hudVisible = !ClientKnowledgeState.hudVisible;
            }
        });

        // Finally, we hook into the game's HUD rendering event. 
        // This fires every single frame when the game is drawing the screen, 
        // so we just pass the context over to our renderer to draw the knowledge box if it's currently visible.
        HudRenderCallback.EVENT.register((context, tickCounter) ->
                KnowledgeHudRenderer.render(context)
        );
    }
}

