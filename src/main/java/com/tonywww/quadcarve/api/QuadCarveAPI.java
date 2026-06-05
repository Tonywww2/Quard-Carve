package com.tonywww.quadcarve.api;

import com.tonywww.quadcarve.api.finalizer.FinalizerRegistry;
import com.tonywww.quadcarve.api.palette.PaletteEntryBuilder;
import com.tonywww.quadcarve.api.palette.PaletteRegistry;
import com.tonywww.quadcarve.api.recipe.QuadCarveRecipeHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Public API facade for the QuadCarve mod.
 *
 * Other mods should only interact with QuadCarve through this class and the
 * sub-packages {@code api.palette}, {@code api.finalizer}, and {@code api.recipe}.
 * Internal packages ({@code core}, {@code item}, {@code menu}, …) are not API.
 *
 * <h3>Typical mod integration</h3>
 * <pre>
 * // In your mod's FMLCommonSetupEvent:
 *
 * // 1. Register a custom carving material
 * QuadCarveAPI.newMaterial("mymod:copper")
 *     .color(184, 115, 51)
 *     .register();
 *
 * // 2. Register a custom finaliser item
 * QuadCarveAPI.registerFinalizer(MyItems.BINDING_RESIN.get());
 *
 * // 3. Match in a recipe predicate (KubeJS / CraftTweaker / custom ingredient)
 * boolean ok = QuadCarveAPI.matchesStructure(stack, "a3f8c2d1…");
 * </pre>
 */
public final class QuadCarveAPI {

    public static final String MOD_ID = com.tonywww.quadcarve.QuadCarveMod.MOD_ID;

    private QuadCarveAPI() {}

    // ══ Palette ════════════════════════════════════════════════════════════════

    /**
     * Start building a new custom palette material with the given namespaced ID.
     * <p>
     * The ID should follow the {@code "namespace:path"} convention, e.g.
     * {@code "mymod:silver"}.  It is used as the texture identifier stored
     * in NBT and as the key in the {@link PaletteRegistry}.
     * <p>
     * Call {@link PaletteEntryBuilder#register()} to commit, or
     * {@link PaletteEntryBuilder#build()} for a one-off entry without registration.
     */
    public static PaletteEntryBuilder newMaterial(String id) {
        return PaletteRegistry.builder(id);
    }

    /**
     * Look up a registered palette material by ID.
     * Returns {@code Optional.empty()} if not registered.
     */
    public static Optional<com.tonywww.quadcarve.core.PaletteEntry> getMaterial(String id) {
        return PaletteRegistry.get(id);
    }

    // ══ Finalisers ═════════════════════════════════════════════════════════════

    /**
     * Register an item that, when right-clicked onto a carved item in inventory,
     * triggers finalisation (定型).
     * <p>
     * Call this during {@code FMLCommonSetupEvent} or earlier.
     * Safe to call multiple times with the same item (idempotent).
     */
    public static void registerFinalizer(Item item) {
        FinalizerRegistry.registerItem(item);
    }

    /**
     * Register an item tag as a finaliser trigger.
     * All items in the tag at right-click time will trigger finalisation.
     */
    public static void registerFinalizerTag(TagKey<Item> tag) {
        FinalizerRegistry.registerTag(tag);
    }

    // ══ Recipe / structure queries ═════════════════════════════════════════════

    /**
     * O(1) check: does {@code stack} have the given structure hash?
     * Only returns {@code true} for finalised carved items.
     *
     * @param structureHash 32-char hex string produced at finalisation
     */
    public static boolean matchesStructure(ItemStack stack, String structureHash) {
        return QuadCarveRecipeHelper.matchesStructure(stack, structureHash);
    }

    /**
     * O(1) check: shape AND material list both match.
     *
     * @param requiredMaterialIds sorted list of texture IDs (order-insensitive)
     */
    public static boolean matchesStructureAndMaterials(ItemStack stack,
                                                       String structureHash,
                                                       List<String> requiredMaterialIds) {
        return QuadCarveRecipeHelper.matchesStructureAndMaterials(stack, structureHash, requiredMaterialIds);
    }

    /**
     * Returns the 32-char hex structure hash of a finalised carved item.
     * Returns {@code Optional.empty()} if not finalised or not a carved item.
     */
    public static Optional<String> getStructureHash(ItemStack stack) {
        return QuadCarveRecipeHelper.getStructureHash(stack);
    }

    /**
     * Returns the sorted material-key list stored at finalisation.
     * Returns an empty list if not a finalised carved item.
     */
    public static List<String> getMaterialKeys(ItemStack stack) {
        return QuadCarveRecipeHelper.getMaterialKeys(stack);
    }

    /** Returns {@code true} if the item is a finalised carved item. */
    public static boolean isFinished(ItemStack stack) {
        return QuadCarveRecipeHelper.isFinished(stack);
    }

    /** Returns the quadtree depth (≥1). Returns 0 if not a carved item. */
    public static int getDepth(ItemStack stack) {
        return QuadCarveRecipeHelper.getDepth(stack);
    }

    /** Returns fill ratio 0.0–1.0. Returns 0 if not a carved item. */
    public static float getFillRatio(ItemStack stack) {
        return QuadCarveRecipeHelper.getFillRatio(stack);
    }
}
