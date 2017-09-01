package com.vip.saturn.job.console.mybatis.entity;

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

}
