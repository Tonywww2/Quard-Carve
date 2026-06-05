package com.tonywww.quadcarve.network;

import com.tonywww.quadcarve.core.CarvingData;
import com.tonywww.quadcarve.item.CarvedItem;
import com.tonywww.quadcarve.menu.ChiselMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → Server: request a carve operation on the item in the chisel slot.
 *
 * action values:
 *   0 = SPLIT    – subdivide the leaf at path
 *   1 = REMOVE   – collapse the node at path back to empty
 *   2 = SET_MAT  – set leaf material to paletteIndex
 *
 * Security: path length, valid chars, UI open, item type, and finished state are all validated.
 */
public class CarveActionPacket {

    public static final byte SPLIT    = 0;
    public static final byte REMOVE   = 1;
    public static final byte SET_MAT  = 2;

    public static final int MAX_PATH_DEPTH = 32;

    private final String path;
    private final byte   action;
    private final int    paletteIndex; // only used for SET_MAT

    public CarveActionPacket(String path, byte action, int paletteIndex) {
        this.path         = path;
        this.action       = action;
        this.paletteIndex = paletteIndex;
    }

    // Convenience constructors
    public static CarveActionPacket split(String path)               { return new CarveActionPacket(path, SPLIT, 0); }
    public static CarveActionPacket remove(String path)              { return new CarveActionPacket(path, REMOVE, 0); }
    public static CarveActionPacket setMat(String path, int palIdx)  { return new CarveActionPacket(path, SET_MAT, palIdx); }

    // ── Codec ─────────────────────────────────────────────────────────────────

    public CarveActionPacket(FriendlyByteBuf buf) {
        this.path         = buf.readUtf(MAX_PATH_DEPTH + 1);
        this.action       = buf.readByte();
        this.paletteIndex = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(path);
        buf.writeByte(action);
        buf.writeVarInt(paletteIndex);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // ── Guard 1: path validation ──────────────────────────────────────
            if (!isValidPath(path)) return;

            // ── Guard 2: player must have chisel UI open ──────────────────────
            if (!(player.containerMenu instanceof ChiselMenu menu)) return;

            // ── Guard 3: carved item must be in slot 0 ────────────────────────
            ItemStack carvedStack = menu.getCarvedItemSlot().getItem();
            if (carvedStack.isEmpty() || !(carvedStack.getItem() instanceof CarvedItem)) return;

            CompoundTag tag = carvedStack.getOrCreateTag();
            CarvingData data = CarvingData.readFromNBT(tag);

            // ── Guard 4: item not finished ────────────────────────────────────
            if (data.isFinished()) return;

            // ── Guard 5: palette index in range for SET_MAT ───────────────────
            if (action == SET_MAT && (paletteIndex < 0 || paletteIndex >= data.getPalette().size())) return;

            // ── Apply ─────────────────────────────────────────────────────────
            boolean changed = switch (action) {
                case SPLIT   -> data.getTree().split(path);
                case REMOVE  -> data.getTree().remove(path);
                case SET_MAT -> data.getTree().setMaterial(path, paletteIndex);
                default      -> false;
            };

            if (!changed) return;

            // Write back
            data.writeToNBT(tag);
            menu.getCarvedItemSlot().setChanged();

            // Push updated slot to client (vanilla tick will also do this, but force immediate)
            menu.broadcastChanges();

            // Send dedicated sync packet with full tree JSON for the JS canvas
            ServerPlayer sp = player;
            ModNetwork.CHANNEL.sendTo(
                    SyncCarvedItemPacket.fromData(data),
                    sp.connection.connection,
                    net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
            );
        });
        ctx.get().setPacketHandled(true);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private static boolean isValidPath(String path) {
        if (path == null || path.length() > MAX_PATH_DEPTH) return false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c < '0' || c > '3') return false;
        }
        return true;
    }
}
