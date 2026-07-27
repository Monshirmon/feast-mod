package com.etherealfeast.network;

import com.etherealfeast.event.FeastHudOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ExpGainPacket(int amount) implements CustomPacketPayload {

    public static final Type<ExpGainPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("ethereal_feast", "exp_gain"));

    public static final StreamCodec<FriendlyByteBuf, ExpGainPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ExpGainPacket::amount,
                    ExpGainPacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(ExpGainPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (packet.amount > 0) {
                FeastHudOverlay.addFloatingText("+" + packet.amount);
            }
        });
    }
}
