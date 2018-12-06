package com.vip.saturn.job.console.domain;

import java.util.ArrayList;
import java.util.List;

public class ArrangeNode {

	private String name;
	private List<String> children = new ArrayList<>();
	private int level;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getChildren() {
		return children;
	}

	public void setChildren(List<String> children) {
		this.children = children;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

}
