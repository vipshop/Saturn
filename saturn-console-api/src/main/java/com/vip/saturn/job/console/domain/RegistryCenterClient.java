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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.curator.framework.CuratorFramework;

/**
 * @author chembo.huang
 *
 */
public final class RegistryCenterClient implements Serializable {

	private static final long serialVersionUID = -946258964014121184L;

	private String nameAndNamespace;

	@JsonIgnore
	private transient CuratorFramework curatorClient;

	private boolean connected;

	private String zkAddr;

	public RegistryCenterClient() {

	}

	public RegistryCenterClient(final String nameAndNamespace) {
		this.nameAndNamespace = nameAndNamespace;
	}

	public String getNameAndNamespace() {
		return nameAndNamespace;
	}

	public void setNameAndNamespace(String nameAndNamespace) {
		this.nameAndNamespace = nameAndNamespace;
	}

	public String getZkAddr() {
		return zkAddr;
	}

	public void setZkAddr(String zkAddr) {
		this.zkAddr = zkAddr;
	}

	public CuratorFramework getCuratorClient() {
		return curatorClient;
	}

	public void setCuratorClient(CuratorFramework curatorClient) {
		this.curatorClient = curatorClient;
	}

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public void close() throws Exception {
		if (curatorClient != null) {
			curatorClient.close();
			curatorClient = null;
			connected = false;
		}
	}

}
