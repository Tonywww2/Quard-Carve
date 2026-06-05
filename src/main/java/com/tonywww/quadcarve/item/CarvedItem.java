package com.tonywww.quadcarve.item;

import com.tonywww.quadcarve.api.finalizer.FinalizerRegistry;
import com.tonywww.quadcarve.core.CarvingData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The carved item. All stages (in-progress and finished) share this single ID.
 *
 * NBT keys are managed by CarvingData. Tooltip shows depth / completion / materials.
 * Finalisation: right-click with any item registered in {@link FinalizerRegistry}
 * (default: slimeball; extensible via {@link com.tonywww.quadcarve.api.QuadCarveAPI}).
 */
public class CarvedItem extends Item {

    public CarvedItem(Properties props) { super(props); }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        CarvingData data = CarvingData.readFromNBT(tag);

        if (data.isFinished()) {
            tooltip.add(Component.translatable("tooltip.quadcarve.finished")
                    .withStyle(ChatFormatting.GREEN));
            if (tag.contains(CarvingData.TAG_HASH)) {
                String hash = tag.getString(CarvingData.TAG_HASH);
                tooltip.add(Component.literal("# " + hash.substring(0, 8) + "…")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.quadcarve.in_progress")
                    .withStyle(ChatFormatting.YELLOW));
        }

        int filled = data.filledLeaves();
        int total  = data.totalLeaves();
        float pct  = total > 0 ? (filled * 100f / total) : 0f;

        tooltip.add(Component.translatable("tooltip.quadcarve.depth", data.depth())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.quadcarve.fill",
                        String.format("%.1f", pct), filled, total)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.quadcarve.materials", data.materialCount())
                .withStyle(ChatFormatting.GRAY));
    }

    // ── Finalisation ──────────────────────────────────────────────────────────

    /**
     * Right-clicking this stack with any item registered in {@link FinalizerRegistry}
     * triggers finalisation: sorts the palette, remaps the tree, and stores an MD5
     * structure hash. After this, carving is permanently disabled.
     *
     * To add custom finaliser items, call
     * {@link com.tonywww.quadcarve.api.QuadCarveAPI#registerFinalizer} during mod setup.
     */
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other,
                                            SlotAccess slot, ClickAction action,
                                            Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY) return false;
        if (!FinalizerRegistry.isFinalizer(other)) return false;

        CompoundTag tag = stack.getOrCreateTag();
        CarvingData data = CarvingData.readFromNBT(tag);
        if (data.isFinished()) return false;

        if (!player.isCreative()) other.shrink(1);

        data.finalize(tag);
        return true;
    }

    @Override
    public boolean canBeDepleted() { return false; }
}
