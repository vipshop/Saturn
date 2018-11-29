/**
 * Copyright 2016 vip.com. <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. </p>
 */

package com.vip.saturn.job.console.domain;

import java.io.Serializable;

public class DomainManagementVo implements Serializable {

	private String name;

	private String zkAddressList;

	private String namespace;

	private String zkAlias;

	private String zkClusterKey;

	private String digest;

	private String nameAndNamespace;

	/** 系统负责人 **/
	private String sysAdmin;

	/** 开发负责人 **/
	private String techAdmin;

	/** 系统重要程度 **/
	private String degree;

	private String version;

	private Boolean container;


	public DomainManagementVo() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getZkAddressList() {
		return zkAddressList;
	}

	public void setZkAddressList(String zkAddressList) {
		this.zkAddressList = zkAddressList;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getZkAlias() {
		return zkAlias;
	}

	public void setZkAlias(String zkAlias) {
		this.zkAlias = zkAlias;
	}

	public String getZkClusterKey() {
		return zkClusterKey;
	}

	public void setZkClusterKey(String zkClusterKey) {
		this.zkClusterKey = zkClusterKey;
	}

	public String getDigest() {
		return digest;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public String getNameAndNamespace() {
		return nameAndNamespace;
	}

	public void setNameAndNamespace(String nameAndNamespace) {
		this.nameAndNamespace = nameAndNamespace;
	}

	public String getSysAdmin() {
		return sysAdmin;
	}

	public void setSysAdmin(String sysAdmin) {
		this.sysAdmin = sysAdmin;
	}

	public String getTechAdmin() {
		return techAdmin;
	}

	public void setTechAdmin(String techAdmin) {
		this.techAdmin = techAdmin;
	}

	public String getDegree() {
		return degree;
	}

	public void setDegree(String degree) {
		this.degree = degree;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Boolean getContainer() {
		return container;
	}

	public void setContainer(Boolean container) {
		this.container = container;
	}
}
