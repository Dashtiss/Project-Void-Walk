package io.github.dashtiss.voidwalk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.dashtiss.voidwalk.managers.BountyManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ApiServices;

import java.util.Map;
import java.util.UUID;

public class BountyCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("bounty")
                // ==========================================
                // 1. PATH: /bounty list
                // ==========================================
                .then(CommandManager.literal("list")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            Map<UUID, Integer> activeBounties = BountyManager.getBountiesMap();

                            if (activeBounties.isEmpty()) {
                                source.sendFeedback(() -> Text.literal("§cThere are currently no active hit contracts on the server."), false);
                                return 1;
                            }

                            source.sendFeedback(() -> Text.literal("§6§l=== ACTIVE PURGE CONTRACTS ==="), false);

// Loop through stored data structures and translate UUID values to text lines
                            for (Map.Entry<UUID, Integer> entry : activeBounties.entrySet()) {
                                UUID targetUUID = entry.getKey();
                                int rewardAmount = entry.getValue();

                                net.minecraft.server.MinecraftServer server = source.getServer();
                                String targetName = "Unknown Target";

                                if (server != null) {
                                    ApiServices apiServices = server.getApiServices();
                                    if (apiServices != null) {
                                        Object cacheObj = apiServices.nameToIdCache();
                                        if (cacheObj instanceof net.minecraft.util.UserCache cache) {
                                            java.util.Optional<PlayerConfigEntry> profileOpt = cache.getByUuid(targetUUID);
                                            if (profileOpt.isPresent()) {
                                                targetName = profileOpt.get().name();
                                            }
                                        }
                                    }
                                }

                                String finalTargetName = targetName;
                                source.sendFeedback(() -> Text.literal("§7• §c" + finalTargetName + " §7- Price: §b" + rewardAmount + " Diamonds"), false);
                            }

                            source.sendFeedback(() -> Text.literal("§6§l=============================="), false);

                            return 1;
                        }))

                // ==========================================
                // 2. PATH: /bounty create <player> <reward>
                // ==========================================
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .then(CommandManager.argument("reward", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerCommandSource source = context.getSource();
                                            ServerPlayerEntity sender = source.getPlayer();

                                            if (sender == null) {
                                                source.sendError(Text.literal("Only online players can issue diamond bounties!"));
                                                return 0;
                                            }

                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                                            int amount = IntegerArgumentType.getInteger(context, "reward");

                                            // Cheat Prevention: Can't hunt yourself
                                            if (sender.getUuid().equals(target.getUuid())) {
                                                sender.sendMessage(Text.literal("§cYou cannot set a bounty on yourself!"), false);
                                                return 0;
                                            }

                                            // Hand execution over to our safe logic processor block
                                            BountyManager.addBounty(sender, target, amount);
                                            return 1;
                                        }))))
                .then(CommandManager.literal("wanted")
                        .executes(ctx -> {

                            MinecraftServer server =
                                    ctx.getSource().getServer();

                            Map<UUID, Integer> bounties =
                                    BountyManager.getActiveBounties();

                            ctx.getSource().sendFeedback(
                                    () -> Text.literal(
                                            "§6§l===== MOST WANTED ====="
                                    ),
                                    false
                            );

                            bounties.entrySet()
                                    .stream()
                                    .sorted((a, b) ->
                                            Integer.compare(
                                                    b.getValue(),
                                                    a.getValue()
                                            )
                                    )
                                    .limit(10)
                                    .forEach(entry -> {

                                        String playerName =
                                                BountyManager.getPlayerName(
                                                        server,
                                                        entry.getKey()
                                                );

                                        ctx.getSource().sendFeedback(
                                                () -> Text.literal(
                                                        "§c" + playerName
                                                                + " §7- §b"
                                                                + entry.getValue()
                                                                + " Diamonds"
                                                ),
                                                false
                                        );
                                    });

                            return 1;
                        })
                )
        );
    }
}