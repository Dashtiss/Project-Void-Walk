package io.github.dashtiss.backrooms;

import io.github.dashtiss.backrooms.commands.BackroomsCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Backrooms implements ModInitializer {

    public static final Set<UUID> AUTHENTICATED_CLIENTS = new HashSet<>();
    public static final Set<UUID> VANISHED_PLAYERS = new HashSet<>();
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(HandshakePayload.ID, HandshakePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(VanishTogglePayload.ID, VanishTogglePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HandshakePayload.ID, HandshakePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(HandshakePayload.ID, (payload, context) -> {
            UUID playerUuid = context.player().getUuid();
            context.server().execute(() -> {
                AUTHENTICATED_CLIENTS.add(playerUuid);
                LOGGER.info("Player {} verified.", context.player().getName().getString());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(VanishTogglePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                if (AUTHENTICATED_CLIENTS.contains(player.getUuid())) {
                    boolean isVanishing = toggleVanish(player);
                    player.sendMessage(Text.literal(isVanishing ? "§cGhost Mode: ON" : "§aGhost Mode: OFF"), true);
                }
            });
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (UUID uuid : VANISHED_PLAYERS) {
                for (ServerWorld world : server.getWorlds()) {
                    ServerPlayerEntity player = world.getPlayerByUuid(uuid);
                    if (player != null) {
                        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BackroomsCommand.register(dispatcher);
        });
    }

    public static boolean toggleVanish(ServerPlayerEntity player) {
        MinecraftServer server = player.server;
        if (server == null) return false;
        ServerWorld world = (ServerWorld) player.getWorld();
        
        if (VANISHED_PLAYERS.contains(player.getUuid())) {
            server.getPlayerManager().broadcast(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD, player), false);
            world.getChunkManager().loadEntity(player);
            VANISHED_PLAYERS.remove(player.getUuid());
            return false;
        } else {
            world.getChunkManager().unloadEntity(player);
            server.getPlayerManager().broadcast(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE, player), false);
            VANISHED_PLAYERS.add(player.getUuid());
            return true;
        }
    }

    public static boolean isClientModded(UUID uuid) {
        return AUTHENTICATED_CLIENTS.contains(uuid);
    }
}
