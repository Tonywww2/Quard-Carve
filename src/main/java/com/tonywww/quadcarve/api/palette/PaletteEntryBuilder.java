package com.tonywww.quadcarve.api.palette;

import com.tonywww.quadcarve.core.PaletteEntry;

/**
 * Fluent builder for creating and registering a custom {@link PaletteEntry}.
 *
 * Obtain via {@link com.tonywww.quadcarve.api.QuadCarveAPI#newMaterial(String)}
 * or directly via {@link PaletteRegistry#builder(String)}.
 *
 * <pre>
 *   // Minimal – opaque solid colour
 *   QuadCarveAPI.newMaterial("mymod:gold")
 *       .color(255, 215, 0)
 *       .register();
 *
 *   // With explicit alpha
 *   QuadCarveAPI.newMaterial("mymod:glass")
 *       .colorARGB(0x80FFFFFF)
 *       .register();
 *
 *   // Build without registering (for ad-hoc use)
 *   PaletteEntry e = QuadCarveAPI.newMaterial("mymod:temp")
 *       .color(128, 0, 128)
 *       .build();
 * </pre>
 */
public final class PaletteEntryBuilder {

    private final String id;
    private int argbColor = 0xFFAAAAAA; // default: mid-grey opaque

    PaletteEntryBuilder(String id) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("Material id must not be null or blank");
        this.id = id;
    }

    // ── Colour setters ────────────────────────────────────────────────────────

    /**
     * Set colour as a packed ARGB integer (e.g. {@code 0xFFRRGGBB}).
     */
    public PaletteEntryBuilder colorARGB(int argb) {
        this.argbColor = argb;
        return this;
    }

    /**
     * Set opaque RGB colour (alpha = 0xFF).
     */
    public PaletteEntryBuilder color(int r, int g, int b) {
        return colorARGB((0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
    }

    /**
     * Set colour from a CSS-style hex string: {@code "#RRGGBB"} or {@code "#AARRGGBB"}.
     */
    public PaletteEntryBuilder colorHex(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        return colorARGB((int) Long.parseLong(h, 16) | (h.length() <= 6 ? 0xFF000000 : 0));
    }

    // ── Terminal operations ───────────────────────────────────────────────────

    /**
     * Build and register the entry in {@link PaletteRegistry}.
     * Throws {@link IllegalStateException} if the id is already registered.
     */
    public void register() {
        PaletteRegistry.register(id, argbColor);
    }

    /**
     * Build a {@link PaletteEntry} without registering it globally.
     * Useful for unit-testing or one-off palette manipulation.
     */
    public PaletteEntry build() {
        return PaletteEntry.custom(argbColor, id);
    }

    public String getId()       { return id; }
    public int    getArgbColor(){ return argbColor; }
}
