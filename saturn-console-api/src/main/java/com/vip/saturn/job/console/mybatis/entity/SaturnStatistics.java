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

package com.vip.saturn.job.console.mybatis.entity;

import java.io.Serializable;

public class SaturnStatistics implements Serializable {

	private static final long serialVersionUID = 1l;

	private Integer id;
	private String name;
	private String zklist;
	private String result;

	public SaturnStatistics() {
	}

	public SaturnStatistics(String name, String zklist) {
		this.name = name;
		this.zklist = zklist;
	}

	public SaturnStatistics(String name, String zklist, String result) {
		this.name = name;
		this.zklist = zklist;
		this.result = result;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getZklist() {
		return zklist;
	}

	public void setZklist(String zklist) {
		this.zklist = zklist;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return "SaturnStatistics [id=" + id + ", name=" + name + ", zklist=" + zklist + ", result=" + result + "]";
	}

}
