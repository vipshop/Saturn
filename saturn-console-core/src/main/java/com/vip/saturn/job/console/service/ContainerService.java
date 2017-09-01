package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.container.ContainerConfig;
import com.vip.saturn.job.console.domain.container.ContainerToken;
import com.vip.saturn.job.console.domain.container.vo.ContainerScaleJobVo;
import com.vip.saturn.job.console.domain.container.vo.ContainerVo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;

import java.util.List;

/**
 * @author hebelala
 */
public interface ContainerService {

	void checkContainerTokenNotNull(ContainerToken containerToken) throws SaturnJobConsoleException;

	void saveOrUpdateContainerToken(ContainerToken containerToken) throws SaturnJobConsoleException;

	void saveOrUpdateContainerTokenIfNecessary(ContainerToken containerToken) throws SaturnJobConsoleException;

	ContainerToken getContainerToken() throws SaturnJobConsoleException;

	ContainerToken getContainerToken(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException;

	List<ContainerVo> getContainerVos() throws SaturnJobConsoleException;

	void addContainer(ContainerConfig containerConfig) throws SaturnJobConsoleException;

	void updateContainerInstances(String taskId, Integer instances) throws SaturnJobConsoleException;

	void removeContainer(String taskId) throws SaturnJobConsoleException;

	String getContainerDetail(String taskId) throws SaturnJobConsoleException;

	int getContainerRunningInstances(String taskId, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp)
			throws SaturnJobConsoleException;

	String getRegistryCatalog() throws SaturnJobConsoleException;

	String getRegistryRepositoryTags(String repository) throws SaturnJobConsoleException;

	void addContainerScaleJob(String taskId, String jobDesc, Integer instances, String timeZone, String cron)
			throws SaturnJobConsoleException;

	ContainerScaleJobVo getContainerScaleJobVo(String taskId, String jobName) throws SaturnJobConsoleException;

	List<ContainerScaleJobVo> getContainerScaleJobVos(String taskId) throws SaturnJobConsoleException;

	void enableContainerScaleJob(String jobName, Boolean enable) throws SaturnJobConsoleException;

	void deleteContainerScaleJob(String taskId, String jobName) throws SaturnJobConsoleException;

}