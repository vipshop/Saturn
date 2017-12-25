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
package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.SaturnJunkDataService;
import com.vip.saturn.job.console.service.helper.ReuseCallBack;
import com.vip.saturn.job.console.service.helper.ReuseUtils;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yangjuanying
 */
@Service
public class SaturnJunkDataServiceImpl implements SaturnJunkDataService {

	private static final Logger log = LoggerFactory.getLogger(SaturnJunkDataServiceImpl.class);

	@Resource
	private RegistryCenterService registryCenterService;

	@Resource
	private CuratorRepository curatorRepository;

	@Override
	public List<SaturnJunkData> getJunkData(String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
		if (zkCluster == null) {
			throw new SaturnJobConsoleException("No zkCluster matched with " + zkClusterKey);
		}
		if (zkCluster.isOffline()) {
			throw new SaturnJobConsoleException("Connect zookeeper failed");
		}
		ArrayList<RegistryCenterConfiguration> regCenterConfList = zkCluster.getRegCenterConfList();
		if (regCenterConfList == null || regCenterConfList.isEmpty()) {
			return new ArrayList<>();
		}
		List<SaturnJunkData> saturnJunkDataList = new ArrayList<>();
		for (RegistryCenterConfiguration conf : regCenterConfList) {
			String namespace = conf.getNamespace();
			RegistryCenterClient registryCenterClient = registryCenterService.connectByNamespace(namespace);
			if (registryCenterClient == null || !registryCenterClient.isConnected()) {
				continue;
			}
			String zkAddressList = conf.getZkAddressList();
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository
					.newCuratorFrameworkOp(registryCenterClient.getCuratorClient());
			List<String> jobNames = curatorFrameworkOp.getChildren(JobNodePath.get$JobsNodePath());
			if (jobNames != null) {
				for (String jobName : jobNames) {
					if (curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(jobName))) { // $Jobs/jobName/config
																									// exists
						String toDeletePath = JobNodePath.getConfigNodePath(jobName, "toDelete");
						if (curatorFrameworkOp.checkExists(toDeletePath)) { // toDelete node is junk data
							SaturnJunkData saturnJunkData = new SaturnJunkData();
							saturnJunkData.setPath(toDeletePath);
							saturnJunkData.setNamespace(namespace);
							saturnJunkData.setType(SaturnJunkDataOpType.DELETE.toString());
							saturnJunkData.setDescription("删除toDelete节点");
							saturnJunkData.setZkAddr(zkAddressList);
							saturnJunkDataList.add(saturnJunkData);
						}
						String jobConfigForceShardNodePath = SaturnExecutorsNode
								.getJobConfigForceShardNodePath(jobName);
						if (curatorFrameworkOp.checkExists(jobConfigForceShardNodePath)) { // forceShard node is junk
																							// data
							SaturnJunkData saturnJunkData = new SaturnJunkData();
							saturnJunkData.setPath(jobConfigForceShardNodePath);
							saturnJunkData.setNamespace(namespace);
							saturnJunkData.setType(SaturnJunkDataOpType.DELETE.toString());
							saturnJunkData.setDescription("删除forceShard节点");
							saturnJunkData.setZkAddr(zkAddressList);
							saturnJunkDataList.add(saturnJunkData);
						}
						String serverNodePath = JobNodePath.getServerNodePath(jobName);
						if (!curatorFrameworkOp.checkExists(serverNodePath)) {
							continue;
						}
						List<String> servers = curatorFrameworkOp.getChildren(serverNodePath);
						if (servers == null || servers.isEmpty()) {
							continue;
						}
						for (String server : servers) {
							String runOneTimePath = JobNodePath.getRunOneTimePath(jobName, server);
							if (curatorFrameworkOp.checkExists(runOneTimePath)) { // runOneTime node is junk data
								SaturnJunkData saturnJunkData = new SaturnJunkData();
								saturnJunkData.setPath(runOneTimePath);
								saturnJunkData.setNamespace(namespace);
								saturnJunkData.setType(SaturnJunkDataOpType.DELETE.toString());
								saturnJunkData.setDescription("删除runOneTime节点");
								saturnJunkData.setZkAddr(zkAddressList);
								saturnJunkDataList.add(saturnJunkData);
							}

							String stopOneTimePath = JobNodePath.getStopOneTimePath(jobName, server);
							if (curatorFrameworkOp.checkExists(stopOneTimePath)) { // stopOneTime node is junk data
								SaturnJunkData saturnJunkData = new SaturnJunkData();
								saturnJunkData.setPath(stopOneTimePath);
								saturnJunkData.setNamespace(namespace);
								saturnJunkData.setType(SaturnJunkDataOpType.DELETE.toString());
								saturnJunkData.setDescription("删除stopOneTime节点");
								saturnJunkData.setZkAddr(zkAddressList);
								saturnJunkDataList.add(saturnJunkData);
							}

							// $Jobs/servers/executors/executorName/sharding has contents, but this executor is
							// offline,this contents is junk data
							String serverShardingPath = JobNodePath.getServerSharding(jobName, server);
							String data = curatorFrameworkOp.getData(serverShardingPath);
							if (data != null && !data.trim().isEmpty()) {
								String executorIpNodePath = ExecutorNodePath.getExecutorIpNodePath(server);
								if (!curatorFrameworkOp.checkExists(executorIpNodePath)) {
									SaturnJunkData saturnJunkData = new SaturnJunkData();
									saturnJunkData.setPath(serverShardingPath);
									saturnJunkData.setNamespace(namespace);
									saturnJunkData.setType(SaturnJunkDataOpType.CLEAR.toString());
									saturnJunkData.setDescription("清除sharding内容（原因：该executor不在线）");
									saturnJunkData.setZkAddr(zkAddressList);
									saturnJunkDataList.add(saturnJunkData);
								}
							}
						}
					} else {
						// if $Jobs/jobName/config is not exists, but $Jobs/jobName/xxx exists,then $Jobs/jobName is
						// junk data
						List<String> children = curatorFrameworkOp.getChildren(JobNodePath.getJobNodePath(jobName));
						if (children != null && !children.isEmpty()) {
							SaturnJunkData saturnJunkData = new SaturnJunkData();
							saturnJunkData.setType(SaturnJunkDataOpType.DELETE.toString());
							saturnJunkData.setPath(JobNodePath.getJobNodePath(jobName));
							saturnJunkData.setNamespace(namespace);
							saturnJunkData.setDescription("删除整个作业节点（原因：$Jobs/" + jobName + "/config节点不存在）");
							saturnJunkData.setZkAddr(zkAddressList);
							saturnJunkDataList.add(saturnJunkData);
						}
					}
				}
			}
		}
		return saturnJunkDataList;
	}

	@Override
	public void removeSaturnJunkData(final SaturnJunkData saturnJunkData) throws SaturnJobConsoleException {
		ReuseUtils.reuse(saturnJunkData.getNamespace(), registryCenterService, curatorRepository,
				new ReuseCallBack<Void>() {
					@Override
					public Void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
							throws SaturnJobConsoleException {
						String path = saturnJunkData.getPath();
						String type = saturnJunkData.getType();
						if (path == null || path.trim().isEmpty()) {
							throw new SaturnJobConsoleException("The parameter path cannot be null or empty");
						}
						if (type == null || type.trim().isEmpty()) {
							throw new SaturnJobConsoleException("The parameter type cannot be null or empty");
						}

						if (SaturnJunkDataOpType.CLEAR.toString().equals(type)) {
							if (curatorFrameworkOp.checkExists(path)) {
								curatorFrameworkOp.update(path, "");
							}
						} else if (SaturnJunkDataOpType.DELETE.toString().equals(type)) {
							if (curatorFrameworkOp.checkExists(path)) {
								curatorFrameworkOp.deleteRecursive(path);
							}
						} else {
							throw new SaturnJobConsoleException("The parameter type(" + type + ") is not supported");
						}
						return null;
					}
				});
	}

	@Override
	public void deleteRunningNode(String namespace, final String jobName, final Integer item)
			throws SaturnJobConsoleException {
		ReuseUtils.reuse(namespace, jobName, registryCenterService, curatorRepository, new ReuseCallBack<Void>() {
			@Override
			public Void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
				try {
					String runningPath = JobNodePath.getExecutionNodePath(jobName, String.valueOf(item), "running");
					if (!curatorFrameworkOp.checkExists(runningPath)) {
						throw new SaturnJobConsoleException("The running path is not existing");
					}
					curatorFrameworkOp.deleteRecursive(runningPath);
				} catch (SaturnJobConsoleException e) {
					throw e;
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
					throw new SaturnJobConsoleException(t);
				}
				return null;
			}
		});
	}

}
