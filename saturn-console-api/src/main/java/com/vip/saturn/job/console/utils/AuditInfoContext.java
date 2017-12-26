package com.vip.saturn.job.console.utils;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * Context to hold the audit info.
 */
public class AuditInfoContext {
	private static ThreadLocal<String> namespaceHolder = new ThreadLocal<>();

	private static ThreadLocal<Map<String, String>> auditInfoHolder = new ThreadLocal<Map<String, String>>(){
		@Override
		protected Map<String, String> initialValue() {
			return Maps.newHashMap();
		}
	};

	public AuditInfoContext() {
	}

	public static void reset() {
		auditInfoHolder.remove();
		namespaceHolder.remove();
	}

	public static void setNamespace(String value) {
		namespaceHolder.set(value);
	}

	public static String getNamespace() {
		return namespaceHolder.get();
	}

	public static void put(String key, String value) {
		auditInfoHolder.get().put(key,value);
	}

	public static String get(String key) {
		return auditInfoHolder.get().get(key);
	}

	public static Map<String, String> currentAuditInfo(){
		return auditInfoHolder.get();
	}
}
