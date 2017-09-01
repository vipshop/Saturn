package com.vip.saturn.job.console.mybatis.entity;

import java.io.Serializable;

public class SaturnStatistics implements Serializable {

	private static final long serialVersionUID = 1l;

	private Integer id;
	private String name;
	private String zklist;
	private String result;

	public SaturnStatistics() {
	}

	public SaturnStatistics(String name, String zklist) {
		this.name = name;
		this.zklist = zklist;
	}

	public SaturnStatistics(String name, String zklist, String result) {
		this.name = name;
		this.zklist = zklist;
		this.result = result;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getZklist() {
		return zklist;
	}

	public void setZklist(String zklist) {
		this.zklist = zklist;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return "SaturnStatistics [id=" + id + ", name=" + name + ", zklist=" + zklist + ", result=" + result + "]";
	}

}
