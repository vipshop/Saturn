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

package com.vip.saturn.job.console.domain;

/**
 * @author hebelala
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
