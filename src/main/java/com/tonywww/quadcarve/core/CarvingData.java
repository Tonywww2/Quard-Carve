package com.tonywww.quadcarve.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Composite of CarvingTree + Palette. The single source of truth stored in a CarvedItem's NBT.
 *
 * NBT layout inside the item tag:
 *   TreeData    : byte[]   (pre-order serialised quadtree)
 *   Palette     : list     (palette entries)
 *   finished    : boolean
 *   StructuralKeys : list<string>  (only after finalisation)
 *   StructureHash  : string        (only after finalisation)
 */
public class CarvingData {

    public static final String TAG_TREE     = "TreeData";
    public static final String TAG_PALETTE  = "Palette";
    public static final String TAG_FINISHED = "finished";
    public static final String TAG_STRUCT_KEYS = "StructuralKeys";
    public static final String TAG_HASH     = "StructureHash";

    private final CarvingTree tree;
    private final Palette palette;
    private boolean finished;

    public CarvingData() {
        this.tree     = new CarvingTree();
        this.palette  = new Palette();
        this.finished = false;
    }

    private CarvingData(CarvingTree tree, Palette palette, boolean finished) {
        this.tree     = tree;
        this.palette  = palette;
        this.finished = finished;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public CarvingTree getTree()    { return tree; }
    public Palette     getPalette() { return palette; }
    public boolean     isFinished() { return finished; }

    // ── NBT I/O ──────────────────────────────────────────────────────────────

    public void writeToNBT(CompoundTag tag) {
        tag.put(TAG_TREE,    CarvingSerializer.serializeTree(tree));
        tag.put(TAG_PALETTE, palette.toListTag());
        tag.putBoolean(TAG_FINISHED, finished);
    }

    public static CarvingData readFromNBT(CompoundTag tag) {
        CarvingTree tree;
        if (tag.contains(TAG_TREE, Tag.TAG_BYTE_ARRAY)) {
            tree = CarvingSerializer.deserializeTree(tag.getByteArray(TAG_TREE));
        } else {
            tree = new CarvingTree();
        }

        Palette palette;
        if (tag.contains(TAG_PALETTE, Tag.TAG_LIST)) {
            palette = Palette.fromListTag(tag.getList(TAG_PALETTE, Tag.TAG_COMPOUND));
        } else {
            palette = new Palette();
        }

        boolean finished = tag.getBoolean(TAG_FINISHED);
        return new CarvingData(tree, palette, finished);
    }

    // ── Phase 5: Finalization ────────────────────────────────────────────────

    /**
     * Finalise the carving:
     *  1. Sort palette alphabetically and remap tree indices.
     *  2. Reserialize tree.
     *  3. Export structural key list and compute structure hash.
     *  4. Write everything to {@code targetTag} and set finished = true.
     */
    public void finalize(CompoundTag targetTag) {
        if (finished) return;

        // 1. Sort palette and remap tree
        int[] remap = palette.sortAndGetRemap();
        tree.remapPaletteIndices(remap);

        // 2. Reserialize
        byte[] treeData = CarvingSerializer.treeToByteArray(tree);

        // 3. Structural keys (texture IDs only, no colours)
        List<String> structural = palette.exportStructureOnly();
        ListTag structTag = new ListTag();
        for (String s : structural) structTag.add(StringTag.valueOf(s));

        // 4. Hash
        String hash = StructureHasher.computeHash(structural, treeData);

        // 5. Write
        finished = true;
        writeToNBT(targetTag);
        targetTag.put(TAG_STRUCT_KEYS, structTag);
        targetTag.putString(TAG_HASH, hash);
    }

    // ── Stat helpers for tooltip ─────────────────────────────────────────────

    public int depth()         { return tree.depth(); }
    public int totalLeaves()   { return tree.leafCount(); }
    public int filledLeaves()  { return tree.filledLeafCount(); }
    public int materialCount() { return Math.max(0, palette.size() - 2); }

    // ── JS interop (ApricityUI) ──────────────────────────────────────────────

    /**
     * Exports the full CarvingData as a plain {@code Map<String, Object>} suitable
     * for ApricityUI's JS engine.  This replaces the Rhino Java-interop pattern
     * ({@code data.isFinished()}, {@code data.getTree()}, …) with pure JS objects.
     *
     * <pre>
     * { finished: bool,
     *   palette:  [ { type: "EMPTY"|"SPLIT"|"CUSTOM", color: int, id: string }, … ],
     *   tree:     { idx: int, children: null | [ tree, tree, tree, tree ] }
     * }
     * </pre>
     */
    public Map<String, Object> toJSObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("finished", finished);

        // Palette
        List<Map<String, Object>> pal = new ArrayList<>(palette.size());
        for (int i = 0; i < palette.size(); i++) {
            PaletteEntry e = palette.get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type",  e.type.name());
            entry.put("color", e.color);
            entry.put("id",    e.textureId);
            pal.add(entry);
        }
        map.put("palette", pal);

        // Tree (recursive)
        map.put("tree", treeToJSMap(tree));

        return map;
    }

    private static Map<String, Object> treeToJSMap(CarvingTree node) {
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("idx", node.getPaletteIndex());
        if (node.isSubdivided() && node.getChildren() != null) {
            List<Map<String, Object>> kids = new ArrayList<>(4);
            for (CarvingTree child : node.getChildren()) {
                kids.add(treeToJSMap(child));
            }
            obj.put("children", kids);
        }
        // NOTE: do NOT put a literal null into this map. Rhino's NativeJavaMap.get()
        // unwraps values via ValueUnwrapper, which calls value.getClass() without a
        // null-check and throws NullPointerException for null entries. Simply omitting
        // the "children" key makes jsData.tree.children resolve to `undefined` in JS,
        // which is falsy and works fine with the existing `!n.children` checks.
        return obj;
    }
}
