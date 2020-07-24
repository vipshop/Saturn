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
