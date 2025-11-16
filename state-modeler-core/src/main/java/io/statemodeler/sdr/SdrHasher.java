package io.statemodeler.sdr;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for computing stable cryptographic hashes.
 *
 * <p>Uses SHA-256 to produce deterministic hashes independent of:
 * <ul>
 *   <li>Input format (YAML vs JSON)</li>
 *   <li>Field ordering</li>
 *   <li>Whitespace and formatting</li>
 * </ul>
 *
 * <p>Thread-safe and stateless.
 */
public final class SdrHasher {

    private SdrHasher() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Computes SHA-256 hash of the input string.
     *
     * @param input the string to hash (non-null)
     * @return lowercase hexadecimal SHA-256 hash (64 characters)
     * @throws IllegalArgumentException if input is null
     * @throws IllegalStateException if SHA-256 algorithm is not available
     */
    public static String computeHash(String input) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Converts byte array to lowercase hexadecimal string.
     *
     * @param bytes the byte array
     * @return hexadecimal string (lowercase)
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
