package com.tonywww.quadcarve.network;

import com.tonywww.quadcarve.core.CarvingData;
import com.tonywww.quadcarve.item.CarvedItem;
import com.tonywww.quadcarve.item.ChiselItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;

import java.util.function.Supplier;

/**
 * Client → Server: request a carve operation on the carved item held in the chisel.
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

            // ── Guard 3: chisel must contain a CarvedItem in slot 0 ──────────
            final ItemStack[] carvedRef = { ItemStack.EMPTY };
            chiselStack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                ItemStack slot0 = handler.getStackInSlot(0);
                if (!slot0.isEmpty() && slot0.getItem() instanceof CarvedItem)
                    carvedRef[0] = slot0;
            });
            ItemStack carvedStack = carvedRef[0];
            if (carvedStack.isEmpty()) return;

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

            // Write back to the carved item's NBT (in-place, same reference)
            data.writeToNBT(tag);

            // Push updated tree JSON to the client's JS canvas
            ModNetwork.CHANNEL.sendTo(
                    SyncCarvedItemPacket.fromData(data),
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
