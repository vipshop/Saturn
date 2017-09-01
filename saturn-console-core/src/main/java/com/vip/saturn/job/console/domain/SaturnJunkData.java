package com.vip.saturn.job.console.domain;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @author yangjuanying
 */
public class SaturnJunkData implements Serializable {

	private static final long serialVersionUID = -3244143761577185890L;

	private String path;

	private String namespace;

	private String description;

	private String type;

	private String zkAddr;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getZkAddr() {
		return zkAddr;
	}

	public void setZkAddr(String zkAddr) {
		this.zkAddr = zkAddr;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
