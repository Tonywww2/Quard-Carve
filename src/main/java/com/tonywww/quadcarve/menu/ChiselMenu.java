package com.tonywww.quadcarve.menu;

import com.tonywww.quadcarve.item.CarvedItem;
import com.tonywww.quadcarve.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Container for the Chisel UI.
 *
 * Slot layout:
 *   0        – carved item slot (from chisel's ItemStackHandler capability)
 *   1 – 27   – player main inventory
 *   28 – 36  – player hotbar
 */
public class ChiselMenu extends AbstractContainerMenu {

    public static final int CARVED_SLOT  = 0;
    public static final int INV_START    = 1;
    public static final int INV_END      = 27;  // inclusive
    public static final int HOTBAR_START = 28;
    public static final int HOTBAR_END   = 36;  // inclusive

    @Nullable private final ItemStack chiselStack;

    // ── Server-side constructor (real handler from capability) ────────────────
    public ChiselMenu(int id, Inventory playerInv, IItemHandler chiselInv, ItemStack chiselStack) {
        super(ModMenus.CHISEL_MENU.get(), id);
        this.chiselStack = chiselStack;

        // Slot 0 – only accept CarvedItem
        addSlot(new SlotItemHandler(chiselInv, 0, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof CarvedItem;
            }
        });

        addPlayerInventory(playerInv);
    }

    // ── Client-side constructor (dummy handler; server sends slot updates) ────
    public ChiselMenu(int id, Inventory playerInv) {
        this(id, playerInv, new ItemStackHandler(1), null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    public Slot getCarvedItemSlot() { return slots.get(CARVED_SLOT); }

    // ── Quick-move (Shift+Click) ──────────────────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return copy;

        ItemStack stack = slot.getItem();
        copy = stack.copy();

        if (index == CARVED_SLOT) {
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END + 1, true)) return ItemStack.EMPTY;
        } else {
            if (stack.getItem() instanceof CarvedItem) {
                if (!moveItemStackTo(stack, CARVED_SLOT, CARVED_SLOT + 1, false)) return ItemStack.EMPTY;
            } else if (index < HOTBAR_START) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END + 1, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, INV_START, INV_END + 1, false)) return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    // ── Persist cap on close ──────────────────────────────────────────────────

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && chiselStack != null && !chiselStack.isEmpty()) {
            chiselStack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> {
                if (h instanceof ItemStackHandler ish)
                    chiselStack.getOrCreateTag().put("ForgeCaps", ish.serializeNBT());
            });
        }
    }
}
