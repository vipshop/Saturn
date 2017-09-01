package com.vip.saturn.it.job;

import java.util.HashMap;
import java.util.Map;

import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

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
