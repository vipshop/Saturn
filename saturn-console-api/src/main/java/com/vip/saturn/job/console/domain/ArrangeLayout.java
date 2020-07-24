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
