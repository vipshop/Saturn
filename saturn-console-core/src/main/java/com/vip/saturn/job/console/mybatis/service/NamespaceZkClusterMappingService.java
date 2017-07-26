package com.vip.saturn.job.console.mybatis.service;

import java.util.List;

import com.vip.saturn.job.console.domain.MoveNamespaceBatchStatus;
import com.vip.saturn.job.console.domain.NamespaceZkClusterMappingVo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

/**
 * @author hebelala
 */
public interface NamespaceZkClusterMappingService {

	List<NamespaceZkClusterMappingVo> getNamespaceZkClusterMappingList() throws SaturnJobConsoleException;

	void initNamespaceZkClusterMapping(String createdBy) throws SaturnJobConsoleException;

	List<String> getZkClusterListWithOnlineFromCfg() throws SaturnJobConsoleException;

	void moveNamespaceTo(String namespace, String bootstrapKeyNew, String lastUpdatedBy, boolean updateDBOnly)
			throws SaturnJobConsoleException;

	void moveNamespaceBatchTo(String namespaces, String bootstrapKeyNew, String lastUpdatedBy, boolean updateDBOnly,
			long id) throws SaturnJobConsoleException;

	MoveNamespaceBatchStatus getMoveNamespaceBatchStatus(long id) throws SaturnJobConsoleException;

	void clearMoveNamespaceBatchStatus(long id) throws SaturnJobConsoleException;
}
