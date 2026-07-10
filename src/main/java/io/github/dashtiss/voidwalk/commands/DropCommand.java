package io.github.dashtiss.voidwalk.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.BoolArgumentType;
import io.github.dashtiss.voidwalk.VoidWalk;
import io.github.dashtiss.voidwalk.managers.SupplyDropManager;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class DropCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("drop")
                .requires(source -> source.hasPermissionLevel(4))
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    ServerPlayerEntity player = source.getPlayer();
                    if (player == null) {
                        source.sendError(Text.literal("Only players can execute this without coordinates!"));
                        return 0;
                    }

                    UUID playerUuid = player.getUuid();
                    Long lastDrop = VoidWalk.DROP_COOLDOWNS.getOrDefault(playerUuid, 0L);
                    long now = System.currentTimeMillis();
                    long cooldownMs = VoidWalk.DROP_COOLDOWN_TICKS * 50;

                    if (now - lastDrop < cooldownMs) {
                        long secondsLeft = (cooldownMs - (now - lastDrop)) / 1000;
                        source.sendError(Text.literal("You must wait " + secondsLeft + " more seconds before calling another drop."));
                        return 0;
                    }

                    VoidWalk.DROP_COOLDOWNS.put(playerUuid, now);

                    BlockPos targetPos = player.getBlockPos();
                    ServerWorld world = player.getEntityWorld();

                    SupplyDropManager.spawnDropShip(world, targetPos, true);
                    source.sendFeedback(() -> Text.literal("§aSupply drop called successfully. Cooldown: 10 minutes."), false);
                    return 1;
                })
                .then(CommandManager.argument("coordinates", BlockPosArgumentType.blockPos())
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerWorld world = source.getWorld();

                            BlockPos targetPos = BlockPosArgumentType.getLoadedBlockPos(context, "coordinates");

                            SupplyDropManager.spawnDropShip(world, targetPos, true);
                            source.sendFeedback(() -> Text.literal("§aSupply drop called at coordinates."), false);
                            return 1;
                        })
                        .then(CommandManager.argument("announce", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    ServerWorld world = source.getWorld();

                                    BlockPos targetPos = BlockPosArgumentType.getLoadedBlockPos(context, "coordinates");
                                    boolean announce = BoolArgumentType.getBool(context, "announce");
                                    SupplyDropManager.spawnDropShip(world, targetPos, announce);
                                    source.sendFeedback(() -> Text.literal("§aSupply drop " + (announce ? "announced" : "silent") + "."), false);
                                    return 1;
                                })
                        )
                );
    }
}
