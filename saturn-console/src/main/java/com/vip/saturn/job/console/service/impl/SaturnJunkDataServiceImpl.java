/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */   
package com.vip.saturn.job.console.service.impl;   

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.helper.ReuseCallBack;
import com.vip.saturn.job.console.service.helper.ReuseUtils;
import com.vip.saturn.job.console.utils.SaturnConstants;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.exception.JobConsoleException;
import com.vip.saturn.job.console.service.SaturnJunkDataService;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;

import javax.annotation.Resource;

/** 
 * @author yangjuanying  
 */
@Service
public class SaturnJunkDataServiceImpl implements SaturnJunkDataService {
	
	protected static Logger log = LoggerFactory.getLogger(SaturnJunkDataServiceImpl.class);

	@Resource
	private RegistryCenterService registryCenterService;

	@Resource
	private CuratorRepository curatorRepository;

	@Override
	public Collection<SaturnJunkData> getJunkData(String zkAddr) {
		if(zkAddr == null) {
			return Collections.emptyList();
		}
		ZkCluster zkCluster = RegistryCenterServiceImpl.ZKADDR_TO_ZKCLUSTER_MAP.get(zkAddr);
		if(zkCluster == null) {
			return Collections.emptyList();
		}
		ArrayList<RegistryCenterConfiguration> registryCenterList = zkCluster.getRegCenterConfList();
		if(CollectionUtils.isEmpty(registryCenterList)){
			return Collections.emptyList();
		}
		List<SaturnJunkData> saturnJunkDataList = new ArrayList<SaturnJunkData>();
		for(RegistryCenterConfiguration registryCenter : registryCenterList){
			try {
				String namespace = registryCenter.getNameAndNamespace();
				RegistryCenterClient registryCenterClient = RegistryCenterServiceImpl.getCuratorByNameAndNamespace(namespace);
				if(registryCenterClient == null){
					continue;
				}
				String zkBootstrapKey = registryCenter.getBootstrapKey();
				CuratorFramework curatorFramework = registryCenterClient.getCuratorClient();
				List<String> jobNames = curatorFramework.getChildren().forPath(JobNodePath.get$JobsNodePath());
				for (String jobName : jobNames) {
					if(null != curatorFramework.checkExists().forPath(JobNodePath.getConfigNodePath(jobName))) {// $Jobs/jobName/config exists
						String toDeletePath = JobNodePath.getConfigNodePath(jobName, "toDelete");
						if(null != curatorFramework.checkExists().forPath(toDeletePath)){// toDelete node is junk data
							SaturnJunkData saturnJunkData = new SaturnJunkData();
							saturnJunkData.setPath(toDeletePath);
							saturnJunkData.setNamespace(namespace);
							saturnJunkData.setType(SaturnJunkDataOpType.DELETE.toString());
							saturnJunkData.setDescription("删除toDelete节点");
							saturnJunkData.setZkAddr(zkBootstrapKey);
							saturnJunkDataList.add(saturnJunkData);
						}
						String jobConfigForceShardNodePath = SaturnExecutorsNode.getJobConfigForceShardNodePath(jobName);
		                if (null != curatorFramework.checkExists().forPath(jobConfigForceShardNodePath)) {// forceShard node is junk data
		                	SaturnJunkData saturnJunkData = new SaturnJunkData();
							saturnJunkData.setPath(jobConfigForceShardNodePath);
							saturnJunkData.setNamespace(namespace);
							saturnJunkData.setType(SaturnJunkDataOpType.DELETE.toString());
							saturnJunkData.setDescription("删除forceShard节点");
							saturnJunkData.setZkAddr(zkBootstrapKey);
							saturnJunkDataList.add(saturnJunkData);
		                }
						if(null == curatorFramework.checkExists().forPath(JobNodePath.getServerNodePath(jobName))){
							continue;
						}
						List<String> jobExecutors = curatorFramework.getChildren().forPath(JobNodePath.getServerNodePath(jobName));
						if(CollectionUtils.isEmpty(jobExecutors)){
							continue;
						}
						for(String jobExecutor : jobExecutors){
							String runOneTimePath = JobNodePath.getRunOneTimePath(jobName, jobExecutor);
							if(null != curatorFramework.checkExists().forPath(runOneTimePath)){// runOneTime node is junk data
								SaturnJunkData saturnJunkData = new SaturnJunkData();
								saturnJunkData.setPath(runOneTimePath);
								saturnJunkData.setNamespace(namespace);
								saturnJunkData.setType(SaturnJunkDataOpType.DELETE.toString());
								saturnJunkData.setDescription("删除runOneTime节点");
								saturnJunkData.setZkAddr(zkBootstrapKey);
								saturnJunkDataList.add(saturnJunkData);
							}
							
							String stopOneTimePath = JobNodePath.getStopOneTimePath(jobName, jobExecutor);
							if(null != curatorFramework.checkExists().forPath(stopOneTimePath)){// stopOneTime node is junk data
								SaturnJunkData saturnJunkData = new SaturnJunkData();
								saturnJunkData.setPath(stopOneTimePath);
								saturnJunkData.setNamespace(namespace);
								saturnJunkData.setType(SaturnJunkDataOpType.DELETE.toString());
								saturnJunkData.setDescription("删除stopOneTime节点");
								saturnJunkData.setZkAddr(zkBootstrapKey);
								saturnJunkDataList.add(saturnJunkData);
							}
							
							String shardingPath = JobNodePath.getServerSharding(jobName,jobExecutor);
							// $Jobs/servers/executors/executorName/sharding has contents, but this executor is offline,this contents is junk data
							if(!Strings.isNullOrEmpty(getData(curatorFramework,shardingPath)) && null == curatorFramework.checkExists().forPath(ExecutorNodePath.getExecutorIpNodePath(jobExecutor))){
								SaturnJunkData saturnJunkData = new SaturnJunkData();
								saturnJunkData.setPath(shardingPath);
								saturnJunkData.setNamespace(namespace);
								saturnJunkData.setType(SaturnJunkDataOpType.CLEAR.toString());
								saturnJunkData.setDescription("清除sharding内容（原因：该executor不在线）");
								saturnJunkData.setZkAddr(zkBootstrapKey);
								saturnJunkDataList.add(saturnJunkData);
							}
						}
					}else if(!CollectionUtils.isEmpty(curatorFramework.getChildren().forPath(JobNodePath.getJobNodePath(jobName)))) {
						// if $Jobs/jobName/config is not exists, but $Jobs/jobName/xxx exists,then $Jobs/jobName is junk data
						SaturnJunkData saturnJunkData = new SaturnJunkData();
						saturnJunkData.setType(SaturnJunkDataOpType.DELETE.toString());
						saturnJunkData.setPath(JobNodePath.getJobNodePath(jobName));
						saturnJunkData.setNamespace(namespace);
						saturnJunkData.setDescription("删除整个作业节点（原因：$Jobs/"+jobName+"/config节点不存在）");
						saturnJunkData.setZkAddr(zkBootstrapKey);
						saturnJunkDataList.add(saturnJunkData);
					}
				}
			}catch(Exception e){
				log.error("getJunkData exception:",e);
			}
		}
		return saturnJunkDataList;
	}
	
	public String getData(final CuratorFramework curatorClient, final String znode) {
		try {
			if (null != curatorClient.checkExists().forPath(znode)) {
				byte[] getZnodeData = curatorClient.getData().forPath(znode);
				if (getZnodeData == null) {// executor的分片可能存在全部飘走的情况，sharding节点有可能获取到的是null，需要对null做判断，否则new
											// String时会报空指针异常
					return null;
				}
				return new String(getZnodeData, Charset.forName("UTF-8"));
			} else {
				return null;
			}
		} catch (final NoNodeException ex) {
			return null;
			// CHECKSTYLE:OFF
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			throw new JobConsoleException(ex);
		}
	}

	@Override
	public String removeSaturnJunkData(SaturnJunkData saturnJunkData) {
		try{
			ArrayList<RegistryCenterConfiguration> registryCenterList = RegistryCenterServiceImpl.ZKADDR_TO_ZKCLUSTER_MAP.get(saturnJunkData.getZkAddr()).getRegCenterConfList();
			if(CollectionUtils.isEmpty(registryCenterList)){
				return "清理废弃数据失败，根据集群zk key:"+saturnJunkData.getZkAddr()+"，在注册中心没有取到相关信息";
			}
			for(RegistryCenterConfiguration registryCenter : registryCenterList){
				String namespace = registryCenter.getNameAndNamespace();
				RegistryCenterClient registryCenterClient = RegistryCenterServiceImpl.getCuratorByNameAndNamespace(namespace);
				if(registryCenterClient == null){
					continue;
				}
				CuratorFramework curatorFramework = registryCenterClient.getCuratorClient();
				if(SaturnJunkDataOpType.CLEAR.toString().equals(saturnJunkData.getType())){
					if(null != curatorFramework.checkExists().forPath(saturnJunkData.getPath())){
						curatorFramework.inTransaction().check().forPath(saturnJunkData.getPath()).and().setData().forPath(saturnJunkData.getPath(), "".getBytes(Charset.forName("UTF-8"))).and().commit();
					}
				}else if(SaturnJunkDataOpType.DELETE.toString().equals(saturnJunkData.getType())){
					if(null != curatorFramework.checkExists().forPath(saturnJunkData.getPath())){
						curatorFramework.delete().deletingChildrenIfNeeded().forPath(saturnJunkData.getPath());
					}
				}
			}
			return SaturnConstants.DEAL_SUCCESS;
		}catch(Exception e){
			log.error("removeSaturnJunkData exception:",e);
			return "清理废弃数据失败:"+e.getMessage();
		}
	}

	@Override
	public void deleteRunningNode(String namespace, final String jobName, final Integer item) throws SaturnJobConsoleException {
		ReuseUtils.reuse(namespace, jobName, registryCenterService, curatorRepository, new ReuseCallBack<Void>() {
			@Override
			public Void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
				try {
					String runningPath = JobNodePath.getExecutionNodePath(jobName, String.valueOf(item), "running");
					if(!curatorFrameworkOp.checkExists(runningPath)) {
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
  