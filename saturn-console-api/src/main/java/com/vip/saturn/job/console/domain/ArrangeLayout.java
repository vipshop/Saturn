package com.vip.saturn.job.console.domain;

import java.util.ArrayList;
import java.util.List;

public class ArrangeLayout {

	private List<ArrangePath> paths = new ArrayList<>();
	private List<List<ArrangeLevel>> levels = new ArrayList<>();

	public List<ArrangePath> getPaths() {
		return paths;
	}

	public void setPaths(List<ArrangePath> paths) {
		this.paths = paths;
	}

	public List<List<ArrangeLevel>> getLevels() {
		return levels;
	}

	public void setLevels(List<List<ArrangeLevel>> levels) {
		this.levels = levels;
	}

}
