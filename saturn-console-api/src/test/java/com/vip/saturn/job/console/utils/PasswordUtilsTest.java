package com.vip.saturn.job.console.utils;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.Assert.*;

public class PasswordUtilsTest {

	@Test
	public void testGenSaltedPassword() throws Exception {
		String password = PasswordUtils.genPassword("password", "salt".getBytes(), "PBKDF2WithHmacSHA1");
		assertEquals("a2c2646186828474b754591a547c18f132d88d744c152655a470161a1a052135$73616c74", password);
	}

	@Test
	public void testValidate() throws Exception {
		String passwordInDB = "a2c2646186828474b754591a547c18f132d88d744c152655a470161a1a052135$73616c74";

		assertTrue(PasswordUtils.validate("password", passwordInDB, "PBKDF2WithHmacSHA1"));
		assertFalse(PasswordUtils.validate("password1", passwordInDB, "PBKDF2WithHmacSHA1"));
		assertTrue(PasswordUtils.validate("password", "password", "plaintext"));
		assertFalse(PasswordUtils.validate("password1", "password", "plaintext"));
	}

	@Test
	public void testValidateWherePasswordInDBisMalfomred() throws Exception {
		int count = 0;
		assertFalse(PasswordUtils.validate("password", "password", "PBKDF2WithHmacSHA1"));
	}
}