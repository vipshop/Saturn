package com.vip.saturn.job.console.mybatis.entity;

import java.util.Objects;

/**
 * @author hebelala
 */
public class SystemConfig {

	private Long id;
	private String property;
	private String value;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SystemConfig that = (SystemConfig) o;
		return Objects.equals(id, that.id) &&
				Objects.equals(property, that.property) &&
				Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {

		return Objects.hash(id, property, value);
	}
}
