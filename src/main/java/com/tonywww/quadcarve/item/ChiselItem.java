package com.tonywww.quadcarve.item;

import com.tonywww.quadcarve.capability.ChiselInventoryProvider;
import com.tonywww.quadcarve.menu.ChiselMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;

public class ChiselItem extends Item {

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
        if (!level.isClientSide()) {
            chisel.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                ItemStack finalChisel = chisel;
                NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("container.quadcarve.chisel");
                    }

                    @Override
                    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                        return new ChiselMenu(id, inv, handler, finalChisel);
                    }
                });
            });
        }
        return InteractionResultHolder.sidedSuccess(chisel, level.isClientSide());
    }
}
