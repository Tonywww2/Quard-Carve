package com.tonywww.quadcarve.core;

import net.minecraft.nbt.ListTag;
import java.util.*;

public class Palette {
    private final List<PaletteEntry> entries = new ArrayList<>();

    public Palette() {
        entries.add(PaletteEntry.EMPTY);
        entries.add(PaletteEntry.SPLIT);
    }

    private Palette(boolean empty) {} // internal factory use

    public int registerCustom(int color, String textureId) {
        for (int i = 2; i < entries.size(); i++) {
            PaletteEntry e = entries.get(i);
            if (e.type == PaletteEntry.Type.CUSTOM && e.color == color && e.textureId.equals(textureId)) return i;
        }
        entries.add(PaletteEntry.custom(color, textureId));
        return entries.size() - 1;
    }

    public PaletteEntry get(int index) {
        if (index < 0 || index >= entries.size()) return PaletteEntry.EMPTY;
        return entries.get(index);
    }

    public int size() { return entries.size(); }

    /**
     * Sort custom entries (index ≥ 2) alphabetically by textureId.
     * Returns remap[oldIndex] = newIndex for use with CarvingTree.remapPaletteIndices().
     * Modifies this Palette in-place.
     */
    public int[] sortAndGetRemap() {
        int size = entries.size();
        int[] remap = new int[size];
        remap[0] = 0;
        if (size > 1) remap[1] = 1;
        if (size <= 2) return remap;

        List<PaletteEntry> customs = new ArrayList<>(entries.subList(2, size));
        List<PaletteEntry> sorted = new ArrayList<>(customs);
        sorted.sort(Comparator.comparing(a -> a.textureId));

        for (int oldI = 2; oldI < size; oldI++) {
            PaletteEntry entry = customs.get(oldI - 2);
            for (int j = 0; j < sorted.size(); j++) {
                if (sorted.get(j) == entry) { remap[oldI] = j + 2; break; }
            }
        }

        entries.subList(2, size).clear();
        entries.addAll(sorted);
        return remap;
    }

    /** @deprecated Use sortAndGetRemap() when you also need to remap the tree. */
    @Deprecated
    public void sortStable() { sortAndGetRemap(); }

    public List<String> exportStructureOnly() {
        List<String> list = new ArrayList<>();
        for (PaletteEntry e : entries)
            list.add(e.type == PaletteEntry.Type.CUSTOM ? e.textureId : e.type.name());
        return list;
    }

    public ListTag toListTag() {
        ListTag list = new ListTag();
        for (PaletteEntry e : entries) list.add(e.toNBT());
        return list;
    }

    public static Palette fromListTag(ListTag tag) {
        Palette p = new Palette(false);
        for (int i = 0; i < tag.size(); i++) p.entries.add(PaletteEntry.fromNBT(tag.getCompound(i)));
        return p;
    }
}
