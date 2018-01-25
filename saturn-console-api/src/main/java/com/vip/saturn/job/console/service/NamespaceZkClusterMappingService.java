package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.NamespaceMigrationOverallStatus;
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

	void migrateNamespaceToNewZk(String namespace, String zkClusterKeyNew, String lastUpdatedBy, boolean updateDBOnly)
			throws SaturnJobConsoleException;

	void migrateNamespaceListToNewZk(String namespaces, String zkClusterKeyNew, String lastUpdatedBy,
			boolean updateDBOnly)
			throws SaturnJobConsoleException;

	NamespaceMigrationOverallStatus getNamespaceMigrationOverallStatus();

	void clearNamespaceMigrationOverallStatus();

	interface UpdateStatusCallback {

		void update(NamespaceMigrationOverallStatus namespaceMigrationOverallStatus);
	}

}
