package com.vip.saturn.job.basic;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Context of Saturn executor.
 */
public class SaturnExecutorContext {

	// 存放作业初始化异常信息，用于避免重复告警
	private static Map<String, Integer> jobInitExceptionMessageMap = Maps.newConcurrentMap();

	public static void putJobInitExceptionMessage(String jobName, String message) {
		if (containsJobInitExceptionMessage(jobName, message)) {
			return;
		}
		jobInitExceptionMessageMap.put(jobName, getHashCode(message));
	}

	public static boolean containsJobInitExceptionMessage(String jobName, String message) {
		if (!jobInitExceptionMessageMap.containsKey(jobName)) {
			return false;
		}

		return jobInitExceptionMessageMap.get(jobName).equals(getHashCode(message));
	}

	private static int getHashCode(String message) {
		if (message != null) {
			return message.hashCode();
		}

		return 0;
	}
}
