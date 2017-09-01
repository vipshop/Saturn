/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.internal.statistics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统计处理数据数量的类.
 * 
 * 
 */
public final class ProcessCountStatistics {

	private static ConcurrentMap<String, AtomicInteger> successCountMap = new ConcurrentHashMap<>();

	private static ConcurrentMap<String, AtomicInteger> failureCountMap = new ConcurrentHashMap<>();

	private static ConcurrentMap<String, AtomicInteger> totalCountDeltaMap = new ConcurrentHashMap<>();

	private static ConcurrentMap<String, AtomicInteger> errorCountDeltaMap = new ConcurrentHashMap<>();

	private static String buildKey(String executorName, String jobName) {
		return executorName + "_" + jobName;
	}

	public static void initTotalCountDelta(final String executorName, final String jobName, final int processCount) {
		totalCountDeltaMap.put(buildKey(executorName, jobName), new AtomicInteger(processCount));
	}

	public static void initErrorCountDelta(final String executorName, final String jobName, final int errorCount) {
		errorCountDeltaMap.put(buildKey(executorName, jobName), new AtomicInteger(errorCount));
	}

	public static synchronized void increaseTotalCountDelta(final String executorName, final String jobName) {
		incrementProcessCount(buildKey(executorName, jobName), totalCountDeltaMap);
	}

	public static synchronized void increaseErrorCountDelta(final String executorName, final String jobName) {
		incrementProcessCount(buildKey(executorName, jobName), errorCountDeltaMap);
	}

	public static int getTotalCountDelta(final String executorName, final String jobName) {
		String key = buildKey(executorName, jobName);
		return null == totalCountDeltaMap.get(key) ? 0 : totalCountDeltaMap.get(key).get();
	}

	public static int getErrorCountDelta(final String executorName, final String jobName) {
		String key = buildKey(executorName, jobName);
		return null == errorCountDeltaMap.get(key) ? 0 : errorCountDeltaMap.get(key).get();
	}

	/**
	 * 增加本作业服务器处理数据正确的数量.
	 *
	 * @param executorName executor名
	 * @param jobName 作业名称
	 */
	public static synchronized void incrementProcessSuccessCount(final String executorName, final String jobName) {
		incrementProcessCount(buildKey(executorName, jobName), successCountMap);
	}

	/**
	 * 增加本作业服务器处理数据正确的数量.
	 *
	 * @param executorName executor名
	 * @param jobName 作业名称
	 * @param successCount 处理数据正确的数量
	 */
	public static synchronized void incrementProcessSuccessCount(final String executorName, final String jobName,
			final int successCount) {
		incrementProcessCount(buildKey(executorName, jobName), successCount, successCountMap);
	}

	/**
	 * 增加本作业服务器处理数据错误的数量.
	 *
	 * @param executorName executor名
	 * @param jobName 作业名称
	 */
	public static synchronized void incrementProcessFailureCount(final String executorName, final String jobName) {
		incrementProcessCount(buildKey(executorName, jobName), failureCountMap);
	}

	/**
	 * 增加本作业服务器处理数据错误的数量.
	 *
	 * @param executorName executor名
	 * @param jobName 作业名称
	 * @param failureCount 处理数据错误的数量
	 */
	public static void incrementProcessFailureCount(final String executorName, final String jobName,
			final int failureCount) {
		incrementProcessCount(buildKey(executorName, jobName), failureCount, failureCountMap);
	}

	private static void incrementProcessCount(final String key,
			final ConcurrentMap<String, AtomicInteger> processCountMap) {
		processCountMap.putIfAbsent(key, new AtomicInteger(0));
		processCountMap.get(key).incrementAndGet();
	}

	private static void incrementProcessCount(final String key, final int count,
			final ConcurrentMap<String, AtomicInteger> processCountMap) {
		processCountMap.putIfAbsent(key, new AtomicInteger(0));
		processCountMap.get(key).addAndGet(count);
	}

	/**
	 * 获取本作业服务器处理数据正确的数量.
	 *
	 * @param executorName executor名
	 * @param jobName 作业名称
	 * @return 本作业服务器处理数据正确的数量
	 */
	public static int getProcessSuccessCount(final String executorName, final String jobName) {
		String key = buildKey(executorName, jobName);
		return null == successCountMap.get(key) ? 0 : successCountMap.get(key).get();
	}

	/**
	 * 获取本作业服务器处理数据错误的数量.
	 *
	 * @param executorName executor名
	 * @param jobName 作业名称
	 * @return 本作业服务器处理数据错误的数量
	 */
	public static int getProcessFailureCount(final String executorName, final String jobName) {
		String key = buildKey(executorName, jobName);
		return null == failureCountMap.get(key) ? 0 : failureCountMap.get(key).get();
	}

	/**
	 * 重置success/failure统计信息. analyse的totalCount和errorCount不清零。
	 *
	 * @param executorName executor名
	 * @param jobName 作业名称
	 */
	public static void resetSuccessFailureCount(final String executorName, final String jobName) {
		String key = buildKey(executorName, jobName);
		if (successCountMap.containsKey(key)) {
			successCountMap.get(key).set(0);
		}
		if (failureCountMap.containsKey(key)) {
			failureCountMap.get(key).set(0);
		}
	}

	/**
	 * 重置analyse统计信息. servers底下的success/failure不清零。
	 * @param executorName
	 * @param jobName
	 */
	public static void resetAnalyseCount(final String executorName, final String jobName) {
		String key = buildKey(executorName, jobName);
		if (totalCountDeltaMap.containsKey(key)) {
			totalCountDeltaMap.get(key).set(0);
		}
		if (errorCountDeltaMap.containsKey(key)) {
			errorCountDeltaMap.get(key).set(0);
		}
	}
}
