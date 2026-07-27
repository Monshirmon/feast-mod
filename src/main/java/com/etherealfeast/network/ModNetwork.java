package com.etherealfeast.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {

    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                SyncIdentityPacket.TYPE,
                SyncIdentityPacket.STREAM_CODEC,
                SyncIdentityPacket::handleClient
        );
    }
}
