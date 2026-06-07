package com.tonywww.quadcarve.network;

import com.sighs.apricityui.init.Window;
import com.tonywww.quadcarve.core.CarvingData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.function.Supplier;

/**
 * Server → Client: pushes a binary NBT snapshot of the current CarvingData
 * along with the carved item's registry ID for the virtual slot display.
 *
 * Dispatches a {@link SyncEvent} on the ApricityUI Window so the JS canvas
 * can re-render without polling slot NBT.
 */
public class SyncCarvedItemPacket {

    private final CarvingData data;
    private final String carvedItemId;

    private SyncCarvedItemPacket(CarvingData data, ItemStack carvedStack) {
        this.data = data;
        this.carvedItemId = carvedStack.isEmpty()
                ? ""
                : BuiltInRegistries.ITEM.getKey(carvedStack.getItem()).toString();
    }

    public SyncCarvedItemPacket(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        this.data = (tag != null) ? CarvingData.readFromNBT(tag) : new CarvingData();
        this.carvedItemId = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        CompoundTag tag = new CompoundTag();
        data.writeToNBT(tag);
        buf.writeNbt(tag);
        buf.writeUtf(carvedItemId != null ? carvedItemId : "");
    }

    public static SyncCarvedItemPacket fromData(CarvingData data, ItemStack carvedStack) {
        return new SyncCarvedItemPacket(data, carvedStack);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(data, carvedItemId))
        );
        ctx.get().setPacketHandled(true);
    }

    private static class ClientHandler {
        static void handle(CarvingData data, String carvedItemId) {
            System.out.println("[QC:net] ClientHandler.handle() called");
            System.out.println("[QC:net]   carvedItemId='" + carvedItemId + "'");
            System.out.println("[QC:net]   data.finished=" + data.isFinished());
            System.out.println("[QC:net]   data.palette.size=" + data.getPalette().size());
            System.out.println("[QC:net]   data.tree.depth=" + data.getTree().depth());
            System.out.println("[QC:net]   data.tree.leafCount=" + data.getTree().leafCount());
            System.out.println("[QC:net]   jsData=" + data.toJSObject());
            SyncEvent event = new SyncEvent(data, carvedItemId);

            // Probe: use the same reflection logic Window.resolveEventType() uses
            String resolvedType;
            try {
                java.lang.reflect.Field f = event.getClass().getField("type");
                resolvedType = String.valueOf(f.get(event));
            } catch (Exception ex) {
                resolvedType = "REFLECTION_FAILED: " + ex;
            }
            System.out.println("[QC:net]   resolveEventType(self) = '" + resolvedType + "'");

            // Probe: peek at Window.window.listeners (private, need reflection)
            try {
                java.lang.reflect.Field lf = Window.class.getDeclaredField("listeners");
                lf.setAccessible(true);
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) lf.get(Window.window);
                System.out.println("[QC:net]   Window.window.listeners.size = " + map.size());
                for (Object k : map.keySet()) {
                    java.util.List<?> v = (java.util.List<?>) map.get(k);
                    System.out.println("[QC:net]   listeners['" + k + "'] = " + (v != null ? v.size() : 0) + " entries");
                }
            } catch (Exception ex) {
                System.out.println("[QC:net]   cannot peek listeners: " + ex);
            }

            System.out.println("[QC:net]   dispatching event type='" + event.type + "'");
            try {
                boolean dispatched = Window.window.dispatchEvent(event);
                System.out.println("[QC:net]   dispatchEvent returned " + dispatched);
            } catch (Exception ex) {
                System.out.println("[QC:net]   dispatchEvent THREW: " + ex);
                ex.printStackTrace(System.out);
            }
        }
    }

    // ── Event ─────────────────────────────────────────────────────────────────

    /**
     * Dispatched on {@code Window.window} after each server sync.
     * JS listens with {@code window.addEventListener('quadcarve:sync', e => ...)}.
     *
     * {@code data} — raw {@link CarvingData} (Rhino interop; works in dev).
     * {@code jsData} — plain {@code Map<String,Object>} that ApricityUI's JS engine
     *                  auto-converts to a native JS object (no Java interop needed).
     * {@code carvedItemId} — registry name of the carved item (e.g. "quadcarve:carved_oak"),
     *                        for the virtual slot display. Empty string if none.
     *
     * {@code type} must be a public field so {@code Window.resolveEventType()} can find it.
     */
    public static final class SyncEvent {
        public final String type = "quadcarve:sync";
        public final CarvingData data;
        public final Object jsData;
        public final String carvedItemId;

        SyncEvent(CarvingData data, String carvedItemId) {
            this.data         = data;
            this.jsData       = data.toJSObject();
            this.carvedItemId = carvedItemId != null ? carvedItemId : "";
        }
    }
}