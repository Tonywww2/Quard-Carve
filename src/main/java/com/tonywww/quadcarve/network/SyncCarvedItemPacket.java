package com.tonywww.quadcarve.network;

import com.tonywww.quadcarve.core.CarvingData;
import com.tonywww.quadcarve.core.CarvingSerializer;
import com.tonywww.quadcarve.core.Palette;
import com.tonywww.quadcarve.core.PaletteEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → Client: pushes a compact JSON representation of the current CarvingData
 * so the ApricityUI JS canvas can render without reading item NBT through the slot API.
 *
 * Payload is a UTF-8 JSON string consumed by window.__quadcarve.onSync(json).
 */
public class SyncCarvedItemPacket {

    private final String json;

    public SyncCarvedItemPacket(String json) { this.json = json; }

    public SyncCarvedItemPacket(FriendlyByteBuf buf) {
        this.json = buf.readUtf(1 << 18); // max ~256 KB
    }

    public void encode(FriendlyByteBuf buf) { buf.writeUtf(json); }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static SyncCarvedItemPacket fromData(CarvingData data) {
        return new SyncCarvedItemPacket(toJson(data));
    }

    private static String toJson(CarvingData data) {
        byte[] treeBytes = CarvingSerializer.treeToByteArray(data.getTree());
        Palette palette  = data.getPalette();

        StringBuilder sb = new StringBuilder("{\"palette\":[");
        for (int i = 0; i < palette.size(); i++) {
            if (i > 0) sb.append(',');
            PaletteEntry e = palette.get(i);
            sb.append("{\"t\":\"").append(e.type.name()).append('"');
            if (e.type == PaletteEntry.Type.CUSTOM) {
                sb.append(",\"id\":\"").append(jsonEscape(e.textureId)).append('"');
                sb.append(",\"c\":").append(e.color);
            }
            sb.append('}');
        }
        sb.append("],\"tree\":\"");
        // Base64-encode the byte array for safe JSON transport
        sb.append(java.util.Base64.getEncoder().encodeToString(treeBytes));
        sb.append("\",\"finished\":").append(data.isFinished()).append('}');
        return sb.toString();
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ── Handler (client side) ─────────────────────────────────────────────────

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(json))
        );
        ctx.get().setPacketHandled(true);
    }

    /** Inner class keeps client-only code off the server classpath. */
    private static class ClientHandler {
        static void handle(String json) {
            // Inject tree state into ApricityUI JS context via the web renderer bridge.
            // The ApricityUI mod exposes net.apricityui.bridge.AUIBridge.evalScript(String).
            // If that class is present, we call it; otherwise we rely on the slot sync path.
            try {
                Class<?> bridge = Class.forName("net.apricityui.bridge.AUIBridge");
                java.lang.reflect.Method eval = bridge.getMethod("evalScript", String.class);
                String script = "if(window.__quadcarve) window.__quadcarve.onSync(" + json + ");";
                eval.invoke(null, script);
            } catch (Exception ignored) {
                // ApricityUI bridge not present or UI not open – slot NBT sync is the fallback
            }
        }
    }
}
