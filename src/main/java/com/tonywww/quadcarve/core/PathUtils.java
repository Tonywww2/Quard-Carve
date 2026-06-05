package com.tonywww.quadcarve.core;

public class PathUtils {

    public static String coordToPath(int x, int y, int gridSize, int maxDepth) {
        StringBuilder path = new StringBuilder();
        int size = gridSize, cx = x, cy = y;
        for (int d = 0; d < maxDepth; d++) {
            int half = size / 2;
            if (half <= 0) break;
            int q = cx < half ? (cy < half ? 0 : 2) : (cy < half ? 1 : 3);
            path.append(q);
            if (q == 1 || q == 3) cx -= half;
            if (q >= 2) cy -= half;
            size = half;
        }
        return path.toString();
    }

    public static int[] pathToRect(String path, int gridSize) {
        int size = gridSize, x = 0, y = 0;
        for (int i = 0; i < path.length(); i++) {
            int d = path.charAt(i) - '0', half = size / 2;
            if (d == 1 || d == 3) x += half;
            if (d == 2 || d == 3) y += half;
            size = half;
        }
        return new int[]{x, y, size, size};
    }

    public static boolean isValid(String path) {
        if (path == null) return false;
        for (int i = 0; i < path.length(); i++) { char c = path.charAt(i); if (c < '0' || c > '3') return false; }
        return true;
    }
}
