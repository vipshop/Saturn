package com.vip.saturn.it.utils;

import com.vip.saturn.it.SaturnAutoBasic;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.internal.config.JobConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Canned {@link ExpectedCondition}s which are generally useful within saturn test. Created by gilbert.guo on 2016/9/26.
 */
public class ExpectedConditions extends SaturnAutoBasic {
	private static final Logger log = LoggerFactory.getLogger(ExpectedConditions.class);

	private ExpectedConditions() {
		// Utility class
	}

	public static ExpectedCondition executorHasSuccessCount(final JobConfiguration jobConfiguration,
			final Main executor) {
		return new ExpectedCondition() {
			@Override
			public boolean apply(Object input) {
				String successCount = getSuccessCountOfExecutor(jobConfiguration, executor);
				// is null when begin
				return successCount != null && Integer.parseInt(successCount) >= 1;
			}
		};
	}

	public static ExpectedCondition executorProcessCountChanged(final JobConfiguration jobConfiguration,
			final Main executor) {
		return new ExpectedCondition() {
			final String successCountBefore = getSuccessCountOfExecutor(jobConfiguration, executor);
			final String failureCountBefore = getFailureCountOfExecutor(jobConfiguration, executor);

			@Override
			public boolean apply(Object input) {
				String successCount = getSuccessCountOfExecutor(jobConfiguration, executor);
				String failureCount = getFailureCountOfExecutor(jobConfiguration, executor);
				if ((successCount == null) || (failureCount == null)) {
					return false;
				} else
					return !(successCount.equals(successCountBefore) && failureCount.equals(failureCountBefore));
			}
		};
	}

	/**
	 * all shards having completed znode and wait some time
	 */
	public static ExpectedCondition jobFinish(final String jobName, final int shardCount) {
		return new ExpectedCondition() {
			@Override
			public boolean apply(Object input) {
				return hasCompletedZnodeForAllShards(jobName, shardCount);
			}
		};
	}

}
