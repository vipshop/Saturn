package com.vip.saturn.job.console.utils;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * Context to hold the audit info.
 *
 * @author kfchu
 */
public class AuditInfoContext {

	private static ThreadLocal<Map<String, String>> auditInfoHolder = new ThreadLocal<Map<String, String>>() {
		@Override
		protected Map<String, String> initialValue() {
			return Maps.newLinkedHashMap();
		}
	};

	public AuditInfoContext() {
	}

	public static void reset() {
		auditInfoHolder.remove();
	}

	public static void put(String key, String value) {
		auditInfoHolder.get().put(key, value);
	}

	public static void putNamespace(String value) {
		auditInfoHolder.get().put("namespace", value);
	}

	public static void putJobName(String value) {
		auditInfoHolder.get().put("jobName", value);
	}

	public static void putJobNames(List<String> value) {
		auditInfoHolder.get().put("jobNames", value.toString());
	}

	public static void putExecutorName(String value) {
		auditInfoHolder.get().put("executorName", value);
	}

	public static String get(String key) {
		return auditInfoHolder.get().get(key);
	}

	public static Map<String, String> currentAuditInfo() {
		return auditInfoHolder.get();
	}
}
