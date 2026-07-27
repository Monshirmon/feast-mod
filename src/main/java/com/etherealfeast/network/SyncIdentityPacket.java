package com.etherealfeast.network;

import com.etherealfeast.capability.PlayerIdentityData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncIdentityPacket(CompoundTag tag) implements CustomPacketPayload {

    public static final Type<SyncIdentityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("ethereal_feast", "sync_identity"));

    public static final StreamCodec<FriendlyByteBuf, SyncIdentityPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG,
                    SyncIdentityPacket::tag,
                    SyncIdentityPacket::new
            );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(SyncIdentityPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                PlayerIdentityData.IdentityData data = PlayerIdentityData.get(mc.player);
                data.deserializeNBT(packet.tag);
            }
        });
    }
}
