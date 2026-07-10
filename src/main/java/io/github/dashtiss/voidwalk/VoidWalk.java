package io.github.dashtiss.voidwalk;

import com.mojang.logging.LogUtils;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dashtiss.voidwalk.commands.BountyCommand;
import io.github.dashtiss.voidwalk.commands.VoidWalkCommand;
import io.github.dashtiss.voidwalk.commands.StatsCommand;
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
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class VoidWalk implements ModInitializer {

    public static final Set<UUID> AUTHENTICATED_CLIENTS = new HashSet<>();
    public static final Set<UUID> VANISHED_PLAYERS = new HashSet<>();
    public static boolean BOUNTY_ENABLED = false;
    public static final Map<UUID, Integer> KILL_COUNTS = Collections.synchronizedMap(new HashMap<>());
    public static final Map<UUID, Integer> DEATH_COUNTS = Collections.synchronizedMap(new HashMap<>());
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_FILE = "voidwalk_data.json";
    private static final Random RANDOM = new Random();
    private static int crateTimer = 0;
    private static int nextCrateTime = 36000 + RANDOM.nextInt(36000);

    // Cooldown tracking for drop commands
    public static final Map<UUID, Long> DROP_COOLDOWNS = new ConcurrentHashMap<>();
    public static final long DROP_COOLDOWN_TICKS = 12000; // 10 minutes

    // Bounty token item registration placeholder (not implemented yet)

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

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            // Handle kill counts for player kills
            if (entity instanceof ServerPlayerEntity victim) {
                ServerPlayerEntity killer = null;
                
                // Get the killer - handle both direct attacks and indirect damage
                if (damageSource.getAttacker() instanceof ServerPlayerEntity attacker) {
                    killer = attacker;
                } else if (damageSource.getAttacker() instanceof ServerPlayerEntity attacker) {
                    killer = attacker;
                }
                
                // Record kills for the killer
                if (killer != null) {
                    if (BOUNTY_ENABLED) {
                        KILL_COUNTS.put(killer.getUuid(), KILL_COUNTS.getOrDefault(killer.getUuid(), 0) + 1);
                        killer.sendMessage(Text.literal("§6[Bounty] §fKill recorded! Your total: " + KILL_COUNTS.get(killer.getUuid())), false);
                    }
                    // Handle bounty rewards
                    BountyManager.handlePlayerDeath(victim, killer);
                    // Broadcast custom death message
                    broadcastDeathMessage(victim, killer, damageSource);
                } else {
                    // Player died to non-player source (mob, environmental, etc.)
                    broadcastDeathMessage(victim, null, damageSource);
                }
                
                // Record death for the victim
                DEATH_COUNTS.put(victim.getUuid(), DEATH_COUNTS.getOrDefault(victim.getUuid(), 0) + 1);
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
            StatsCommand.register(dispatcher, registryAccess);
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            SupplyDropManager.tickActiveDrops();
            server.getWorlds().forEach(SupplyDropManager::tickLockedDrops);

            crateTimer++;

            if (crateTimer >= nextCrateTime) {

                crateTimer = 0;

                nextCrateTime = 36000 + RANDOM.nextInt(36000);

                SupplyDropManager.attemptRandomLootCrate(server.getOverworld());
            }
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

            if (world.isClient()) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();

            if (SupplyDropManager.handleChestInteraction((ServerPlayerEntity) player, pos)) {
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }

    public static void toggleVanish(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();
        MinecraftServer server = player.getEntityWorld().getServer();

        if (server == null) {
            LOGGER.warn("Failed to toggle vanish - no server available for player {}", player.getUuid());
            return;
        }

        if (VANISHED_PLAYERS.contains(playerUuid)) {

            // ==========================
            // REAPPEAR
            // ==========================

            VANISHED_PLAYERS.remove(playerUuid);

            player.setInvisible(false);

            for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {

                if (other == player) continue;

                other.networkHandler.sendPacket(new PlayerListS2CPacket(EnumSet.of(PlayerListS2CPacket.Action.ADD_PLAYER), List.of(player)));
            }

            player.sendMessage(Text.literal("§aYou have reappeared."), true);

        } else {

            // ==========================
            // VANISH
            // ==========================

            VANISHED_PLAYERS.add(playerUuid);

            player.setInvisible(true);

            for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {

                if (other == player) continue;

                other.networkHandler.sendPacket(new PlayerRemoveS2CPacket(List.of(player.getUuid())));
            }

            player.sendMessage(Text.literal("§7You have slipped into the shadows..."), true);
        }
        LOGGER.info("Player {} toggled vanish state", player.getName().getString());
    }

    public static void hallucination(PlayerEntity player) {
        World world = player.getEntityWorld();

        // 1. Define the 30-block range vectors
        Vec3d eyePosition = player.getEyePos();
        Vec3d lookVector = player.getRotationVec(1.0F);
        Vec3d endPosition = eyePosition.add(lookVector.multiply(30.0)); // 30 blocks away

        // 2. Build the context setup
        RaycastContext context = new RaycastContext(eyePosition, endPosition, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, // Ignores water/lava blocks
                player);

        // 3. Execute the raycast
        BlockHitResult hitResult = world.raycast(context);

        // 4. Check if we actually hit a solid block
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = hitResult.getBlockPos();

            // 5. Play the sound (Server-safe wrapper method)
            // This plays the sound at the block pos for everyone nearby, except the target player parameter if specified.
            // Passing 'null' as the first parameter ensures EVERYONE (including the casting player) hears it.
            world.playSound(null,                             // Except player (null = play for everyone)
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
                writer.println("KILL:" + entry.getKey() + ":" + entry.getValue());
            }
            for (Map.Entry<UUID, Integer> entry : DEATH_COUNTS.entrySet()) {
                writer.println("DEATH:" + entry.getKey() + ":" + entry.getValue());
            }
            LOGGER.info("Bounty data saved: {} kill records, {} death records", KILL_COUNTS.size(), DEATH_COUNTS.size());
        } catch (IOException e) {
            LOGGER.error("Failed to save bounty data to {}", DATA_FILE, e);
        }
    }

    private static void loadBountyData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            LOGGER.info("No existing bounty data found at {}. Starting fresh.", DATA_FILE);
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("BOUNTY_ENABLED:")) {
                    BOUNTY_ENABLED = Boolean.parseBoolean(line.split(":")[1]);
                } else if (line.startsWith("KILL:") && line.split(":").length >= 3) {
                    String[] parts = line.split(":");
                    KILL_COUNTS.put(UUID.fromString(parts[1]), Integer.parseInt(parts[2]));
                } else if (line.startsWith("DEATH:") && line.split(":").length >= 3) {
                    String[] parts = line.split(":");
                    DEATH_COUNTS.put(UUID.fromString(parts[1]), Integer.parseInt(parts[2]));
                }
            }
            LOGGER.info("Loaded {} kill records and {} death records from bounty data", KILL_COUNTS.size(), DEATH_COUNTS.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load bounty data from {}", DATA_FILE, e);
        }
    }

    public static boolean isClientModded(UUID uuid) {
        return AUTHENTICATED_CLIENTS.contains(uuid);
    }

    /**
     * Broadcasts custom purge-themed death messages
     */
    public static void broadcastDeathMessage(ServerPlayerEntity victim, ServerPlayerEntity killer, DamageSource damageSource) {
        MinecraftServer server = victim.getEntityWorld().getServer();
        if (server == null) return;

        String victimName = victim.getName().getString();
        Text deathMessage;

        if (killer != null) {
            String killerName = killer.getName().getString();
            // Player killed by player - The Purge style
            deathMessage = Text.literal("§4☠ §c" + victimName + " §7was §4PURGED §7by §c" + killerName + "§7!");
        } else {
            // Player died to non-player source (mob, environmental, etc.)
            Entity trueSource = damageSource.getAttacker();
            String cause = getDeathCause(damageSource, trueSource);
            
            if (cause != null) {
                deathMessage = Text.literal("§4☠ §c" + victimName + " §7was §celiminated §7by " + cause + "§7!");
            } else if (damageSource.isOf(DamageTypes.OUT_OF_WORLD)) {
                deathMessage = Text.literal("§4☠ §c" + victimName + " §7§operished in the void§7...");
            } else if (damageSource.isOf(net.minecraft.entity.damage.DamageTypes.GENERIC)) {
                deathMessage = Text.literal("§4☠ §c" + victimName + " §7§operished in the void§7...");
            } else {
                deathMessage = Text.literal("§4☠ §c" + victimName + " §7§ldied§7!");
            }
        }

        server.getPlayerManager().broadcast(deathMessage, false);
        LOGGER.debug("Death message broadcasted for {}", victimName);
    }

    /**
     * Gets a formatted death cause string for non-player deaths
     */
    private static String getDeathCause(DamageSource damageSource, Entity trueSource) {
        if (trueSource != null) {
            // Mob or entity killed player - capitalize first letter
            String entityName = trueSource.getName().getString();
            if (!entityName.isEmpty()) {
                entityName = entityName.substring(0, 1).toUpperCase() + entityName.substring(1);
            }
            return "§e" + entityName;
        }

        // Handle specific damage types
        if (damageSource.isOf(net.minecraft.entity.damage.DamageTypes.DROWN)) {
            return "§bDrowned§7";
        } else if (damageSource.isOf(net.minecraft.entity.damage.DamageTypes.FALL)) {
            return "§7fall damage§7";
        } else if (damageSource.isOf(net.minecraft.entity.damage.DamageTypes.ON_FIRE)) {
            return "§6flames§7";
        } else if (damageSource.isOf(net.minecraft.entity.damage.DamageTypes.CACTUS)) {
            return "§aa cactus§7";
        } else if (damageSource.isOf(net.minecraft.entity.damage.DamageTypes.LIGHTNING_BOLT)) {
            return "§b§lLIGHTNING§7";
        } else if (damageSource.isOf(net.minecraft.entity.damage.DamageTypes.STARVE)) {
            return "§5starvation§7";
        } else if (damageSource.isOf(net.minecraft.entity.damage.DamageTypes.WITHER)) {
            return "§dWither§7";
        } else if (damageSource.isOf(net.minecraft.entity.damage.DamageTypes.MAGIC)) {
            return "§dmagic§7";
        } else if (damageSource.isOf(net.minecraft.entity.damage.DamageTypes.EXPLOSION)) {
            return "§ean explosion§7";
        } else if (damageSource.isOf(net.minecraft.entity.damage.DamageTypes.ARROW)) {
            return "§ean arrow§7";
        } else if (damageSource.isOf(net.minecraft.entity.damage.DamageTypes.MOB_PROJECTILE)) {
            return "§ea projectile§7";
        }

        return null;
    }

    /**
     * Get formatted kill source for death messages
     */
    public static String getKillSourceDescription(DamageSource damageSource) {
        Entity attacker = damageSource.getAttacker();
        if (attacker != null) {
            String name = attacker.getName().getString();
            if (!name.isEmpty()) {
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
            }
            return name;
        }
        
        if (damageSource.isOf(DamageTypes.DROWN)) return "Drowned";
        if (damageSource.isOf(DamageTypes.FALL)) return "Fall Damage";
        if (damageSource.isOf(DamageTypes.ON_FIRE)) return "Fire";
        if (damageSource.isOf(DamageTypes.CACTUS)) return "Cactus";
        if (damageSource.isOf(DamageTypes.LIGHTNING_BOLT)) return "Lightning";
        if (damageSource.isOf(DamageTypes.STARVE)) return "Starvation";
        if (damageSource.isOf(DamageTypes.WITHER)) return "Wither";
        if (damageSource.isOf(DamageTypes.MAGIC)) return "Magic";
        if (damageSource.isOf(DamageTypes.EXPLOSION)) return "Explosion";
        if (damageSource.isOf(DamageTypes.OUT_OF_WORLD)) return "The Void";
        
        return "Unknown";
    }

    /**
     * Get kill stats for a player
     */
    public static PlayerStats getPlayerStats(UUID uuid) {
        int kills = KILL_COUNTS.getOrDefault(uuid, 0);
        int deaths = DEATH_COUNTS.getOrDefault(uuid, 0);
        double kdr = deaths > 0 ? (double) kills / deaths : kills;
        int bountyClaims = BountyManager.getActiveBounties().getOrDefault(uuid, 0);
        
        return new PlayerStats(kills, deaths, kdr, bountyClaims);
    }

    public static class PlayerStats {
        public final int kills;
        public final int deaths;
        public final double kdr;
        public final int currentBounty;

        public PlayerStats(int kills, int deaths, double kdr, int currentBounty) {
            this.kills = kills;
            this.deaths = deaths;
            this.kdr = kdr;
            this.currentBounty = currentBounty;
        }
    }
}
