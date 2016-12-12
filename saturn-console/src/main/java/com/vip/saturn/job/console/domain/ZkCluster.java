/**
 * 
 */
package com.vip.saturn.job.console.domain;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.curator.framework.CuratorFramework;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author chembo.huang
 *
 */
public class ZkCluster implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String zkAlias;
	
	private String zkAddr;
	
	private boolean offline = false;
	
	@JsonIgnore
	private transient  CuratorFramework curatorFramework;
	
	@JsonIgnore
	private transient ArrayList<RegistryCenterConfiguration> regCenterConfList = new ArrayList<>();
	
	public ZkCluster(String zkAlias, String zkAddr, CuratorFramework curatorFramework) {
		this.zkAddr = zkAddr;
		this.zkAlias = zkAlias;
		this.curatorFramework = curatorFramework;
	}

	public boolean isOffline() {
		return offline;
	}

	public void setOffline(boolean offline) {
		this.offline = offline;
	}

	public String getZkAlias() {
		return zkAlias;
	}

	public void setZkAlias(String zkAlias) {
		this.zkAlias = zkAlias;
	}

	public String getZkAddr() {
		return zkAddr;
	}

	public void setZkAddr(String zkAddr) {
		this.zkAddr = zkAddr;
	}

	public CuratorFramework getCuratorFramework() {
		return curatorFramework;
	}

	public void setCuratorFramework(CuratorFramework curatorFramework) {
		this.curatorFramework = curatorFramework;
	}

	public ArrayList<RegistryCenterConfiguration> getRegCenterConfList() {
		return regCenterConfList;
	}

	public void setRegCenterConfList(
			ArrayList<RegistryCenterConfiguration> regCenterConfList) {
		this.regCenterConfList = regCenterConfList;
	}

	@Override
	public String toString() {
		return "ZkCluster [zkAlias=" + zkAlias + ", zkAddr=" + zkAddr
				+ ", offline=" + offline + ", regCenterConfList="
				+ regCenterConfList + "]";
	}
}
