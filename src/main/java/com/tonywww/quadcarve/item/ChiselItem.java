package com.tonywww.quadcarve.item;

import com.sighs.apricityui.ApricityUI;
import com.tonywww.quadcarve.capability.ChiselInventoryProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;

public class ChiselItem extends Item {

    /** Resource path of the ApricityUI HTML template. */
    public static final String UI_TEMPLATE = "quadcarve:ui/chisel.html";

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
            // Open the HTML screen via ApricityUI. Binds the player's inventory so the
            // "player" container in the HTML maps to real slots. The chisel's single slot
            // (carved item) is managed through CarveActionPacket which reads from the held item.
            ApricityUI.menu(serverPlayer, UI_TEMPLATE)
                    .bind(b -> b.player());
        }
        return InteractionResultHolder.sidedSuccess(chisel, level.isClientSide());
    }
}
