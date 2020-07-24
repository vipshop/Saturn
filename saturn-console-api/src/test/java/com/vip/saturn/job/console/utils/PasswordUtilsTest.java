/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

package com.vip.saturn.job.console.utils;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import org.junit.Test;

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

		PasswordUtils.validate("password", passwordInDB, "PBKDF2WithHmacSHA1");
		PasswordUtils.validate("password", "password", "plaintext");

		int count = 0;
		try {
			PasswordUtils.validate("password1", passwordInDB, "PBKDF2WithHmacSHA1");
		} catch (SaturnJobConsoleException e) {
			count++;
			assertEquals(SaturnJobConsoleException.ERROR_CODE_AUTHN_FAIL, e.getErrorCode());
		}
		try {
			PasswordUtils.validate("password1", "password", "plaintext");
		} catch (SaturnJobConsoleException e) {
			count++;
			assertEquals(SaturnJobConsoleException.ERROR_CODE_AUTHN_FAIL, e.getErrorCode());
		}

		assertEquals(2, count);
	}

	@Test
	public void testValidateWherePasswordInDBisMalfomred() throws Exception {
		int count = 0;
		try {
			PasswordUtils.validate("password", "password", "PBKDF2WithHmacSHA1");
		} catch (SaturnJobConsoleException e) {
			count++;
			assertEquals(SaturnJobConsoleException.ERROR_CODE_AUTHN_FAIL, e.getErrorCode());
		}

		assertEquals(1, count);
	}
}