package com.tonywww.quadcarve.api.finalizer;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * Registry for items that can finalise (定型) a carved item.
 *
 * By default, QuadCarve registers the vanilla slimeball.
 * Other mods can add their own items or tags in {@code FMLCommonSetupEvent}.
 *
 * Usage:
 * <pre>
 *   // Register a single item
 *   QuadCarveAPI.registerFinalizer(MyItems.BINDING_RESIN.get());
 *
 *   // Register a tag (evaluated lazily at right-click time)
 *   QuadCarveAPI.registerFinalizerTag(MyTags.Items.BINDERS);
 * </pre>
 *
 * The check order is: item identity first, then tags.
 */
public final class FinalizerRegistry {

    private FinalizerRegistry() {}

    private static final Set<Item>          ITEMS = new LinkedHashSet<>();
    private static final List<TagKey<Item>> TAGS  = new ArrayList<>();

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Register a specific item as a finaliser.
     * Safe to call multiple times with the same item (idempotent).
     */
    public static void registerItem(Item item) {
        Objects.requireNonNull(item, "finalizer item must not be null");
        ITEMS.add(item);
    }

    /**
     * Register an item tag as a finaliser.
     * All items matching the tag at right-click time will trigger finalisation.
     */
    public static void registerTag(TagKey<Item> tag) {
        Objects.requireNonNull(tag, "finalizer tag must not be null");
        if (!TAGS.contains(tag)) TAGS.add(tag);
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the given stack should trigger finalisation.
     * Called from {@link com.tonywww.quadcarve.item.CarvedItem#overrideOtherStackedOnMe}.
     */
    public static boolean isFinalizer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (ITEMS.contains(stack.getItem())) return true;
        for (TagKey<Item> tag : TAGS) {
            if (stack.is(tag)) return true;
        }
        return false;
    }

    // ── Introspection (for UIs / debug) ──────────────────────────────────────

    public static Set<Item>          registeredItems() { return Collections.unmodifiableSet(ITEMS); }
    public static List<TagKey<Item>> registeredTags()  { return Collections.unmodifiableList(TAGS); }
}
