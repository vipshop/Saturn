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

package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

import java.util.List;
import java.util.Map;

/**
 * @author rayleung
 */
public interface NamespaceService {

	/**
	 * 把源域的作业导入到目标域
	 * @param srcNamespace
	 * @param destNamespace
	 * @param createdBy
	 * @return 导入成功和导入失败作业列表
	 * @throws SaturnJobConsoleException
	 */
	Map<String, List> importJobsFromNamespaceToNamespace(String srcNamespace, String destNamespace, String createdBy)
			throws SaturnJobConsoleException;

	/**
	 * 删除域
	 * @param namespace
	 * @throws SaturnJobConsoleException
	 */
	void deleteNamespace(String namespace) throws SaturnJobConsoleException;

}
