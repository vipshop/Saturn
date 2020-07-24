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

package com.vip.saturn.it.job;

import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ReturnParamJavaJob extends AbstractSaturnJavaJob {
	public static Map<String, Integer> statusMap = new HashMap<String, Integer>();

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) {
		String key = jobName + "_" + shardItem;
		Integer status = statusMap.get(key);
		int count = 0;
		if (status != null) {
			count = status;
		}

		count++;
		statusMap.put(key, count);
		System.out
				.println(new Date() + " running:" + jobName + "; " + shardItem + ";" + shardParam + "; count:" + count);
		return new SaturnJobReturn(shardParam);
	}
}
