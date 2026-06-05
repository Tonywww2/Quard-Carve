package com.tonywww.quadcarve.api.palette;

import com.tonywww.quadcarve.core.PaletteEntry;

import java.util.*;

/**
 * Global registry of named custom materials available for use in CarvingData palettes.
 *
 * Other mods register their materials here during mod setup (FMLCommonSetupEvent or earlier).
 * At carving time, the player selects a registered material; the mod looks up its PaletteEntry
 * by ID and calls {@link com.tonywww.quadcarve.core.Palette#registerCustom(int, String)}.
 *
 * Registration is open (no lock), so datapacks / KubeJS can also add entries at load time.
 *
 * Usage:
 * <pre>
 *   QuadCarveAPI.newMaterial("mymod:silver")
 *       .color(192, 192, 192)
 *       .register();
 * </pre>
 */
public final class PaletteRegistry {

    private PaletteRegistry() {}

    private static final Map<String, PaletteEntry> REGISTRY = new LinkedHashMap<>();

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Start building a new material entry.
     * Call {@link PaletteEntryBuilder#register()} to commit it.
     */
    public static PaletteEntryBuilder builder(String id) {
        return new PaletteEntryBuilder(id);
    }

    /**
     * Direct registration (used internally by PaletteEntryBuilder).
     *
     * @throws IllegalStateException if {@code id} is already taken
     */
    public static void register(String id, int argbColor) {
        Objects.requireNonNull(id, "material id must not be null");
        if (REGISTRY.containsKey(id))
            throw new IllegalStateException("[QuadCarve] Duplicate palette material id: " + id);
        REGISTRY.put(id, PaletteEntry.custom(argbColor, id));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public static Optional<PaletteEntry> get(String id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static boolean contains(String id) {
        return REGISTRY.containsKey(id);
    }

    /** All registered IDs in insertion order. */
    public static Set<String> ids() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }

    /** All registered entries in insertion order. */
    public static Collection<PaletteEntry> entries() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static int size() { return REGISTRY.size(); }
}
