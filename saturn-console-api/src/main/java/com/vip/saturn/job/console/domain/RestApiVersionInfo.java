package com.vip.saturn.job.console.domain;

/**
 * @author timmy.hu
 */
public class RestApiVersionInfo {

	private String versionNumber;

	private String checkCode;

	private boolean forced;

	public String getVersionNumber() {
		return versionNumber;
	}

	public void setVersionNumber(String versionNumber) {
		this.versionNumber = versionNumber;
	}

	public String getCheckCode() {
		return checkCode;
	}

	public void setCheckCode(String checkCode) {
		this.checkCode = checkCode;
	}

	public boolean isForced() {
		return forced;
	}

	public void setForced(boolean forced) {
		this.forced = forced;
	}


}
