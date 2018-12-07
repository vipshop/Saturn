package com.vip.saturn.job.console.domain;

import java.util.ArrayList;
import java.util.List;

public class ArrangeNode extends ArrangeLevel {

	private List<String> children = new ArrayList<>();
	private int level;

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
