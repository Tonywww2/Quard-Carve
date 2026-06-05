package com.tonywww.quadcarve.api.recipe;

import com.tonywww.quadcarve.core.CarvingData;
import com.tonywww.quadcarve.item.CarvedItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Read-only helpers for querying a finalised carved item.
 *
 * Designed for use in KubeJS recipe scripts, CraftTweaker, or any other
 * recipe system that needs O(1) matching without parsing the full quadtree.
 *
 * All methods are null-safe and return {@link Optional} / empty collections
 * rather than throwing on bad input, so script code never needs try/catch.
 *
 * <pre>
 * // KubeJS example
 * ServerEvents.recipes(event => {
 *   event.custom({
 *     type: "mymod:sculpted_alloy",
 *     ingredient: { item: "quadcarve:carved_item" },
 *     structureHash: "a3f8c2…"
 *   });
 * });
 *
 * // Java ingredient predicate
 * stack -> QuadCarveRecipeHelper.matchesStructure(stack, "a3f8c2…")
 * </pre>
 */
public final class QuadCarveRecipeHelper {

    private QuadCarveRecipeHelper() {}

    // ── Core match ────────────────────────────────────────────────────────────

    /**
     * O(1) check: is {@code stack} a finalised carved item whose structure hash
     * equals {@code structureHash}?
     *
     * @param structureHash the 32-char hex MD5 produced by {@link com.tonywww.quadcarve.core.StructureHasher}
     */
    public static boolean matchesStructure(ItemStack stack, String structureHash) {
        if (structureHash == null || structureHash.isBlank()) return false;
        return getStructureHash(stack).filter(structureHash::equals).isPresent();
    }

    /**
     * O(1) check: shape matches AND the palette contains exactly the given
     * material IDs (order-insensitive, must match count).
     */
    public static boolean matchesStructureAndMaterials(ItemStack stack,
                                                       String structureHash,
                                                       List<String> requiredMaterialIds) {
        if (!matchesStructure(stack, structureHash)) return false;
        List<String> actual = getMaterialKeys(stack);
        if (actual.size() != requiredMaterialIds.size()) return false;
        return actual.containsAll(requiredMaterialIds) && requiredMaterialIds.containsAll(actual);
    }

    // ── Field accessors ───────────────────────────────────────────────────────

    /**
     * Returns the 32-char hex structure hash if the item is finalised, else empty.
     */
    public static Optional<String> getStructureHash(ItemStack stack) {
        CompoundTag tag = getFinishedTag(stack);
        if (tag == null || !tag.contains(CarvingData.TAG_HASH)) return Optional.empty();
        String hash = tag.getString(CarvingData.TAG_HASH);
        return hash.isEmpty() ? Optional.empty() : Optional.of(hash);
    }

    /**
     * Returns the sorted list of texture IDs (no colours) stored at finalisation.
     * Indices 0 and 1 are always {@code "EMPTY"} and {@code "SPLIT"}.
     * Custom materials start at index 2.
     * Returns an empty list if the item is not a finalised carved item.
     */
    public static List<String> getMaterialKeys(ItemStack stack) {
        CompoundTag tag = getFinishedTag(stack);
        if (tag == null || !tag.contains(CarvingData.TAG_STRUCT_KEYS)) return Collections.emptyList();
        var listTag = tag.getList(CarvingData.TAG_STRUCT_KEYS, net.minecraft.nbt.Tag.TAG_STRING);
        List<String> keys = new java.util.ArrayList<>(listTag.size());
        for (int i = 0; i < listTag.size(); i++) keys.add(listTag.getString(i));
        return Collections.unmodifiableList(keys);
    }

    /** Returns {@code true} if the item is a finalised (定型) carved item. */
    public static boolean isFinished(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof CarvedItem)) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(CarvingData.TAG_FINISHED);
    }

    /**
     * Returns the depth of the quadtree (1 = no splits, each split adds 1 level).
     * Returns 0 if the item is not a carved item.
     */
    public static int getDepth(ItemStack stack) {
        CompoundTag tag = getAnyCarvedTag(stack);
        if (tag == null) return 0;
        return CarvingData.readFromNBT(tag).depth();
    }

    /**
     * Returns 0.0–1.0 fill ratio (filled leaves / total leaves).
     * Returns 0 if the item is not a carved item.
     */
    public static float getFillRatio(ItemStack stack) {
        CompoundTag tag = getAnyCarvedTag(stack);
        if (tag == null) return 0f;
        CarvingData data = CarvingData.readFromNBT(tag);
        int total = data.totalLeaves();
        return total == 0 ? 0f : (float) data.filledLeaves() / total;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /** Returns the item tag if the stack is a finished CarvedItem, else null. */
    private static CompoundTag getFinishedTag(ItemStack stack) {
        if (!isFinished(stack)) return null;
        return stack.getTag();
    }

    /** Returns the item tag if the stack is any CarvedItem (finished or not), else null. */
    private static CompoundTag getAnyCarvedTag(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof CarvedItem)) return null;
        return stack.getTag();
    }
}
