package io.github.dashtiss.voidwalk.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.dashtiss.voidwalk.VoidWalk;
import io.github.dashtiss.voidwalk.managers.BountyManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class DebugCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("debug")
                .requires(source -> source.hasPermissionLevel(4))
                .executes(context -> {
                    ServerCommandSource source = context.getSource();

                    source.sendFeedback(() -> Text.literal("§6§l=== VoidWalk Debug Info ==="), false);
                    source.sendFeedback(() -> Text.literal("§7Bounty System: §" + (VoidWalk.BOUNTY_ENABLED ? "aEnabled" : "cDisabled")), false);
                    source.sendFeedback(() -> Text.literal("§7Authenticated Clients: §b" + VoidWalk.AUTHENTICATED_CLIENTS.size()), false);
                    source.sendFeedback(() -> Text.literal("§7Vanished Players: §b" + VoidWalk.VANISHED_PLAYERS.size()), false);
                    source.sendFeedback(() -> Text.literal("§7Kill Records: §b" + VoidWalk.KILL_COUNTS.size()), false);
                    source.sendFeedback(() -> Text.literal("§7Death Records: §b" + VoidWalk.DEATH_COUNTS.size()), false);
                    source.sendFeedback(() -> Text.literal("§7Active Bounties: §b" + BountyManager.getActiveBounties().size()), false);
                    source.sendFeedback(() -> Text.literal("§6§l============================="), false);

                    VoidWalk.LOGGER.info("Debug info viewed by {}", source.getName());
                    return 1;
                })
                .then(CommandManager.literal("data")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> Text.literal("§cKill Counts:"), false);
                            VoidWalk.KILL_COUNTS.forEach((uuid, kills) -> {
                                String name = "Unknown";
                                ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(uuid);
                                if (player != null) name = player.getName().getString();
                                String finalName = name;
                                source.sendFeedback(() -> Text.literal(" §7- §c" + finalName + " §7- §b" + kills + " kills"), false);
                            });
                            return 1;
                        })
                )
                .then(CommandManager.literal("status")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            source.sendFeedback(() -> Text.literal("§7Server TPS: §b" + source.getServer().getTicks() + " ticks"), false);
                            source.sendFeedback(() -> Text.literal("§7Online Players: §b" + source.getServer().getPlayerManager().getPlayerList().size()), false);
                            source.sendFeedback(() -> Text.literal("§7Memory: §b" + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB"), false);
                            return 1;
                        })
                );
    }
}
