package io.github.dashtiss.backrooms;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Backrooms implements ModInitializer {

    public static final Set<UUID> AUTHENTICATED_CLIENTS = new HashSet<>();

    @Override
    public void onInitialize() {
        // 1. Register the packet payload so the game engine recognizes it
        PayloadTypeRegistry.playC2S().register(HandshakePayload.ID, HandshakePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HandshakePayload.ID, HandshakePayload.CODEC);

        // 2. Set up the server-side listener to catch the packet from modded clients
        ServerPlayNetworking.registerGlobalReceiver(HandshakePayload.ID, (payload, context) -> {
            UUID playerUuid = context.player().getUuid();

            context.server().execute(() -> {
                AUTHENTICATED_CLIENTS.add(playerUuid);
                System.out.println("[Backrooms] Player " + context.player().getName().getString() + " verified with modded client.");
            });
        });

        // 3. Remove them from the authorized list if they disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            AUTHENTICATED_CLIENTS.remove(handler.getPlayer().getUuid());
        });
    }

    public static boolean isClientModded(UUID uuid) {
        return AUTHENTICATED_CLIENTS.contains(uuid);
    }
}
