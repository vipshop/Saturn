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

import java.util.HashMap;
import java.util.Map;

public class UpdateCronJob extends AbstractSaturnJavaJob {
	private static boolean saled = false;

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext context) {
		try {
			Map<String, String> customContext = context.getCustomContext();
			if (customContext == null) {
				customContext = new HashMap<String, String>();
			}
			if (saled) {
				customContext.put("charge", "true");
			}
			customContext.put("sale", "true");
			saled = true;

			updateJobCron(shardParam, "*/1 * * * * ?", customContext);
		} catch (Exception e) { // job maybe is not found; cron maybe is valid; customContext maybe is out of zk limit
								// memory.
			e.printStackTrace();
		}

		return new SaturnJobReturn();
	}
}
