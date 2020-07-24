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

package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.User;
import com.vip.saturn.job.console.mybatis.entity.UserRole;

/**
 * @author hebelala
 */
public interface AuthorizationService {

	void refreshAuthCache() throws SaturnJobConsoleException;

	boolean isAuthorizationEnabled() throws SaturnJobConsoleException;

	User getUser(String userName) throws SaturnJobConsoleException;

	boolean hasUserRole(UserRole userRole) throws SaturnJobConsoleException;

	void assertIsPermitted(String permissionKey, String userName, String namespace) throws SaturnJobConsoleException;

	void assertIsSystemAdmin(String userName) throws SaturnJobConsoleException;

}
