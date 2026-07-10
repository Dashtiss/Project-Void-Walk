package io.github.dashtiss.voidwalk.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.github.dashtiss.voidwalk.VoidWalk;
import io.github.dashtiss.voidwalk.managers.BountyManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class StatsCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("stats")
                // ==========================================
                // 1. PATH: /stats [player]
                // ==========================================
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
        );

        // Register /topkillers command separately
        dispatcher.register(CommandManager.literal("topkillers")
                // ==========================================
                // 2. PATH: /topkillers
                // ==========================================
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    return executeTopKillers(source);
                })
        );

        // Alias /topkills
        dispatcher.register(CommandManager.literal("topkills")
                .requires(src -> src.hasPermissionLevel(0))
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    return executeTopKillers(source);
                })
        );
    }

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

    private static int executeTopKillers(ServerCommandSource source) {
        Map<UUID, Integer> killCounts = VoidWalk.KILL_COUNTS;
        
        if (killCounts.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§cNo kill data available yet!"), false);
            return 0;
        }

        // Sort by kills, descending, take top 10
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
        // Try to get online player first
        ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(uuid);
        if (player != null) {
            return player.getName().getString();
        }
        
        // Fallback to BountyManager's lookup
        return BountyManager.getPlayerName(source.getServer(), uuid);
    }
}