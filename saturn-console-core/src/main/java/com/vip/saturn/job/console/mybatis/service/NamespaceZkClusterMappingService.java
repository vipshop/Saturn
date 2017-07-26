package com.vip.saturn.job.console.mybatis.service;

import java.util.List;

import com.vip.saturn.job.console.domain.MoveDomainBatchStatus;
import com.vip.saturn.job.console.domain.NamespaceZkClusterMappingVo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

/**
 * @author hebelala
 */
public interface NamespaceZkClusterMappingService {

	List<NamespaceZkClusterMappingVo> getNamespaceZkClusterMappingList() throws SaturnJobConsoleException;

	void initNamespaceZkClusterMapping(String createdBy) throws SaturnJobConsoleException;

	List<String> getZkClusterListWithOnlineFromCfg() throws SaturnJobConsoleException;

	void moveDomainTo(String namespace, String bootstrapKeyNew, String lastUpdatedBy, boolean updateDBOnly)
			throws SaturnJobConsoleException;

	void moveDomainBatchTo(String namespaces, String bootstrapKeyNew, String lastUpdatedBy, boolean updateDBOnly,
			long id) throws SaturnJobConsoleException;

	MoveDomainBatchStatus getMoveDomainBatchStatus(long id) throws SaturnJobConsoleException;

	void clearMoveDomainBatchStatus(long id) throws SaturnJobConsoleException;
}
