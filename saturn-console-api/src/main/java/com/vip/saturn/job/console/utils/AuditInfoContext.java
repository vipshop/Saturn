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
