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

package com.vip.saturn.job.internal.election;

import java.util.concurrent.atomic.AtomicBoolean;

import com.vip.saturn.job.internal.storage.JobNodeStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.storage.LeaderExecutionCallback;

/**
 * 选举主节点的服务.
 * 
 * 
 */
public class LeaderElectionService extends AbstractSaturnService {
	static Logger log = LoggerFactory.getLogger(LeaderElectionService.class);

	private AtomicBoolean isShutdown = new AtomicBoolean(false);

	public LeaderElectionService(final JobScheduler jobScheduler) {
		super(jobScheduler);
	}

	@Override
	public void shutdown() {
		synchronized (isShutdown) {
			if (isShutdown.compareAndSet(false, true)) {
				try { // Release my leader position
					JobNodeStorage jobNodeStorage = getJobNodeStorage();
					if (jobNodeStorage.isConnected()
							&& executorName.equals(jobNodeStorage.getJobNodeDataDirectly(ElectionNode.LEADER_HOST))) {
						jobNodeStorage.removeJobNodeIfExisted(ElectionNode.LEADER_HOST);
						log.info("[{}] msg={} that was {}'s leader, released itself", jobName, executorName, jobName);
					}
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}

	/**
	 * 选举主节点.
	 */
	public void leaderElection() {
		getJobNodeStorage().executeInLeader(ElectionNode.LATCH, new LeaderElectionExecutionCallback());
	}

	/**
	 * 判断当前节点是否是主节点，如果没有主节点，则选举，直到有主节点
	 *
	 * @return 当前节点是否是主节点
	 */
	public Boolean isLeader() {
		while (!isShutdown.get() && !hasLeader()) {
			log.info("[{}] msg=No leader, try to election", jobName);
			leaderElection();
		}
		return executorName.equals(getJobNodeStorage().getJobNodeDataDirectly(ElectionNode.LEADER_HOST));
	}

	/**
	 * 判断是否已经有主节点
	 * 
	 * @return 是否已经有主节点
	 */
	public boolean hasLeader() {
		return getJobNodeStorage().isJobNodeExisted(ElectionNode.LEADER_HOST);
	}

	class LeaderElectionExecutionCallback implements LeaderExecutionCallback {

		@Override
		public void execute() {
			synchronized (isShutdown) {
				if (isShutdown.get()) {
					return;
				}
				if (!getJobNodeStorage().isJobNodeExisted(ElectionNode.LEADER_HOST)) {
					getJobNodeStorage().fillEphemeralJobNode(ElectionNode.LEADER_HOST, executorName);
					log.info("[{}] msg=executor {} become job {}'s leader", jobName, executorName, jobName);
				}
			}
		}
	}
}
