/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.internal.server;

import com.vip.saturn.job.basic.AbstractElasticJob;
import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.election.LeaderElectionService;
import com.vip.saturn.job.utils.LocalHostService;

import java.util.Collections;
import java.util.List;

/**
 * 作业服务器节点服务.
 * @author dylan.xue
 */
public class ServerService extends AbstractSaturnService {

	private LeaderElectionService leaderElectionService;

	public ServerService(final JobScheduler jobScheduler) {
		super(jobScheduler);

	}

	@Override
	public void start() {
		leaderElectionService = jobScheduler.getLeaderElectionService();
	}

	/**
	 * 持久化作业服务器上线相关信息.
	 */
	public void persistServerOnline(AbstractElasticJob job) {
		if (!leaderElectionService.hasLeader()) {
			leaderElectionService.leaderElection();
		}
		persistIp();
		persistVersion();
		persistJobVersion(job.getJobVersion());
		ephemeralPersistServerReady();
	}

	public void persistVersion() {
		String executorVersion = jobScheduler.getSaturnExecutorService().getExecutorVersion();
		if (executorVersion != null) {
			getJobNodeStorage().fillJobNodeIfNullOrOverwrite(ServerNode.getVersionNode(executorName), executorVersion);
		}
	}

	public void resetCount() {
		persistProcessFailureCount(0);
		persistProcessSuccessCount(0);
	}

	private void persistIp() {
		getJobNodeStorage().fillJobNodeIfNullOrOverwrite(ServerNode.getIpNode(executorName),
				LocalHostService.cachedIpAddress);
	}

	private void persistJobVersion(String jobVersion) {
		if (jobVersion != null) {
			getJobNodeStorage().fillJobNodeIfNullOrOverwrite(ServerNode.getJobVersionNode(executorName), jobVersion);
		}
	}

	private void ephemeralPersistServerReady() {
		getJobNodeStorage().fillEphemeralJobNode(ServerNode.getStatusNode(executorName), "");
	}

	/**
	 * 清除立即运行的标记
	 */
	public void clearRunOneTimePath() {
		getJobNodeStorage().removeJobNodeIfExisted(ServerNode.getRunOneTimePath(executorName));
	}

	/**
	 * 清除立即终止作业的标记
	 */
	public void clearStopOneTimePath() {
		getJobNodeStorage().removeJobNodeIfExisted(ServerNode.getStopOneTimePath(executorName));
	}

	/**
	 * 获取该作业的所有服务器列表.
	 *
	 * @return 所有的作业服务器列表
	 */
	public List<String> getAllServers() {
		List<String> result = getJobNodeStorage().getJobNodeChildrenKeys(ServerNode.ROOT);
		Collections.sort(result);
		return result;
	}

	/**
	 * 持久化统计处理数据成功的数量的数据.
	 *
	 * @param processSuccessCount 成功数
	 */
	public void persistProcessSuccessCount(final int processSuccessCount) {
		getJobNodeStorage().replaceJobNode(ServerNode.getProcessSuccessCountNode(executorName), processSuccessCount);
	}

	/**
	 * 持久化统计处理数据失败的数量的数据.
	 *
	 * @param processFailureCount 失败数
	 */
	public void persistProcessFailureCount(final int processFailureCount) {
		getJobNodeStorage().replaceJobNode(ServerNode.getProcessFailureCountNode(executorName), processFailureCount);
	}

}
