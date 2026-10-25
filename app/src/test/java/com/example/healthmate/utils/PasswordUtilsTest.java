package com.example.healthmate.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/*
 * Checks that passwords are hashed and checked correctly (login feature).
 */
public class PasswordUtilsTest {

    @Test
    public void rightPassword_matches() {
        String salt = PasswordUtils.newSalt();
        String hash = PasswordUtils.hash("secret123", salt);
        assertTrue(PasswordUtils.matches("secret123", salt, hash));
    }

    @Test
    public void wrongPassword_doesNotMatch() {
        String salt = PasswordUtils.newSalt();
        String hash = PasswordUtils.hash("secret123", salt);
        assertFalse(PasswordUtils.matches("Secret123", salt, hash));
        assertFalse(PasswordUtils.matches("", salt, hash));
    }

    @Test
    public void hash_isNotThePlainPassword() {
        String salt = PasswordUtils.newSalt();
        String hash = PasswordUtils.hash("secret123", salt);
        assertNotEquals("secret123", hash);
        assertEquals(64, hash.length()); // 256 bits as hex
    }

    @Test
    public void samePassword_differentSalt_givesDifferentHash() {
        String saltA = PasswordUtils.newSalt();
        String saltB = PasswordUtils.newSalt();
        assertNotEquals(saltA, saltB);
        assertNotEquals(PasswordUtils.hash("secret123", saltA), PasswordUtils.hash("secret123", saltB));
    }
}