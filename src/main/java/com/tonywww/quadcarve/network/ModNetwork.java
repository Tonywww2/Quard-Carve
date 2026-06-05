package com.tonywww.quadcarve.network;

import com.tonywww.quadcarve.QuadCarveMod;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            QuadCarveMod.prefix("main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id = 0;

    public static void register() {
        CHANNEL.registerMessage(id++, CarveActionPacket.class,
                CarveActionPacket::encode, CarveActionPacket::new, CarveActionPacket::handle);

        CHANNEL.registerMessage(id++, SyncCarvedItemPacket.class,
                SyncCarvedItemPacket::encode, SyncCarvedItemPacket::new, SyncCarvedItemPacket::handle);
    }
}
