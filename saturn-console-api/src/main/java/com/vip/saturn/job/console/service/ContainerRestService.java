package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.container.ContainerConfig;
import com.vip.saturn.job.console.domain.container.ContainerStatus;
import com.vip.saturn.job.console.domain.container.ContainerToken;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

/**
 * @author hebelala
 */
public interface ContainerRestService {

	String serializeContainerToken(ContainerToken containerToken) throws SaturnJobConsoleException;

	ContainerToken deserializeContainerToken(String containerTokenStr) throws SaturnJobConsoleException;

	void checkContainerTokenNotNull(ContainerToken containerToken) throws SaturnJobConsoleException;

	boolean containerTokenEquals(ContainerToken ctNew, ContainerToken ctOld) throws SaturnJobConsoleException;

	String getContainerScaleJobShardingItemParameters(ContainerToken containerToken, String appId, Integer instances)
			throws SaturnJobConsoleException;

	ContainerStatus getContainerStatus(ContainerToken containerToken, String appId) throws SaturnJobConsoleException;

	void deploy(ContainerToken containerToken, ContainerConfig containerConfig) throws SaturnJobConsoleException;

	void scale(ContainerToken containerToken, String appId, Integer instances) throws SaturnJobConsoleException;

	void destroy(ContainerToken containerToken, String appId) throws SaturnJobConsoleException;

	int count(ContainerToken containerToken, String appId) throws SaturnJobConsoleException;

	String info(ContainerToken containerToken, String appId) throws SaturnJobConsoleException;

	String getRegistryCatalog() throws SaturnJobConsoleException;

	String getRegistryRepositoryTags(String repository) throws SaturnJobConsoleException;
}
