package io.github.dashtiss.voidwalk.managers;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class SupplyDropManager {

    private static class ActiveDrop {
        ArmorStandEntity shipEntity;
        BlockPos targetPos;
        double currentY;
        ServerWorld world;

        ActiveDrop(ArmorStandEntity shipEntity, BlockPos targetPos, ServerWorld world) {
            this.shipEntity = shipEntity;
            this.targetPos = targetPos;
            this.currentY = shipEntity.getY();
            this.world = world;
        }
    }
    private static class LockedDrop {
        BlockPos chestPos;
        UUID capturingPlayer;
        int captureTicks;

        LockedDrop(BlockPos chestPos) {
            this.chestPos = chestPos;
            this.capturingPlayer = null;
            this.captureTicks = 0;
        }
    }

    // FIXED: Switched to a thread-safe list to prevent silent loop crashes during tick iterations
    private static final CopyOnWriteArrayList<ActiveDrop> ACTIVE_DROPS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<LockedDrop> LOCKED_DROPS =
            new CopyOnWriteArrayList<>();

    public static void spawnDropShip(ServerWorld world, BlockPos targetPos, boolean announce) {
        // Force the spawn height to be exactly 80 blocks above target ground level
        double spawnY = targetPos.getY() + 80;
        Vec3d spawnVec = new Vec3d(targetPos.getX() + 0.5, spawnY, targetPos.getZ() + 0.5);

        ArmorStandEntity dropShip = new ArmorStandEntity(world, spawnVec.x, spawnVec.y, spawnVec.z);
        dropShip.setInvisible(true);
        dropShip.setNoGravity(true);
        dropShip.setInvulnerable(true);
        dropShip.setCustomName(Text.literal("§6§lSUPPLY DROP CAPSULE"));
        dropShip.setCustomNameVisible(true);

        dropShip.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.RESPAWN_ANCHOR));

        boolean spawned = world.spawnEntity(dropShip);

        // Debug logger to console
        System.out.println("[VoidWalk] Spawning drop capsule at Y: " + spawnY + " | Success: " + spawned);

        if (spawned) {
            ACTIVE_DROPS.add(new ActiveDrop(dropShip, targetPos, world));
            if (announce)
                world.getServer().getPlayerManager().broadcast(
                        Text.literal("§e§l[!] §6A supply drop-ship has entered the atmosphere at X: "
                                + targetPos.getX() + " Z: " + targetPos.getZ() + "!"), false
                );
        }
    }

    public static void tickActiveDrops() {
        if (ACTIVE_DROPS.isEmpty()) return;

        for (ActiveDrop drop : ACTIVE_DROPS) {
            // Safety: if entity was cleared outside of our logic, dump it
            if (drop.shipEntity == null || !drop.shipEntity.isAlive()) {
                ACTIVE_DROPS.remove(drop);
                continue;
            }

            // --- 1. SMOOTH DESCENT ---
            // 0.25 blocks per tick = 5 blocks per second descent speed
            drop.currentY -= 0.25;
            drop.shipEntity.refreshPositionAndAngles(
                    drop.shipEntity.getX(), drop.currentY, drop.shipEntity.getZ(), 0, 0
            );

            // --- 2. BEACON PARTICLE COLUMN ---
            // Spawns a nice line pointing up towards the sky
            for (int h = 0; h < 30; h += 3) {
                drop.world.spawnParticles(
                        ParticleTypes.END_ROD,
                        drop.shipEntity.getX(),
                        drop.currentY + h,
                        drop.shipEntity.getZ(),
                        1, 0.0, 0.0, 0.0, 0.0
                );
            }

            // Engine trail smoke
            drop.world.spawnParticles(
                    ParticleTypes.SMOKE,
                    drop.shipEntity.getX(), drop.currentY + 1.5, drop.shipEntity.getZ(),
                    2, 0.1, 0.1, 0.1, 0.01
            );

            // --- 3. LANDING DETECTION ---
            if (!(drop.world.getBlockState(new BlockPos(drop.shipEntity.getBlockX(), drop.shipEntity.getBlockY() - 1, drop.shipEntity.getBlockZ())).isAir())) {
                System.out.println("[VoidWalk] Capsule reached target Y: " + drop.targetPos.getY() + ". Executing landing.");
                drop.shipEntity.discard();
                executeLandingSequence(drop.world, new BlockPos(drop.shipEntity.getBlockX(), drop.shipEntity.getBlockY(), drop.shipEntity.getBlockZ()));
                ACTIVE_DROPS.remove(drop);
            }
        }
    }

    private static void executeLandingSequence(ServerWorld world, BlockPos pos) {
        // Place the physical chest down
        world.setBlockState(pos, Blocks.CHEST.getDefaultState());
        LOCKED_DROPS.add(new LockedDrop(pos));
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof ChestBlockEntity chest) {
            RegistryKey<LootTable> lootTableKey = RegistryKey.of(
                    RegistryKeys.LOOT_TABLE,
                    Identifier.of("voidwalk", "chests/supply_drop")
            );
            chest.setLootTable(lootTableKey, world.getRandom().nextLong());
            chest.markDirty();
        }

        // Landing impacts explosions
        world.spawnParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 25, 0.3, 0.8, 0.3, 0.03);

        world.getServer().getPlayerManager().broadcast(
                Text.literal("§c§l[!] §eThe Supply Drop has touched down safely at X: " + pos.getX() + " Z: " + pos.getZ() + "!"), false
        );
    }
    public static boolean handleChestInteraction(
            ServerPlayerEntity player,
            BlockPos pos
    ) {
        for (LockedDrop drop : LOCKED_DROPS) {

            if (!drop.chestPos.equals(pos))
                continue;

            if (drop.capturingPlayer == null) {

                drop.capturingPlayer = player.getUuid();
                drop.captureTicks = 0;

                player.getEntityWorld().getServer().getPlayerManager().broadcast(
                        Text.literal(
                                "§6§l[SUPPLY DROP] §e"
                                        + player.getName().getString()
                                        + " has begun capturing a supply drop!"
                        ),
                        false
                );

                return true;
            }

            if (drop.capturingPlayer.equals(player.getUuid())) {

                player.sendMessage(
                        Text.literal("§eAlready capturing this drop."),
                        true
                );

                return true;
            }

            player.sendMessage(
                    Text.literal(
                            "§cSupply drop is currently being captured."
                    ),
                    true
            );

            return true;
        }

        return false;
    }
    public static void tickLockedDrops(ServerWorld world) {

        if (LOCKED_DROPS.isEmpty())
            return;

        for (LockedDrop drop : LOCKED_DROPS) {

            if (drop.capturingPlayer == null)
                continue;

            ServerPlayerEntity player =
                    world.getServer()
                            .getPlayerManager()
                            .getPlayer(drop.capturingPlayer);

            if (player == null || !player.isAlive()) {

                drop.capturingPlayer = null;
                drop.captureTicks = 0;

                continue;
            }

            double distance =
                    player.getBlockPos()
                            .getSquaredDistance(drop.chestPos);

            if (distance > 25) {

                player.sendMessage(
                        Text.literal(
                                "§cSupply drop capture failed."
                        ),
                        true
                );

                drop.capturingPlayer = null;
                drop.captureTicks = 0;

                continue;
            }

            drop.captureTicks++;

            if (drop.captureTicks % 20 == 0) {

                int percent =
                        (drop.captureTicks * 100) / 600;

                player.sendMessage(
                        Text.literal(
                                "§6Capturing Supply Drop: "
                                        + percent + "%"
                        ),
                        true
                );
            }

            if (drop.captureTicks >= 600) {

                LOCKED_DROPS.remove(drop);

                world.getServer()
                        .getPlayerManager()
                        .broadcast(
                                Text.literal(
                                        "§a§l[SUPPLY DROP] §e"
                                                + player.getName().getString()
                                                + " captured the supply drop!"
                                ),
                                false
                        );
            }
        }
    }
    public static void attemptRandomLootCrate(ServerWorld world) {

        var players = world.getPlayers();

        if (players.size() < 5)
            return;

        double avgX = 0;
        double avgZ = 0;

        for (var player : players) {
            avgX += player.getX();
            avgZ += player.getZ();
        }

        avgX /= players.size();
        avgZ /= players.size();

        double angle = world.random.nextDouble() * Math.PI * 2;

        double distance =
                500 + world.random.nextInt(251);

        int targetX =
                (int) Math.round(
                        avgX + Math.cos(angle) * distance
                );

        int targetZ =
                (int) Math.round(
                        avgZ + Math.sin(angle) * distance
                );

        int targetY =
                world.getTopY(
                        net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                        targetX,
                        targetZ
                );

        BlockPos cratePos =
                new BlockPos(
                        targetX,
                        targetY,
                        targetZ
                );

        spawnLootCrate(world, cratePos);
    }
    private static void spawnLootCrate(
            ServerWorld world,
            BlockPos pos
    ) {

        world.setBlockState(
                pos,
                Blocks.BARREL.getDefaultState()
        );

        BlockEntity blockEntity =
                world.getBlockEntity(pos);

        if (blockEntity instanceof BarrelBlockEntity barrel) {

            RegistryKey<LootTable> lootTableKey =
                    RegistryKey.of(
                            RegistryKeys.LOOT_TABLE,
                            Identifier.of(
                                    "voidwalk",
                                    "chests/random_crate"
                            )
                    );

            barrel.setLootTable(
                    lootTableKey,
                    world.getRandom().nextLong()
            );

            barrel.markDirty();
        }

        world.spawnParticles(
                ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                pos.getX() + 0.5,
                pos.getY() + 1,
                pos.getZ() + 0.5,
                50,
                0.3,
                1,
                0.3,
                0.03
        );

        world.getServer()
                .getPlayerManager()
                .broadcast(
                        Text.literal(
                                "§6§l[LOOT CACHE] §eA hidden cache has been detected!"
                                        + "\n§fX: §b" + pos.getX()
                                        + " §fZ: §b" + pos.getZ()
                        ),
                        false
                );
    }
}