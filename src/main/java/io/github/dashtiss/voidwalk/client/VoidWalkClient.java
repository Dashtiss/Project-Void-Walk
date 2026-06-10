package io.github.dashtiss.voidwalk.client;

import io.github.dashtiss.voidwalk.payloads.HandshakePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import io.github.dashtiss.voidwalk.payloads.VanishTogglePayload;

public class VoidWalkClient implements ClientModInitializer {

    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("backrooms", "category"));

    @Override
    public void onInitializeClient() {
        // When the client joins, send the handshake payload to verify the mod presence
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientPlayNetworking.send(new HandshakePayload());
        });

        // Register the "V" keybind
        KeyBinding vanishKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.backrooms.vanish",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY
        ));

        // Listen for keypress and send toggle packet
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (vanishKey.wasPressed()) {
                ClientPlayNetworking.send(new VanishTogglePayload());
            }
        });
    }


}
