package com.vip.saturn.job.console.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author hebelala
 *
 */
public class ExportJobConfigPageStatus {

	private boolean exported;
	private boolean success;
	private int successNamespaceNum;
	private int successJobNum;
	private List<RegistryCenterConfiguration> regCenterConfList = new ArrayList<>();
	private Map<String, RegistryCenterConfiguration> regCenterConfMap = new HashMap<String, RegistryCenterConfiguration>();

	public boolean isExported() {
		return exported;
	}

	public void setExported(boolean exported) {
		this.exported = exported;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public int getSuccessNamespaceNum() {
		return successNamespaceNum;
	}

	public void setSuccessNamespaceNum(int successNamespaceNum) {
		this.successNamespaceNum = successNamespaceNum;
	}

	public int getSuccessJobNum() {
		return successJobNum;
	}

	public void setSuccessJobNum(int successJobNum) {
		this.successJobNum = successJobNum;
	}

	public List<RegistryCenterConfiguration> getRegCenterConfList() {
		return regCenterConfList;
	}

	public void setRegCenterConfList(List<RegistryCenterConfiguration> regCenterConfList) {
		this.regCenterConfList = regCenterConfList;
	}

	public Map<String, RegistryCenterConfiguration> getRegCenterConfMap() {
		return regCenterConfMap;
	}

	public void setRegCenterConfMap(Map<String, RegistryCenterConfiguration> regCenterConfMap) {
		this.regCenterConfMap = regCenterConfMap;
	}

}
