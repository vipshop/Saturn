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

package com.vip.saturn.job.basic;

/**
 * @author chembo.huang
 */
public class SaturnConstant {

	public static final String LOG_FORMAT = "[{}] msg={}";

	public static final String LOG_FORMAT_FOR_STRING = "[%s] msg=%s";

	public static final String TIME_ZONE_ID_DEFAULT = "Asia/Shanghai";
	// 最大允许的job log节点长度
	public static final int MAX_JOB_LOG_DATA_LENGTH = 512000;

}
