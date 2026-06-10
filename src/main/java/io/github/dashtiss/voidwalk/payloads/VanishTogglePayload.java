package io.github.dashtiss.voidwalk.payloads;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VanishTogglePayload() implements CustomPayload {
    public static final CustomPayload.Id<VanishTogglePayload> ID =
            new CustomPayload.Id<>(Identifier.of("backrooms", "vanish_toggle"));

    public static final PacketCodec<RegistryByteBuf, VanishTogglePayload> CODEC =
            PacketCodec.unit(new VanishTogglePayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
