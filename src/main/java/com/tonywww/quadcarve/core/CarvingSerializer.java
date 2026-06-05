package com.tonywww.quadcarve.core;

import net.minecraft.nbt.ByteArrayTag;

public class CarvingSerializer {

    /** Serialize tree to NBT. Buffer size uses nodeCount() (leaves + internal), not leafCount(). */
    public static ByteArrayTag serializeTree(CarvingTree tree) {
        return new ByteArrayTag(treeToByteArray(tree));
    }

    public static CarvingTree deserializeTree(ByteArrayTag tag) {
        return deserializeTree(tag.getAsByteArray());
    }

    public static CarvingTree deserializeTree(byte[] data) {
        if (data == null || data.length == 0) return new CarvingTree();
        int[] h = {0};
        return CarvingTree.preorderDeserialize(data, h);
    }

    public static byte[] treeToByteArray(CarvingTree tree) {
        int cnt = tree.nodeCount(); // FIX: was leafCount() – too small for internal nodes
        byte[] data = new byte[Math.max(cnt, 1)];
        tree.preorderSerialize(data, 0);
        return data;
    }
}
