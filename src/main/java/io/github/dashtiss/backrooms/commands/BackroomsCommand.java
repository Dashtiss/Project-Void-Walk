package io.github.dashtiss.backrooms.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.github.dashtiss.backrooms.Backrooms;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackroomsCommand {
    private static final Map<UUID, ItemStack[]> SAVED_ARMOR = new HashMap<>();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("backrooms")
                // Existing vanish command sub-branch
                .then(CommandManager.literal("vanish")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity player = source.getPlayer();
                            if (player == null) return 0;

                            if (!Backrooms.isClientModded(player.getUuid())) {
                                source.sendError(Text.literal("You do not have the required client-side Backrooms Mod to execute this function!"));
                                return 0;
                            }

                            UUID uuid = player.getUuid();
                            if (SAVED_ARMOR.containsKey(uuid)) {
                                player.removeStatusEffect(StatusEffects.INVISIBILITY);
                                ItemStack[] armor = SAVED_ARMOR.remove(uuid);
                                player.equipStack(EquipmentSlot.HEAD, armor[0]);
                                player.equipStack(EquipmentSlot.CHEST, armor[1]);
                                player.equipStack(EquipmentSlot.LEGS, armor[2]);
                                player.equipStack(EquipmentSlot.FEET, armor[3]);
                                source.sendFeedback(() -> Text.literal("You have reappeared."), false);
                            } else {
                                ItemStack[] armor = new ItemStack[]{
                                        player.getEquippedStack(EquipmentSlot.HEAD).copy(),
                                        player.getEquippedStack(EquipmentSlot.CHEST).copy(),
                                        player.getEquippedStack(EquipmentSlot.LEGS).copy(),
                                        player.getEquippedStack(EquipmentSlot.FEET).copy()
                                };
                                SAVED_ARMOR.put(uuid, armor);

                                player.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
                                player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
                                player.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY);
                                player.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);

                                StatusEffectInstance vanishEffect = new StatusEffectInstance(
                                        StatusEffects.INVISIBILITY,
                                        StatusEffectInstance.INFINITE,
                                        0,
                                        false,
                                        false
                                );
                                player.addStatusEffect(vanishEffect);
                                source.sendFeedback(() -> Text.literal("You have slipped into the shadows..."), false);
                            }
                            return 1;
                        })
                )
                // NEW LIST COMMAND SUB-BRANCH
                .then(CommandManager.literal("list")
                        .requires(source -> source.hasPermissionLevel(4)) // Optional: Requires OP level 2 (cheat access)
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            MinecraftServer server = source.getServer();

                            if (Backrooms.AUTHENTICATED_CLIENTS.isEmpty()) {
                                source.sendFeedback(() -> Text.literal("§cNo players currently connected have the Backrooms client mod."), false);
                                return 1;
                            }

                            source.sendFeedback(() -> Text.literal("§a=== Verified Backrooms Mod Users ==="), false);

                            // Loop through the UUID set and convert them to active player entities to get their names
                            for (UUID uuid : Backrooms.AUTHENTICATED_CLIENTS) {
                                ServerPlayerEntity verifiedPlayer = server.getPlayerManager().getPlayer(uuid);
                                if (verifiedPlayer != null) {
                                    String name = verifiedPlayer.getName().getString();
                                    source.sendFeedback(() -> Text.literal("§7- §e" + name + " §8(" + uuid + ")"), false);
                                } else {
                                    // If they are in the database but somehow glitched outside player visibility tracking
                                    source.sendFeedback(() -> Text.literal("§7- §oOffline/Loading User §8(" + uuid + ")"), false);
                                }
                            }
                            return 1;
                        })
                )
        );
    }
}