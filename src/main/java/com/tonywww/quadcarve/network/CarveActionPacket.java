package com.tonywww.quadcarve.network;

import com.sighs.apricityui.instance.ApricitySavedData;
import com.tonywww.quadcarve.core.CarvingData;
import com.tonywww.quadcarve.item.CarvedItem;
import com.tonywww.quadcarve.item.ChiselItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;

import java.util.function.Supplier;

/**
 * Client → Server: request a carve operation on the CarvedItem
 * stored in the ApricityUI saved_data container.
 *
 * action values:
 *   0 = SPLIT    – subdivide the leaf at path
 *   1 = REMOVE   – collapse the node at path back to empty
 *   2 = SET_MAT  – set leaf material to paletteIndex
 *
 * Security: path chars, path depth, player holding chisel, carved item type,
 * palette index range, and finished state are all validated server-side.
 */
public class CarveActionPacket {

    public static final byte SPLIT   = 0;
    public static final byte REMOVE  = 1;
    public static final byte SET_MAT = 2;

    public static final int MAX_PATH_DEPTH = 32;

    private static final String SAVED_DATA_NAME = "quadcarve_carved";
    private static final String INVENTORY_KEY   = "saved_data"; // matches BindingBuilder's container id

    private final String path;
    private final byte   action;
    private final int    paletteIndex;

    public CarveActionPacket(String path, byte action, int paletteIndex) {
        this.path         = path;
        this.action       = action;
        this.paletteIndex = paletteIndex;
    }

    public static CarveActionPacket split(String path)              { return new CarveActionPacket(path, SPLIT, 0); }
    public static CarveActionPacket remove(String path)             { return new CarveActionPacket(path, REMOVE, 0); }
    public static CarveActionPacket setMat(String path, int palIdx) { return new CarveActionPacket(path, SET_MAT, palIdx); }

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

            // ── Guard 2: player must be holding a chisel ──────────────────────
            ItemStack chiselStack = findChisel(player);
            if (chiselStack.isEmpty()) return;

            // ── Guard 3: read CarvedItem from saved_data container ─────────
            if (player.getServer() == null) return;
            ApricitySavedData savedData = ApricitySavedData.get(
                    player.getServer(), SAVED_DATA_NAME);
            ItemStackHandler handler = savedData.getOrCreate(INVENTORY_KEY, 1);
            ItemStack carvedStack = handler.getStackInSlot(0);
            if (carvedStack.isEmpty() || !(carvedStack.getItem() instanceof CarvedItem))
                return;

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

            // Write back to the carved item's NBT, then persist in saved_data
            data.writeToNBT(tag);
            handler.setStackInSlot(0, carvedStack); // triggers setDirty() via onContentsChanged

            // Push updated tree JSON to the client's JS canvas
            ModNetwork.CHANNEL.sendTo(
                    SyncCarvedItemPacket.fromData(data, carvedStack),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT
            );
        });
        ctx.get().setPacketHandled(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the chisel ItemStack if the player is holding one, else EMPTY. */
    private static ItemStack findChisel(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof ChiselItem) return held;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isValidPath(String path) {
        if (path == null || path.length() > MAX_PATH_DEPTH) return false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c < '0' || c > '3') return false;
        }
        return true;
    }
}
