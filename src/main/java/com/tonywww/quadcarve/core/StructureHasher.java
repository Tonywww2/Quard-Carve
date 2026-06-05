package com.tonywww.quadcarve.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Computes a deterministic structure fingerprint for a finalised CarvingData.
 *
 * Input: structural key list (texture IDs, no colours) + raw tree byte array.
 * Algorithm: MD5 (always available in Java SE). Result: 32-char hex string.
 *
 * Usage in KubeJS / recipe systems: O(1) string comparison against stored hash.
 */
public class StructureHasher {

    private StructureHasher() {}

    public static String computeHash(List<String> structuralKeys, byte[] treeData) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            for (String key : structuralKeys) {
                md.update(key.getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0x00); // null-byte separator
            }
            md.update((byte) 0xFF); // section separator
            md.update(treeData);
            byte[] hash = md.digest();
            StringBuilder sb = new StringBuilder(32);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e); // unreachable on any standard JVM
        }
    }
}
