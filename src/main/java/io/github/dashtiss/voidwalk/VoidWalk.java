package io.github.dashtiss.voidwalk;

import io.github.dashtiss.voidwalk.commands.BountyCommand;
import io.github.dashtiss.voidwalk.commands.VoidWalkCommand;
import io.github.dashtiss.voidwalk.managers.BountyManager;
import io.github.dashtiss.voidwalk.managers.SupplyDropManager;
import io.github.dashtiss.voidwalk.payloads.HandshakePayload;
import io.github.dashtiss.voidwalk.payloads.VanishTogglePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;

public class VoidWalk implements ModInitializer {

    public static final Set<UUID> AUTHENTICATED_CLIENTS = new HashSet<>();
    public static final Set<UUID> VANISHED_PLAYERS = new HashSet<>();
    public static boolean BOUNTY_ENABLED = false;
    public static final Map<UUID, Integer> KILL_COUNTS = new HashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_FILE = "backrooms_bounty.txt";
    private static final Random RANDOM = new Random();
    private static int crateTimer = 0;
    private static int nextCrateTime =
            36000 + RANDOM.nextInt(36000);

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(HandshakePayload.ID, HandshakePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(VanishTogglePayload.ID, VanishTogglePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HandshakePayload.ID, HandshakePayload.CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> loadBountyData());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> saveBountyData());
        ServerLifecycleEvents.SERVER_STARTED.register(BountyManager::loadBounties);

        // 2. HOOK: Fires right during the midnight shutdown process before chunks unload
        ServerLifecycleEvents.SERVER_STOPPING.register(BountyManager::saveBounties);

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (BOUNTY_ENABLED && source.getAttacker() instanceof ServerPlayerEntity attacker) {
                if (entity instanceof ServerPlayerEntity) {
                    KILL_COUNTS.put(attacker.getUuid(), KILL_COUNTS.getOrDefault(attacker.getUuid(), 0) + 1);
                    attacker.sendMessage(Text.literal("§6[Bounty] §fKill recorded! Your total: " + KILL_COUNTS.get(attacker.getUuid())), false);
                }
            }
            // will handle bounty deaths
            if (source.getAttacker() instanceof ServerPlayerEntity killer) {
                if (entity.isPlayer())
                    BountyManager.handlePlayerDeath((ServerPlayerEntity) entity, killer);
            }
        });

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
                    toggleVanish(player);
                }
            });
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0) return;
            for (UUID vanishedUuid : VANISHED_PLAYERS) {
                for (net.minecraft.server.world.ServerWorld world : server.getWorlds()) {
                    ServerPlayerEntity vanishedPlayer = (ServerPlayerEntity) world.getPlayerByUuid(vanishedUuid);
                    if (vanishedPlayer == null) continue;
                    for (ServerPlayerEntity otherPlayer : world.getPlayers()) {
                        if (VANISHED_PLAYERS.contains(otherPlayer.getUuid())) continue;
                        double distance = vanishedPlayer.distanceTo(otherPlayer);
                        if (distance < 15.0) {
                            float volume = (float) Math.max(0.1, 1.0 - (distance / 15.0));
                            float pitch = (float) Math.max(0.5, 1.5 - (distance / 15.0));
                            otherPlayer.playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, volume, pitch);
                        }
                    }
                }
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            VoidWalkCommand.register(dispatcher, registryAccess);
            BountyCommand.register(dispatcher);
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            SupplyDropManager.tickActiveDrops();
            server.getWorlds().forEach(
                    SupplyDropManager::tickLockedDrops
            );

            crateTimer++;

            if (crateTimer >= nextCrateTime) {

                crateTimer = 0;

                nextCrateTime =
                        36000 + RANDOM.nextInt(36000);

                SupplyDropManager.attemptRandomLootCrate(
                        server.getOverworld()
                );
            }
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

            if (world.isClient())
                return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();

            if (SupplyDropManager.handleChestInteraction(
                    (ServerPlayerEntity) player,
                    pos
            )) {
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }

    public static void toggleVanish(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();
        MinecraftServer server = player.getEntityWorld().getServer();
        ServerWorld world = (ServerWorld) player.getEntityWorld();

        if (VANISHED_PLAYERS.contains(playerUuid)) {
            // --- REAPPEAR LOGIC ---
            VANISHED_PLAYERS.remove(playerUuid);

            // 1. Tell everyone's client tracking system to reload your player profile
            PlayerListS2CPacket addPacket = new PlayerListS2CPacket(
                    EnumSet.of(PlayerListS2CPacket.Action.ADD_PLAYER),
                    List.of(player)
            );
            server.getPlayerManager().sendToAll(addPacket);

            // 2. Force the server chunk manager to load and broadcast your physical avatar entity again
            world.getChunkManager().loadEntity(player);

            player.sendMessage(Text.literal("§aYou have reappeared."), true);
        } else {
            // --- VANISH LOGIC ---
            VANISHED_PLAYERS.add(playerUuid);

            // 1. Force the server chunk manager to stop sending your entity updates to other clients
            world.getChunkManager().unloadEntity(player);

            // 2. FIXED: Pass the player list collection directly into the constructor!
            PlayerListS2CPacket removePacket = new PlayerListS2CPacket(
                    EnumSet.of(PlayerListS2CPacket.Action.UPDATE_LISTED),
                    List.of(player)
            );
            server.getPlayerManager().sendToAll(removePacket);

            player.sendMessage(Text.literal("§7You have slipped into the shadows..."), true);
        }
    }

    public static void hallucination(PlayerEntity player) {
        World world = player.getEntityWorld();

        // 1. Define the 30-block range vectors
        Vec3d eyePosition = player.getEyePos();
        Vec3d lookVector = player.getRotationVec(1.0F);
        Vec3d endPosition = eyePosition.add(lookVector.multiply(30.0)); // 30 blocks away

        // 2. Build the context setup
        RaycastContext context = new RaycastContext(
                eyePosition,
                endPosition,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE, // Ignores water/lava blocks
                player
        );

        // 3. Execute the raycast
        BlockHitResult hitResult = world.raycast(context);

        // 4. Check if we actually hit a solid block
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = hitResult.getBlockPos();

            // 5. Play the sound (Server-safe wrapper method)
            // This plays the sound at the block pos for everyone nearby, except the target player parameter if specified.
            // Passing 'null' as the first parameter ensures EVERYONE (including the casting player) hears it.
            world.playSound(
                    null,                             // Except player (null = play for everyone)
                    hitPos,                           // The block position coordinates
                    SoundEvents.BLOCK_GLASS_STEP,       // The SoundEvent asset identifier
                    SoundCategory.MASTER,             // Volume slider assignment group
                    1.0F,                             // Volume multiplier (1.0 = standard 16 block radius)
                    1.0F                              // Pitch multiplier (1.0 = normal speed)
            );
        }
    }

    private static void saveBountyData() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE))) {
            writer.println("BOUNTY_ENABLED:" + BOUNTY_ENABLED);
            for (Map.Entry<UUID, Integer> entry : KILL_COUNTS.entrySet()) {
                writer.println(entry.getKey() + ":" + entry.getValue());
            }
        } catch (IOException e) { LOGGER.error("Failed to save bounty data", e); }
    }

    private static void loadBountyData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("BOUNTY_ENABLED:")) {
                    BOUNTY_ENABLED = Boolean.parseBoolean(line.split(":")[1]);
                } else if (line.contains(":")) {
                    String[] parts = line.split(":");
                    KILL_COUNTS.put(UUID.fromString(parts[0]), Integer.parseInt(parts[1]));
                }
            }
        } catch (Exception e) { LOGGER.error("Failed to load bounty data", e); }
    }

    public static boolean isClientModded(UUID uuid) {
        return AUTHENTICATED_CLIENTS.contains(uuid);
    }
}
