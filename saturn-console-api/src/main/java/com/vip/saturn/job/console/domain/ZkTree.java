package com.vip.saturn.job.console.domain;

import java.util.ArrayList;
import java.util.List;
import org.apache.zookeeper.data.Stat;

/**
 * @author hebelala
 */
public class ZkTree {

	private String name;
	private String data;
	private Stat stat;
	private List<ZkTree> children = new ArrayList<>();

	public ZkTree() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Stat getStat() {
		return stat;
	}

	public void setStat(Stat stat) {
		this.stat = stat;
	}

	public List<ZkTree> getChildren() {
		return children;
	}

	public void setChildren(List<ZkTree> children) {
		this.children = children;
	}
}
