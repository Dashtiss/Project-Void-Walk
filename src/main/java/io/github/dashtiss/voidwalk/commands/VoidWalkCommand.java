package io.github.dashtiss.voidwalk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.dashtiss.voidwalk.ConfigManager;
import io.github.dashtiss.voidwalk.VoidWalk;
import io.github.dashtiss.voidwalk.managers.BountyManager;
import io.github.dashtiss.voidwalk.managers.SupplyDropManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class VoidWalkCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("voidwalk")
                // ==========================================
                // 1. IsModded check
                // ==========================================
                .then(CommandManager.literal("IsModded")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity player = source.getPlayer();
                            if (player == null) {
                                source.sendError(Text.literal("Only players can execute this command!"));
                                return 0;
                            }

                            boolean isModded = VoidWalk.isClientModded(player.getUuid());
                            String message = isModded ? "You have the Project VoidWalk Mod installed!" : "You do NOT have the Project VoidWalk Mod installed!";
                            source.sendFeedback(() -> Text.literal(message), false);
                            return 1;
                        })
                )

                // ==========================================
                // 2. Supply Drop: /voidwalk drop
                // ==========================================
                .then(DropCommand.build())

                // ==========================================
                // 3. Vanish: /voidwalk vanish
                // ==========================================
                .then(VanishCommand.build())

                // ==========================================
                // 4. Admin: RELOAD
                // ==========================================
                .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            ConfigManager.reloadConfig();
                            context.getSource().sendFeedback(() -> Text.literal("§a[VoidWalk] Configuration reloaded!"), false);
                            VoidWalk.LOGGER.info("Config reloaded by {}", context.getSource().getName());
                            return 1;
                        })
                )

                // ==========================================
                // 5. Admin: BOUNTY CONTROL
                // ==========================================
                .then(CommandManager.literal("bounty")
                        .requires(source -> source.hasPermissionLevel(4))
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
                                    BountyManager.getActiveBounties().clear();
                                    context.getSource().sendFeedback(() -> Text.literal("§c§l[Admin] All bounty data has been wiped!"), true);
                                    return 1;
                                })
                        )
                )

                // ==========================================
                // 6. Admin: CACHE (trigger random loot crate)
                // ==========================================
                .then(CommandManager.literal("cache")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player == null) return 0;
                            SupplyDropManager.attemptRandomLootCrate(player.getEntityWorld());
                            context.getSource().sendFeedback(() -> Text.literal("§aRandom loot cache triggered."), false);
                            return 1;
                        })
                )

                // ==========================================
                // 7. Admin: PURGE CONTROL
                // ==========================================
                .then(CommandManager.literal("purge")
                        .requires(source -> source.hasPermissionLevel(4))
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
                // 8. Admin: PLAYER MANAGEMENT
                // ==========================================
                .then(CommandManager.literal("heal")
                        .requires(source -> source.hasPermissionLevel(4))
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
                        .requires(source -> source.hasPermissionLevel(4))
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
                        .requires(source -> source.hasPermissionLevel(4))
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
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                    if (player != null) {
                                        player.teleport((ServerWorld) target.getEntityWorld(), target.getX(), target.getY(), target.getZ(), Set.of(PositionFlag.DELTA_X, PositionFlag.DELTA_Y, PositionFlag.DELTA_Z, PositionFlag.Y_ROT, PositionFlag.X_ROT), target.getYaw(), target.getPitch(), false);
                                        context.getSource().sendFeedback(() -> Text.literal("§aTeleported to " + target.getName().getString()), false);
                                    }
                                    return 1;
                                })
                        )
                )

                // ==========================================
                // 9. Stats: /voidwalk stats [player]
                // ==========================================
                .then(CommandManager.literal("stats")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity player = source.getPlayer();
                            if (player == null) {
                                source.sendError(Text.literal("Only players can use this command!"));
                                return 0;
                            }
                            return executeStats(source, player);
                        })
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .requires(src -> src.hasPermissionLevel(4))
                                .executes(context -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                    return executeStats(context.getSource(), target);
                                })
                        )
                )

                // ==========================================
                // 10. Top Killers: /voidwalk topkillers
                // ==========================================
                .then(CommandManager.literal("topkillers")
                        .executes(context -> {
                            return executeTopKillers(context.getSource());
                        })
                )

                // ==========================================
                // 11. Debug: /voidwalk debug
                // ==========================================
                .then(DebugCommand.build())

                // ==========================================
                // 12. God Mode: /voidwalk god (legacy, owner-only)
                // ==========================================
                .then(CommandManager.literal("god")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.literal("heal")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    if (player != null) {
                                        player.setHealth(player.getMaxHealth());
                                        context.getSource().sendFeedback(() -> Text.literal("God Mode: Healed!"), false);
                                    }
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("speed")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    if (player != null) {
                                        player.addStatusEffect(new StatusEffectInstance(
                                                StatusEffects.SPEED, 1200, 2));
                                        context.getSource().sendFeedback(() -> Text.literal("God Mode: Speed Activated!"), false);
                                    }
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("give")
                                .then(CommandManager.argument("item",
                                                ItemStackArgumentType.itemStack(registryAccess))
                                        .then(CommandManager.argument("amount",
                                                        IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                    if (player == null) return 0;

                                                    ItemStackArgument itemArg =
                                                            ItemStackArgumentType.getItemStackArgument(ctx, "item");
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    ItemStack stack = itemArg.createStack(amount, false);

                                                    player.giveItemStack(stack);
                                                    ctx.getSource().sendFeedback(() -> Text.literal("Given " + stack.getName().getString()), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
        );
    }

    // ============================================================
    // STATS DISPLAY
    // ============================================================
    private static int executeStats(ServerCommandSource source, ServerPlayerEntity target) {
        UUID targetUuid = target.getUuid();
        VoidWalk.PlayerStats stats = VoidWalk.getPlayerStats(targetUuid);

        int kills = stats.kills;
        int deaths = stats.deaths;
        double kdr = deaths > 0 ? Math.round((double) kills / deaths * 100.0) / 100.0 : kills;
        int bounty = BountyManager.getActiveBounties().getOrDefault(targetUuid, 0);

        source.sendFeedback(() -> Text.literal("§6§l═══════════ STATS: " + target.getName().getString() + " ═══════════"), false);
        source.sendFeedback(() -> Text.literal("§7Kills: §c" + kills), false);
        source.sendFeedback(() -> Text.literal("§7Deaths: §e" + deaths), false);
        source.sendFeedback(() -> Text.literal("§7K/D Ratio: §b" + kdr), false);
        if (bounty > 0) {
            source.sendFeedback(() -> Text.literal("§7Active Bounty: §b" + bounty + " Diamonds"), false);
        }
        source.sendFeedback(() -> Text.literal("§6§l═══════════════════════════════════════"), false);

        return 1;
    }

    // ============================================================
    // TOP KILLERS LEADERBOARD
    // ============================================================
    private static int executeTopKillers(ServerCommandSource source) {
        Map<UUID, Integer> killCounts = VoidWalk.KILL_COUNTS;

        if (killCounts.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§cNo kill data available yet!"), false);
            return 0;
        }

        List<Map.Entry<UUID, Integer>> topKillers = killCounts.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        source.sendFeedback(() -> Text.literal("§6§l═══════════ TOP KILLERS ═══════════"), false);

        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : topKillers) {
            String playerName = getPlayerName(source, entry.getKey());
            int kills = entry.getValue();
            int currentRank = rank;

            String medal = currentRank == 1 ? "§6" : currentRank == 2 ? "§7" : currentRank == 3 ? "§c" : "§7";
            String prefix = currentRank == 1 ? "☠ " : currentRank == 2 ? "☠ " : currentRank == 3 ? "☠ " : "  ";
            source.sendFeedback(() -> Text.literal(
                    medal + prefix + currentRank + ". §c" + playerName + " §7- §b" + kills + " kills"
            ), false);
            rank++;
        }

        source.sendFeedback(() -> Text.literal("§6§l═══════════════════════════════════════"), false);
        return 1;
    }

    private static String getPlayerName(ServerCommandSource source, UUID uuid) {
        ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(uuid);
        if (player != null) {
            return player.getName().getString();
        }
        return BountyManager.getPlayerName(source.getServer(), uuid);
    }
}
