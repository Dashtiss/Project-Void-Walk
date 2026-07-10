package io.github.dashtiss.voidwalk.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.dashtiss.voidwalk.VoidWalk;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class VanishCommand {

    public static LiteralArgumentBuilder<ServerCommandSource> build() {
        return CommandManager.literal("vanish")
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
                });
    }
}
