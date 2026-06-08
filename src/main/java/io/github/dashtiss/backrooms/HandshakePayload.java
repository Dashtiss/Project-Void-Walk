package io.github.dashtiss.backrooms;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HandshakePayload() implements CustomPayload {
    public static final CustomPayload.Id<HandshakePayload> ID =
            new CustomPayload.Id<>(Identifier.of("backrooms", "handshake"));

    public static final PacketCodec<RegistryByteBuf, HandshakePayload> CODEC =
            PacketCodec.unit(new HandshakePayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}