package com.vip.saturn.job.alarm;

import java.util.HashMap;
import java.util.Map;

/**
 * Alarm info entity.
 * <p>
 * Created by Jeff Zhu on 10/05/2017.
 */
public class AlarmInfo {

	private String level;

	private String name;

	private String title;

	private String message;

	private Map<String, String> customFields = new HashMap<>();

	public AlarmInfo(String level, String name, String title, String message, Map<String, String> customFields) {
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
		return "AlarmInfo{" + "level='" + level + '\'' + ", name='" + name + '\'' + ", title='" + title + '\''
				+ ", message='" + message + '\'' + ", customFields=" + customFields + '}';
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

	public static class AlarmInfoBuilder {
		private String nestedLevel;

		private String nestedName;

		private String nestedTitle;

		private String nestedMessage;

		private Map<String, String> nestedCustomFields = new HashMap<>();

		public AlarmInfoBuilder() {

		}

		public AlarmInfoBuilder level(String level) {
			this.nestedLevel = level;
			return this;
		}

		public AlarmInfoBuilder name(String name) {
			this.nestedName = name;
			return this;
		}

		public AlarmInfoBuilder title(String title) {
			this.nestedTitle = title;
			return this;
		}

		public AlarmInfoBuilder message(String message) {
			this.nestedMessage = message;
			return this;
		}

		public AlarmInfoBuilder customField(String key, String value) {
			this.nestedCustomFields.put(key, value);
			return this;
		}

		public AlarmInfo build() {
			return new AlarmInfo(nestedLevel, nestedName, nestedTitle, nestedMessage, nestedCustomFields);
		}
	}
}
