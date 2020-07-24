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

package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.User;
import com.vip.saturn.job.console.mybatis.repository.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private AuthenticationServiceImpl authnService;

	@Test
	public void testAuthenticateSuccessfully() throws SaturnJobConsoleException {
		authnService.setHashMethod("plaintext");
		User user = createUser("jeff", "password");
		when(userRepository.select("jeff")).thenReturn(user);

		assertEquals(user, authnService.authenticate("jeff", "password"));
	}

	@Test
	public void testAuthenticationFailWhenUserIsNotFound() throws SaturnJobConsoleException {
		authnService.setHashMethod("plaintext");
		when(userRepository.select("john")).thenReturn(null);
		boolean hasException = false;
		try {
			assertNull(authnService.authenticate("john", "password"));
		} catch (SaturnJobConsoleException e) {
			hasException = true;
			assertEquals(SaturnJobConsoleException.ERROR_CODE_AUTHN_FAIL, e.getErrorCode());
		}

		assertTrue(hasException);

	}

	@Test
	public void testAuthenticationFailWhenPasswordInputIsEmpty() throws SaturnJobConsoleException {
		authnService.setHashMethod("plaintext");
		boolean hasException = false;
		try {
			authnService.authenticate("john", "");
		} catch (SaturnJobConsoleException e) {
			hasException = true;
			assertEquals(SaturnJobConsoleException.ERROR_CODE_AUTHN_FAIL, e.getErrorCode());
		}

		assertTrue(hasException);
	}

	private User createUser(String username, String password) {
		User user = new User();
		user.setUserName(username);
		user.setPassword(password);

		return user;
	}
}