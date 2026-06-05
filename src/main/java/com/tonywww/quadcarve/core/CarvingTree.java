package com.tonywww.quadcarve.core;

public class CarvingTree {
    public enum Operation { SPLIT, REMOVE }

    private CarvingTree[] children;
    private int paletteIndex = 0;
    private boolean subdivided = false;

    public CarvingTree() {}

    public boolean split(String path) { return applyAtPath(path, Operation.SPLIT); }
    public boolean remove(String path) { return applyAtPath(path, Operation.REMOVE); }

    private boolean applyAtPath(String path, Operation op) {
        if (path.isEmpty()) {
            if (op == Operation.SPLIT) {
                if (subdivided) return false;
                subdivided = true;
                children = new CarvingTree[4];
                for (int i = 0; i < 4; i++) { children[i] = new CarvingTree(); children[i].paletteIndex = paletteIndex; }
                paletteIndex = 1;
                return true;
            } else {
                if (!subdivided) return false;
                subdivided = false; children = null; paletteIndex = 0;
                return true;
            }
        }
        if (!subdivided || children == null) {
            if (op == Operation.REMOVE) return false;
            subdivided = true; children = new CarvingTree[4];
            for (int i = 0; i < 4; i++) { children[i] = new CarvingTree(); children[i].paletteIndex = paletteIndex; }
            paletteIndex = 1;
        }
        int dir = path.charAt(0) - '0';
        if (dir < 0 || dir > 3) return false;
        return children[dir].applyAtPath(path.substring(1), op);
    }

    public boolean setMaterial(String path, int paletteIdx) {
        if (path.isEmpty()) { if (subdivided) return false; paletteIndex = paletteIdx; return true; }
        if (!subdivided || children == null) return false;
        int dir = path.charAt(0) - '0';
        if (dir < 0 || dir > 3) return false;
        return children[dir].setMaterial(path.substring(1), paletteIdx);
    }

    public int getMaterial(String path) {
        if (path.isEmpty()) return paletteIndex;
        if (!subdivided || children == null) return paletteIndex;
        int dir = path.charAt(0) - '0';
        if (dir < 0 || dir > 3) return paletteIndex;
        return children[dir].getMaterial(path.substring(1));
    }

    public int depth() {
        if (!subdivided || children == null) return 1;
        int max = 0;
        for (CarvingTree c : children) max = Math.max(max, c.depth());
        return max + 1;
    }

    /** Total nodes (leaves + internal SPLIT nodes) – correct serialization buffer size. */
    public int nodeCount() {
        if (!subdivided || children == null) return 1;
        int sum = 1;
        for (CarvingTree c : children) sum += c.nodeCount();
        return sum;
    }

    /** Remap palette indices in-place after Palette.sortAndGetRemap(). */
    public void remapPaletteIndices(int[] remap) {
        if (paletteIndex < remap.length) paletteIndex = remap[paletteIndex];
        if (subdivided && children != null)
            for (CarvingTree c : children) c.remapPaletteIndices(remap);
    }

    public int leafCount() {
        if (!subdivided || children == null) return 1;
        int sum = 0;
        for (CarvingTree c : children) sum += c.leafCount();
        return sum;
    }

    public int filledLeafCount() {
        if (!subdivided || children == null) return paletteIndex > 1 ? 1 : 0;
        int sum = 0;
        for (CarvingTree c : children) sum += c.filledLeafCount();
        return sum;
    }

    public int preorderSerialize(byte[] data, int index) {
        data[index++] = (byte)(paletteIndex & 0xFF);
        if (subdivided && children != null) {
            for (CarvingTree child : children) index = child.preorderSerialize(data, index);
        }
        return index;
    }

    public static CarvingTree preorderDeserialize(byte[] data, int[] idxHolder) {
        CarvingTree node = new CarvingTree();
        int idx = idxHolder[0];
        if (idx >= data.length) return node;
        node.paletteIndex = data[idx] & 0xFF;
        idxHolder[0] = idx + 1;
        if (node.paletteIndex == 1) {
            node.subdivided = true; node.children = new CarvingTree[4];
            for (int i = 0; i < 4; i++) node.children[i] = preorderDeserialize(data, idxHolder);
        }
        return node;
    }

    public void visitLeaves(String prefix, LeafVisitor visitor) {
        if (!subdivided || children == null) { visitor.visit(prefix, paletteIndex); return; }
        children[0].visitLeaves(prefix + "0", visitor);
        children[1].visitLeaves(prefix + "1", visitor);
        children[2].visitLeaves(prefix + "2", visitor);
        children[3].visitLeaves(prefix + "3", visitor);
    }

    @FunctionalInterface
    public interface LeafVisitor { void visit(String path, int paletteIndex); }

    public CarvingTree getNode(String path) {
        if (path.isEmpty()) return this;
        if (!subdivided || children == null) return null;
        int dir = path.charAt(0) - '0';
        if (dir < 0 || dir > 3) return null;
        return children[dir].getNode(path.substring(1));
    }

    public boolean isSubdivided() { return subdivided; }
    public int getPaletteIndex() { return paletteIndex; }
    public void setPaletteIndex(int idx) { this.paletteIndex = idx; }
    public CarvingTree[] getChildren() { return children; }
}
