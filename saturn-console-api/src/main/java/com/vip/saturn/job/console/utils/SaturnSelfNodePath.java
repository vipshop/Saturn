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

/**
 * @author hebelala
 */
public class SaturnSelfNodePath {

	public static final String ROOT_NAME = "$SaturnSelf";

	public static final String ROOT = "/" + ROOT_NAME;

	public static final String SATURN_CONSOLE = ROOT + "/saturn-console";

	public static final String SATURN_CONSOLE_DASHBOARD = SATURN_CONSOLE + "/dashboard";

	public static final String SATURN_CONSOLE_DASHBOARD_LEADER = SATURN_CONSOLE_DASHBOARD + "/leader";

	public static final String SATURN_CONSOLE_DASHBOARD_LEADER_LATCH = SATURN_CONSOLE_DASHBOARD_LEADER + "/latch";

	public static final String SATURN_CONSOLE_DASHBOARD_LEADER_HOST = SATURN_CONSOLE_DASHBOARD_LEADER + "/host";

	public static final String SATURN_EXECUTOR = ROOT + "/saturn-executor";

	public static final String SATURN_EXECUTOR_CONFIG = SATURN_EXECUTOR + "/config";

}
