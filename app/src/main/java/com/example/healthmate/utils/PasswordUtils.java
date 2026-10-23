package com.example.healthmate.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/*
 * Password hashing, so the real password is never saved in the database.
 *
 * - a random salt is made for every user
 * - PBKDF2 mixes the password and the salt many times (slow on purpose,
 *   so guessing passwords is hard)
 * - to log in, the typed password is hashed again with the same salt
 *   and the two hashes are compared
 *
 * Plain Java (no Android code), so it is tested in PasswordUtilsTest.
 */
public final class PasswordUtils {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA1"; // works on every Android version
    private static final int ITERATIONS = 10_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtils() {
    }

    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return toHex(salt);
    }

    public static String hash(String password, String saltHex) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), fromHex(saltHex), ITERATIONS, KEY_LENGTH_BITS);
        try {
            byte[] hash = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            return toHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing is not available", e);
        } finally {
            spec.clearPassword();
        }
    }

    // true if the typed password gives the same hash as the saved one
    public static boolean matches(String password, String saltHex, String expectedHashHex) {
        String actual = hash(password, saltHex);
        // compares every byte, so the time taken does not give hints
        return MessageDigest.isEqual(actual.getBytes(), expectedHashHex.getBytes());
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}