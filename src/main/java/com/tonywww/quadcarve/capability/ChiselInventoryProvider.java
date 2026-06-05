package com.tonywww.quadcarve.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 1-slot ItemStackHandler attached to the Chisel item via initCapabilities().
 * Forge stores/loads the serialized data automatically under the "ForgeCaps" key.
 */
public class ChiselInventoryProvider implements ICapabilitySerializable<CompoundTag> {

    private final ItemStackHandler handler;
    private final LazyOptional<IItemHandler> optional;

    public ChiselInventoryProvider(@Nullable CompoundTag nbt) {
        this.handler  = new ItemStackHandler(1);
        if (nbt != null) this.handler.deserializeNBT(nbt);
        this.optional = LazyOptional.of(() -> handler);
    }

    @Override
    public CompoundTag serializeNBT() { return handler.serializeNBT(); }

    @Override
    public void deserializeNBT(CompoundTag nbt) { handler.deserializeNBT(nbt); }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, optional);
    }

    public ItemStackHandler getHandler() { return handler; }

    public void invalidate() { optional.invalidate(); }
}
