package com.tonywww.quadcarve.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class PaletteEntry {
    public static final PaletteEntry EMPTY = new PaletteEntry(Type.EMPTY, 0, 0, "empty");
    public static final PaletteEntry SPLIT = new PaletteEntry(Type.SPLIT, 0xFF888888, 0, "split");

    public enum Type { EMPTY, SPLIT, CUSTOM }

    public final Type type;
    public final int color;
    public final int extraData;
    public final String textureId;

    public PaletteEntry(Type type, int color, int extraData, String textureId) {
        this.type = type;
        this.color = color;
        this.extraData = extraData;
        this.textureId = textureId;
    }

    public static PaletteEntry custom(int color, String textureId) {
        return new PaletteEntry(Type.CUSTOM, color, 0, textureId);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putInt("color", color);
        tag.putInt("extra", extraData);
        if (type == Type.CUSTOM) tag.putString("texture", textureId);
        return tag;
    }

    public static PaletteEntry fromNBT(CompoundTag tag) {
        Type t = Type.valueOf(tag.getString("type"));
        if (t == Type.EMPTY) return EMPTY;
        if (t == Type.SPLIT) return SPLIT;
        return custom(tag.getInt("color"), tag.getString("texture"));
    }

    public Component toDisplay() {
        if (type == Type.EMPTY) return Component.literal("▢");
        if (type == Type.SPLIT) return Component.literal("▣");
        return Component.literal("■");
    }
}
