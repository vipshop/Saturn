/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.domain;

import java.io.Serializable;

/**
 * @author chembo.huang
 *
 */
public class RegistryCenterConfiguration implements Serializable {

	private static final long serialVersionUID = -5996257770767863699L;

	public static final String SLASH = "/";

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

	public RegistryCenterConfiguration() {
	}

	public String getName() {
		return this.name;
	}

	public String getZkAddressList() {
		return this.zkAddressList;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public String getDigest() {
		return this.digest;
	}

	public String getNameAndNamespace() {
		return this.nameAndNamespace;
	}

	public String getSysAdmin() {
		return this.sysAdmin;
	}

	public String getTechAdmin() {
		return this.techAdmin;
	}

	public String getDegree() {
		return this.degree;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setZkAddressList(String zkAddressList) {
		this.zkAddressList = zkAddressList;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	/**
	 * please use initNameAndNamespace method
	 */
	public void setNameAndNamespace(String nameAndNamespace) {
		this.nameAndNamespace = nameAndNamespace;
	}

	public void setSysAdmin(String sysAdmin) {
		this.sysAdmin = sysAdmin;
	}

	public void setTechAdmin(String techAdmin) {
		this.techAdmin = techAdmin;
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

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o == null)
			return false;
		if (this.getClass() != o.getClass())
			return false;
		RegistryCenterConfiguration other = (RegistryCenterConfiguration) o;
		if (!other.canEqual(this))
			return false;
		Object this$name = getName();
		Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name))
			return false;
		Object this$namespace = getNamespace();
		Object other$namespace = other.getNamespace();
		if (this$namespace == null ? other$namespace != null : !this$namespace.equals(other$namespace))
			return false;
		Object this$sysAdmin = getSysAdmin();
		Object other$sysAdmin = other.getSysAdmin();
		if (this$sysAdmin == null ? other$sysAdmin != null : !this$sysAdmin.equals(other$sysAdmin))
			return false;
		Object this$techAdmin = getTechAdmin();
		Object other$techAdmin = other.getTechAdmin();
		if (this$techAdmin == null ? other$techAdmin != null : !this$techAdmin.equals(other$techAdmin))
			return false;
		Object this$degree = getDegree();
		Object other$degree = other.getDegree();
		return this$degree == null ? other$degree == null : this$degree.equals(other$degree);
	}

	protected boolean canEqual(Object other) {
		return other instanceof RegistryCenterConfiguration;
	}

	public int hashCode() {
		int result = 1;
		Object $name = getName();
		result = result * 59 + ($name == null ? 43 : $name.hashCode());
		Object $namespace = getNamespace();
		result = result * 59 + ($namespace == null ? 43 : $namespace.hashCode());
		Object $sysAdmin = getSysAdmin();
		result = result * 59 + ($sysAdmin == null ? 43 : $sysAdmin.hashCode());
		Object $techAdmin = getTechAdmin();
		result = result * 59 + ($techAdmin == null ? 43 : $techAdmin.hashCode());
		Object $degree = getDegree();
		return result * 59 + ($degree == null ? 43 : $degree.hashCode());
	}

	@Override
	public String toString() {
		return "RegistryCenterConfiguration [name=" + name + ", zkAddressList=" + zkAddressList + ", namespace="
				+ namespace + ", zkAlias=" + zkAlias + ", zkClusterKey=" + zkClusterKey + ", digest=" + digest
				+ ", nameAndNamespace=" + nameAndNamespace + ", sysAdmin=" + sysAdmin + ", techAdmin=" + techAdmin
				+ ", degree=" + degree + ", version=" + version + "]";
	}

}
