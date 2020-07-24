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

package com.vip.saturn.job;

import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author xiaopeng.he
 *
 */
public final class SaturnSystemErrorGroup {

	public static final int SUCCESS = 200;

	// general fail
	public static final int FAIL = 500;

	public static final int TIMEOUT = 550;

	// alarm will be raised with this error code
	public static final int FAIL_NEED_RAISE_ALARM = 551;

	public static Set<Integer> getAllSystemErrorGroups(){
		Set<Integer> resultSet = new HashSet<>();
		resultSet.add(SUCCESS);
		resultSet.add(FAIL);
		resultSet.add(TIMEOUT);
		resultSet.add(FAIL_NEED_RAISE_ALARM);

		return resultSet;
	}

}
