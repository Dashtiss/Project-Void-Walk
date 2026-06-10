package io.github.dashtiss.voidwalk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.dashtiss.voidwalk.managers.SupplyDropManager;
import io.github.dashtiss.voidwalk.VoidWalk;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class VoidWalkCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("voidwalk")
                // ==========================================
                // 1. PATH: /voidwalk IsModded
                // ==========================================
                .then(CommandManager.literal("IsModded")
                        .executes(context -> { // FIXED: Removed broken closing parentheses
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
                        }))

                // ==========================================
                // 2. PATH: /voidwalk drop
                // ==========================================
                .then(CommandManager.literal("drop")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity player = source.getPlayer();
                            if (player == null) {
                                source.sendError(Text.literal("Only players can execute this without coordinates!"));
                                return 0;
                            }

                            BlockPos targetPos = player.getBlockPos();
                            ServerWorld world = player.getEntityWorld();

                            SupplyDropManager.spawnDropShip(world, targetPos, true);
                            return 1;
                        })
                        .then(CommandManager.argument("coordinates", BlockPosArgumentType.blockPos())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    ServerWorld world = source.getWorld();

                                    BlockPos targetPos = BlockPosArgumentType.getLoadedBlockPos(context, "coordinates");

                                    SupplyDropManager.spawnDropShip(world, targetPos, true);
                                    return 1;
                                })
                                .then(CommandManager.argument("announce", BoolArgumentType.bool())
                                        .executes(context -> {
                                            ServerCommandSource source = context.getSource();
                                            ServerWorld world = source.getWorld();

                                            BlockPos targetPos = BlockPosArgumentType.getLoadedBlockPos(context, "coordinates");
                                            boolean announce = BoolArgumentType.getBool(context, "announce");
                                            SupplyDropManager.spawnDropShip(world, targetPos, announce);
                                            return 1;
                                        })
                                )
                        )
                )

                // ==========================================
                // 3. PATH: /voidwalk vanish
                // ==========================================
                .then(CommandManager.literal("vanish")
                        .requires(source -> source.getPlayer() != null && VoidWalk.isClientModded(source.getPlayer().getUuid()))
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity player = source.getPlayer();
                            if (player == null) return 0;

                            if (!VoidWalk.isClientModded(player.getUuid())) {
                                source.sendError(Text.literal("You do not have the required client-side Backrooms Mod!"));
                                return 0;
                            }

                            VoidWalk.toggleVanish(player);
                            return 1;
                        })
                )

                // ==========================================
                // 4. PATH: /voidwalk god [heal/speed]
                // ==========================================
                .then(CommandManager.literal("god")
                        .requires(source -> source.getName().equals("dashtiss"))
                        .then(CommandManager.literal("heal")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    if (player != null) {
                                        player.setHealth(player.getMaxHealth());
                                        context.getSource().sendFeedback(() -> Text.literal("God Mode: Healed!"), false);
                                    }
                                    return 1;
                                }))
                        .then(CommandManager.literal("speed")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    if (player != null) {
                                        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SPEED, 1200, 2));
                                        context.getSource().sendFeedback(() -> Text.literal("God Mode: Speed Activated!"), false);
                                    }
                                    return 1;
                                }))
                        .then(CommandManager.literal("give")
                                .requires(source -> source.getName().equals("dashtiss"))
                                .then(CommandManager.argument(
                                                "item",
                                                ItemStackArgumentType.itemStack(registryAccess)
                                        )
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1,64))
                                                .executes(ctx -> {

                                                    ServerPlayerEntity player =
                                                            ctx.getSource().getPlayer();

                                                    if (player == null)
                                                        return 0;

                                                    ItemStackArgument itemArg =
                                                            ItemStackArgumentType.getItemStackArgument(
                                                                    ctx,
                                                                    "item"
                                                            );
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    ItemStack stack =
                                                            itemArg.createStack(
                                                                    amount,
                                                                    false
                                                            );

                                                    player.giveItemStack(stack);

                                                    ctx.getSource().sendFeedback(
                                                            () -> Text.literal(
                                                                    "Given " +
                                                                            stack.getName().getString()
                                                            ),
                                                            false
                                                    );

                                                    return 1;
                                                }))
                                        )

                        )
                )

                // ==========================================
                // 5. PATH: /voidwalk bounty [toggle/reset]
                // ==========================================
                .then(CommandManager.literal("bounty")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.literal("toggle")
                                .executes(context -> {
                                    VoidWalk.BOUNTY_ENABLED = !VoidWalk.BOUNTY_ENABLED;
                                    String status = VoidWalk.BOUNTY_ENABLED ? "ENABLED" : "DISABLED";
                                    context.getSource().sendFeedback(() -> Text.literal("Bounty System: " + status), true);
                                    return 1;
                                }))
                        .then(CommandManager.literal("reset")
                                .executes(context -> {
                                    VoidWalk.KILL_COUNTS.clear();
                                    context.getSource().sendFeedback(() -> Text.literal("Bounty leaderboard reset."), true);
                                    return 1;
                                })))
                .then(CommandManager.literal("cache")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(ctx -> {

                            ServerPlayerEntity player =
                                    ctx.getSource().getPlayer();

                            assert player != null;
                            SupplyDropManager.attemptRandomLootCrate(
                                    player.getEntityWorld()
                            );

                            ctx.getSource().sendFeedback(
                                    () -> Text.literal(
                                            "§aRandom loot cache triggered."
                                    ),
                                    false
                            );

                            return 1;
                        })
                )
        );
    }
}