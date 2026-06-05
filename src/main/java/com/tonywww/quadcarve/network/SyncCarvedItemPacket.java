package com.tonywww.quadcarve.network;

import com.sighs.apricityui.init.Window;
import com.tonywww.quadcarve.core.CarvingData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → Client: pushes a binary NBT snapshot of the current CarvingData.
 *
 * Dispatches a {@link SyncEvent} on the ApricityUI Window so the JS canvas
 * can re-render without polling slot NBT. JS accesses CarvingData via Rhino
 * Java interop — no JSON serialization needed.
 */
public class SyncCarvedItemPacket {

    private final CarvingData data;

    private SyncCarvedItemPacket(CarvingData data) { this.data = data; }

    public SyncCarvedItemPacket(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        this.data = (tag != null) ? CarvingData.readFromNBT(tag) : new CarvingData();
    }

    public void encode(FriendlyByteBuf buf) {
        CompoundTag tag = new CompoundTag();
        data.writeToNBT(tag);
        buf.writeNbt(tag);
    }

    public static SyncCarvedItemPacket fromData(CarvingData data) {
        return new SyncCarvedItemPacket(data);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(data))
        );
        ctx.get().setPacketHandled(true);
    }

    private static class ClientHandler {
        static void handle(CarvingData data) {
            Window.window.dispatchEvent(new SyncEvent(data));
        }
    }

    // ── Event ─────────────────────────────────────────────────────────────────

    /**
     * Dispatched on {@code Window.window} after each server sync.
     * JS listens with {@code window.addEventListener('quadcarve:sync', e => ...)}.
     * The {@code data} field is a live {@link CarvingData} accessible via Rhino Java interop.
     *
     * {@code type} must be a public field so {@code Window.resolveEventType()} can find it.
     */
    public static final class SyncEvent {
        public final String type = "quadcarve:sync";
        public final CarvingData data;

        SyncEvent(CarvingData data) { this.data = data; }
    }
}