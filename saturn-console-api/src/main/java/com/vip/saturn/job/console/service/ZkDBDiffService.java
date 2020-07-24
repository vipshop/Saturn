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

import com.vip.saturn.job.console.domain.JobDiffInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import java.util.List;

public interface ZkDBDiffService {

	/**
	 * Diff the config data in zk and db of the same zk cluster.
	 *
	 * @param clusterKey zk cluster key.
	 * @return The different info organized by job.
	 */
	List<JobDiffInfo> diffByCluster(String clusterKey) throws SaturnJobConsoleException;

	/**
	 * Diff the config data in zk and db of namespace.
	 *
	 * @return The different info organized by job.
	 */
	List<JobDiffInfo> diffByNamespace(String namespace) throws SaturnJobConsoleException;

	/**
	 * Diff the config data in zk and db of job.
	 *
	 * @return The different info organized by job. If no difference, return null;
	 */
	JobDiffInfo diffByJob(String namespace, String jobName) throws SaturnJobConsoleException;
}
