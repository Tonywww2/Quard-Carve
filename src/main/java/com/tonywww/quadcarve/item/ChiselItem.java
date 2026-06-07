package com.tonywww.quadcarve.item;

import com.sighs.apricityui.ApricityUI;
import com.sighs.apricityui.instance.ApricitySavedData;
import com.tonywww.quadcarve.capability.ChiselInventoryProvider;
import com.tonywww.quadcarve.core.CarvingData;
import com.tonywww.quadcarve.network.ModNetwork;
import com.tonywww.quadcarve.network.SyncCarvedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkDirection;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chisel Item – the core tool for the QuadCarve mod.
 *
 * Uses ApricityUI menu bindings.  Carved-data sync is driven by a server-tick
 * poller ({@link #onPlayerTick(ServerPlayer)}) so that both the initial sync
 * (after the client-side HTML/JS has loaded) and subsequent slot-change
 * detections are handled reliably.
 */
public class ChiselItem extends Item {

    /** Resource path of the ApricityUI HTML template. */
    public static final String UI_TEMPLATE = "quadcarve/chisel.html";

    // ── Server-tick tracking ──────────────────────────────────────────────────
    // Each entry represents one open chisel UI.  The poller runs every player
    // tick until the UI is closed.

    private static final String SAVED_DATA_NAME = "quadcarve_carved";
    private static final String INVENTORY_KEY  = "saved_data";
    private static final int    INITIAL_SYNC_DELAY_TICKS = 4; // ~200 ms

    /** Per-player session: tick counter + last known ItemStack identity. */
    static final class Session {
        int ticks;
        /** {@code ItemStack.isEmpty()} identity — we use EMPTY vs non-EMPTY. */
        boolean hadItem;

        Session() { this.ticks = 0; this.hadItem = false; }
    }

    /** Active chisel-UI sessions keyed by player UUID.  Server-thread only. */
    static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    public ChiselItem(Properties props) { super(props); }

    // ── Capability ────────────────────────────────────────────────────────────

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ChiselInventoryProvider(nbt);
    }

    // ── Use → open UI ─────────────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack chisel = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            System.out.println("[QC:use] Opening UI for " + serverPlayer.getName().getString());

            ApricityUI.menu(serverPlayer, UI_TEMPLATE)
                    .bind(b -> b.saveddata(SAVED_DATA_NAME, 1).player());

            // Register a tick poller session — the initial sync will be sent
            // after a short delay so the client-side HTML/JS has time to load.
            SESSIONS.put(serverPlayer.getUUID(), new Session());
            System.out.println("[QC:use] Session registered, active sessions=" + SESSIONS.size());
        }

        return InteractionResultHolder.sidedSuccess(chisel, level.isClientSide());
    }

    // ── Tick poller (called from QuadCarveMod on PlayerTickEvent) ─────────────

    /**
     * Called every server tick for players who have an active chisel session.
     * <ul>
     *   <li>Waits {@link #INITIAL_SYNC_DELAY_TICKS} ticks then sends the first sync.</li>
     *   <li>Thereafter polls the saved_data slot; any change triggers a new sync.</li>
     *   <li>Automatically removes the session when the player closes the UI.</li>
     * </ul>
     */
    public static void onPlayerTick(ServerPlayer player) {
        UUID id = player.getUUID();
        Session s = SESSIONS.get(id);
        if (s == null) return;

        // Auto-remove if the player closed the ApricityUI screen.
        if (!(player.containerMenu instanceof com.sighs.apricityui.instance.ApricityContainerMenu)) {
            System.out.println("[QC:tick] Player closed UI, removing session " + id);
            SESSIONS.remove(id);
            return;
        }

        s.ticks++;

        // 1) Delayed initial sync — avoids the client-side JS-not-ready race.
        if (s.ticks == INITIAL_SYNC_DELAY_TICKS) {
            System.out.println("[QC:tick] INITIAL SYNC at tick=" + s.ticks + " for " + id);
            syncCarvedDataToClient(player);
            s.hadItem = hasCarvedItem(player);
            System.out.println("[QC:tick] Initial sync done, hadItem=" + s.hadItem);
            return;
        }

        // 2) After the initial sync, poll for slot changes.
        if (s.ticks > INITIAL_SYNC_DELAY_TICKS) {
            boolean now = hasCarvedItem(player);
            if (now != s.hadItem) {
                System.out.println("[QC:tick] SLOT CHANGE: " + s.hadItem + " -> " + now + " at tick=" + s.ticks);
                s.hadItem = now;
                syncCarvedDataToClient(player);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean hasCarvedItem(ServerPlayer player) {
        if (player.getServer() == null) return false;
        ApricitySavedData sd = ApricitySavedData.get(player.getServer(), SAVED_DATA_NAME);
        ItemStackHandler h = sd.getOrCreate(INVENTORY_KEY, 1);
        return !h.getStackInSlot(0).isEmpty();
    }

    static void syncCarvedDataToClient(ServerPlayer player) {
        if (player.getServer() == null) { System.out.println("[QC:sync] server null, abort"); return; }
        ApricitySavedData sd = ApricitySavedData.get(player.getServer(), SAVED_DATA_NAME);
        ItemStackHandler h = sd.getOrCreate(INVENTORY_KEY, 1);
        ItemStack stack = h.getStackInSlot(0);

        System.out.println("[QC:sync] slot0 empty=" + stack.isEmpty()
                + " item=" + (stack.isEmpty() ? "none" : stack.getItem())
                + " isCarved=" + (stack.getItem() instanceof CarvedItem));

        CarvingData data = (stack.isEmpty() || !(stack.getItem() instanceof CarvedItem))
                ? new CarvingData()
                : CarvingData.readFromNBT(stack.getOrCreateTag());

        System.out.println("[QC:sync] data: finished=" + data.isFinished()
                + " paletteSize=" + data.getPalette().size()
                + " treeDepth=" + data.getTree().depth()
                + " treeLeafCount=" + data.getTree().leafCount());

        SyncCarvedItemPacket pkt = SyncCarvedItemPacket.fromData(data, stack);
        System.out.println("[QC:sync] packet created, sending to " + player.getName().getString());

        ModNetwork.CHANNEL.sendTo(
                pkt,
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
        );
        System.out.println("[QC:sync] packet sent to " + player.getName().getString());
    }
}
