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

/**
 * 域信息
 *
 * @author chembo.huang
 *
 */
public class RegistryCenterConfiguration implements Serializable {

	public static final String SLASH = "/";
	private static final long serialVersionUID = -5996257770767863699L;
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

	public RegistryCenterConfiguration(final String name, final String namespace, final String zkAddressList) {
		this.name = name;
		this.namespace = namespace;
		initNameAndNamespace();
		this.zkAddressList = zkAddressList;
	}

	public RegistryCenterConfiguration() {
	}

	/**
	 * before invoke this method, be sure that the name and namespace are set, and the namespace cannot be null
	 */
	public void initNameAndNamespace() {
		// namespace cannot be null, i am sure. But the name could be.
		if (name != null) {
			this.nameAndNamespace = name + SLASH + namespace;
		} else {
			this.nameAndNamespace = SLASH + namespace;
		}
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

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getZkAddressList() {
		return this.zkAddressList;
	}

	public void setZkAddressList(String zkAddressList) {
		this.zkAddressList = zkAddressList;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getDigest() {
		return this.digest;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public String getNameAndNamespace() {
		return this.nameAndNamespace;
	}

	/**
	 * please use initNameAndNamespace method
	 */
	public void setNameAndNamespace(String nameAndNamespace) {
		this.nameAndNamespace = nameAndNamespace;
	}

	public String getSysAdmin() {
		return this.sysAdmin;
	}

	public void setSysAdmin(String sysAdmin) {
		this.sysAdmin = sysAdmin;
	}

	public String getTechAdmin() {
		return this.techAdmin;
	}

	public void setTechAdmin(String techAdmin) {
		this.techAdmin = techAdmin;
	}

	public String getDegree() {
		return this.degree;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		RegistryCenterConfiguration that = (RegistryCenterConfiguration) o;

		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (zkAddressList != null ? !zkAddressList.equals(that.zkAddressList) : that.zkAddressList != null)
			return false;
		if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;
		if (zkAlias != null ? !zkAlias.equals(that.zkAlias) : that.zkAlias != null) return false;
		if (zkClusterKey != null ? !zkClusterKey.equals(that.zkClusterKey) : that.zkClusterKey != null) return false;
		if (digest != null ? !digest.equals(that.digest) : that.digest != null) return false;
		if (nameAndNamespace != null ? !nameAndNamespace.equals(that.nameAndNamespace) : that.nameAndNamespace != null)
			return false;
		if (sysAdmin != null ? !sysAdmin.equals(that.sysAdmin) : that.sysAdmin != null) return false;
		if (techAdmin != null ? !techAdmin.equals(that.techAdmin) : that.techAdmin != null) return false;
		if (degree != null ? !degree.equals(that.degree) : that.degree != null) return false;
		return version != null ? version.equals(that.version) : that.version == null;

	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (zkAddressList != null ? zkAddressList.hashCode() : 0);
		result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
		result = 31 * result + (zkAlias != null ? zkAlias.hashCode() : 0);
		result = 31 * result + (zkClusterKey != null ? zkClusterKey.hashCode() : 0);
		result = 31 * result + (digest != null ? digest.hashCode() : 0);
		result = 31 * result + (nameAndNamespace != null ? nameAndNamespace.hashCode() : 0);
		result = 31 * result + (sysAdmin != null ? sysAdmin.hashCode() : 0);
		result = 31 * result + (techAdmin != null ? techAdmin.hashCode() : 0);
		result = 31 * result + (degree != null ? degree.hashCode() : 0);
		result = 31 * result + (version != null ? version.hashCode() : 0);
		return result;
	}

	protected boolean canEqual(Object other) {
		return other instanceof RegistryCenterConfiguration;
	}

	@Override
	public String toString() {
		return "RegistryCenterConfiguration [name=" + name + ", zkAddressList=" + zkAddressList + ", namespace="
				+ namespace + ", zkAlias=" + zkAlias + ", zkClusterKey=" + zkClusterKey + ", digest=" + digest
				+ ", nameAndNamespace=" + nameAndNamespace + ", sysAdmin=" + sysAdmin + ", techAdmin=" + techAdmin
				+ ", degree=" + degree + ", version=" + version + "]";
	}

}
