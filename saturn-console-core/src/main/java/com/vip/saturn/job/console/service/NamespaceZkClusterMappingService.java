package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.MoveNamespaceBatchStatus;
import com.vip.saturn.job.console.domain.NamespaceZkClusterMappingVo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

import java.util.List;

/**
 * @author hebelala
 */
public interface NamespaceZkClusterMappingService {

	List<NamespaceZkClusterMappingVo> getNamespaceZkClusterMappingList() throws SaturnJobConsoleException;

	void initNamespaceZkClusterMapping(String createdBy) throws SaturnJobConsoleException;

	List<String> getZkClusterListWithOnline() throws SaturnJobConsoleException;

	void moveNamespaceTo(String namespace, String zkClusterKeyNew, String lastUpdatedBy, boolean updateDBOnly)
			throws SaturnJobConsoleException;

	MoveNamespaceBatchStatus moveNamespaceBatchTo(String namespaces, String zkClusterKeyNew, String lastUpdatedBy,
			boolean updateDBOnly) throws SaturnJobConsoleException;

}
