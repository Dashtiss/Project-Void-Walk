package io.github.dashtiss.voidwalk.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.dashtiss.voidwalk.ConfigManager;
import io.github.dashtiss.voidwalk.VoidWalk;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import java.util.Set;

public class AdminCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("admin")
                .requires(source -> source.hasPermissionLevel(4))

                // ==========================================
                // RELOAD CONFIG: /voidwalk reload
                // ==========================================
                .then(CommandManager.literal("reload")
                        .executes(context -> {
                            ConfigManager.reloadConfig();
                            context.getSource().sendFeedback(() -> Text.literal("§a[VoidWalk] Configuration reloaded!"), false);
                            VoidWalk.LOGGER.info("Config reloaded by {}", context.getSource().getName());
                            return 1;
                        })
                )

                // ==========================================
                // PURGE CONTROL: /voidwalk purge start|stop
                // ==========================================
                .then(CommandManager.literal("purge")
                        .then(CommandManager.literal("start")
                                .executes(context -> {
                                    VoidWalk.BOUNTY_ENABLED = true;
                                    context.getSource().getServer().getPlayerManager().broadcast(
                                            Text.literal("§4§l⚠ THE PURGE HAS BEGUN! ⚠\n§cAll bounties are now active! Kill or be killed!"),
                                            false
                                    );
                                    VoidWalk.LOGGER.info("Purge started by {}", context.getSource().getName());
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("stop")
                                .executes(context -> {
                                    VoidWalk.BOUNTY_ENABLED = false;
                                    context.getSource().getServer().getPlayerManager().broadcast(
                                            Text.literal("§a§lThe Purge has ended. Stay safe."),
                                            false
                                    );
                                    VoidWalk.LOGGER.info("Purge stopped by {}", context.getSource().getName());
                                    return 1;
                                })
                        )
                )

                // ==========================================
                // BOUNTY CONTROL: /voidwalk bounty toggle|reset|resetall
                // ==========================================
                .then(CommandManager.literal("bounty")
                        .then(CommandManager.literal("toggle")
                                .executes(context -> {
                                    VoidWalk.BOUNTY_ENABLED = !VoidWalk.BOUNTY_ENABLED;
                                    String status = VoidWalk.BOUNTY_ENABLED ? "§aENABLED" : "§cDISABLED";
                                    context.getSource().sendFeedback(() -> Text.literal("§6[Bounty] §fBounty System: " + status), true);
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("reset")
                                .executes(context -> {
                                    VoidWalk.KILL_COUNTS.clear();
                                    VoidWalk.DEATH_COUNTS.clear();
                                    context.getSource().sendFeedback(() -> Text.literal("§6[Bounty] §fStats leaderboard reset."), true);
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("resetall")
                                .executes(context -> {
                                    VoidWalk.KILL_COUNTS.clear();
                                    VoidWalk.DEATH_COUNTS.clear();
                                    io.github.dashtiss.voidwalk.managers.BountyManager.getActiveBounties().clear();
                                    context.getSource().sendFeedback(() -> Text.literal("§c§l[Admin] All bounty data has been wiped!"), true);
                                    return 1;
                                })
                        )
                )

                // ==========================================
                // PLAYER MANAGEMENT: /voidwalk heal|feed|clearinv|teleport
                // ==========================================
                .then(CommandManager.literal("heal")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player != null) {
                                player.setHealth(player.getMaxHealth());
                                player.getHungerManager().setFoodLevel(20);
                                context.getSource().sendFeedback(() -> Text.literal("§aYou have been healed!"), false);
                            }
                            return 1;
                        })
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                    target.setHealth(target.getMaxHealth());
                                    target.getHungerManager().setFoodLevel(20);
                                    context.getSource().sendFeedback(() -> Text.literal("§aHealed " + target.getName().getString()), false);
                                    target.sendMessage(Text.literal("§aYou have been healed by an admin!"), false);
                                    return 1;
                                })
                        )
                )

                .then(CommandManager.literal("feed")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player != null) {
                                player.getHungerManager().setFoodLevel(20);
                                context.getSource().sendFeedback(() -> Text.literal("§aYou have been fed!"), false);
                            }
                            return 1;
                        })
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                    target.getHungerManager().setFoodLevel(20);
                                    context.getSource().sendFeedback(() -> Text.literal("§aFed " + target.getName().getString()), false);
                                    target.sendMessage(Text.literal("§aYou have been fed by an admin!"), false);
                                    return 1;
                                })
                        )
                )

                .then(CommandManager.literal("clearinv")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player != null) {
                                player.getInventory().clear();
                                player.currentScreenHandler.syncState();
                                context.getSource().sendFeedback(() -> Text.literal("§aYour inventory has been cleared!"), false);
                            }
                            return 1;
                        })
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                    target.getInventory().clear();
                                    target.currentScreenHandler.syncState();
                                    context.getSource().sendFeedback(() -> Text.literal("§aCleared inventory of " + target.getName().getString()), false);
                                    target.sendMessage(Text.literal("§cYour inventory has been cleared by an admin!"), false);
                                    return 1;
                                })
                        )
                )

                .then(CommandManager.literal("teleport")
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                    if (player != null) {
                                        player.teleport((ServerWorld)target.getEntityWorld(), target.getX(), target.getY(), target.getZ(), Set.of(PositionFlag.DELTA_X, PositionFlag.DELTA_Y, PositionFlag.DELTA_Z, PositionFlag.Y_ROT, PositionFlag.X_ROT), target.getYaw(), target.getPitch(), false);
                                        context.getSource().sendFeedback(() -> Text.literal("§aTeleported to " + target.getName().getString()), false);
                                    }
                                    return 1;
                                })
                        )
                );
    }
}
