package com.vip.saturn.job.console.domain;

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

}
