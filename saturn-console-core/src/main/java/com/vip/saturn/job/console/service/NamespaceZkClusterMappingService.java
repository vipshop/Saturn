package com.vip.saturn.job.console.service;

import java.util.List;

import com.vip.saturn.job.console.domain.MoveNamespaceBatchStatus;
import com.vip.saturn.job.console.domain.NamespaceZkClusterMappingVo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

/**
 * @author hebelala
 */
public interface NamespaceZkClusterMappingService {

	interface UpdateStatusCallback {
		void update(MoveNamespaceBatchStatus moveNamespaceBatchStatus);
	}

	List<NamespaceZkClusterMappingVo> getNamespaceZkClusterMappingList() throws SaturnJobConsoleException;

	void initNamespaceZkClusterMapping(String createdBy) throws SaturnJobConsoleException;

	List<String> getZkClusterListWithOnline() throws SaturnJobConsoleException;

	void moveNamespaceTo(String namespace, String zkClusterKeyNew, String lastUpdatedBy, boolean updateDBOnly)
			throws SaturnJobConsoleException;

	void moveNamespaceBatchTo(String namespaces, String zkClusterKeyNew, String lastUpdatedBy, boolean updateDBOnly) throws SaturnJobConsoleException;
	
	MoveNamespaceBatchStatus getMoveNamespaceBatchStatus();
	
	void clearMoveNamespaceBatchStatus();

}
