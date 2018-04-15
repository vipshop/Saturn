package com.vip.saturn.job.console.utils;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.Assert.*;

public class PasswordUtilsTest {

	@Test
	public void testGenSaltedPassword() throws Exception {
		String password = PasswordUtils.genPassword("password", "salt".getBytes(), "PBKDF2WithHmacSHA1");
		assertEquals("osJkYYaChHS3VFkaVHwY8TLYjXRMFSZVpHAWGhoFITU=$c2FsdA==", password);
	}

	@Test
	public void testValidate() throws Exception {
		assertTrue(PasswordUtils.validate("password", "osJkYYaChHS3VFkaVHwY8TLYjXRMFSZVpHAWGhoFITU=$c2FsdA==", "PBKDF2WithHmacSHA1"));
		assertFalse(PasswordUtils.validate("password1", "osJkYYaChHS3VFkaVHwY8TLYjXRMFSZVpHAWGhoFITU=$c2FsdA==", "PBKDF2WithHmacSHA1"));
		assertTrue(PasswordUtils.validate("password", "password", "plaintext"));
		assertFalse(PasswordUtils.validate("password1", "password", "plaintext"));
	}

	@Test
	public void testValidateWherePasswordInDBisMalfomred() {
		int count = 0;
		try {
			PasswordUtils.validate("password", "password", "PBKDF2WithHmacSHA1");
		} catch (Exception e) {
			count++;
			assertEquals("Invalid password stored in DB", e.getMessage());
		}

		assertEquals(1, count);
	}
}