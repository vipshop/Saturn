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

package com.vip.saturn.job.integrate.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Alarm info entity.
 * <p>
 * Created by Jeff Zhu on 10/05/2017.
 */
public class AlarmInfo {

	private String type;

	private String level;

	private String name;

	private String title;

	private String message;

	private Map<String, String> customFields = new HashMap<>();

	public AlarmInfo() {
	}

	public AlarmInfo(String type, String level, String name, String title, String message,
			Map<String, String> customFields) {
		this.type = type;
		this.level = level;
		this.name = name;
		this.title = title;
		this.message = message;
		this.customFields = customFields;
	}

	public void addCustomField(String key, String value) {
		customFields.put(key, value);
	}

	@Override
	public String toString() {
		return "AlarmInfo{" + "type='" + type + '\'' + ", level='" + level + '\'' + ", name='" + name + '\''
				+ ", title='" + title + '\'' + ", message='" + message + '\'' + ", addtionalInfo="
				+ customFields.toString() + '}';
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public Map<String, String> getCustomFields() {
		return customFields;
	}

	public void setCustomFields(Map<String, String> customFields) {
		this.customFields = customFields;
	}

}
