package com.vip.saturn.job.console.controller.gui;

import com.google.common.collect.Lists;
import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.NamespaceZkClusterMappingService;
import com.vip.saturn.job.console.utils.PermissionKeys;
import com.vip.saturn.job.console.utils.SaturnConsoleUtils;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Collection;
import java.util.List;

@RequestMapping("/console")
public class RegistryCenterController extends AbstractGUIController {

	private static final String EXPORT_FILE_NAME = "namespace_info.xls";

	@Resource
	private NamespaceZkClusterMappingService namespaceZkClusterMappingService;

	/**
	 * 创建域
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/namespaces")
	public SuccessResponseEntity createNamespace(@AuditParam("namespace") @RequestParam String namespace,
			@AuditParam("zkClusterKey") @RequestParam String zkClusterKey) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.registryCenterAddNamespace);
		NamespaceDomainInfo namespaceInfo = constructNamespaceDomainInfo(namespace, zkClusterKey);
		registryCenterService.createNamespace(namespaceInfo);
		return new SuccessResponseEntity();
	}

	private NamespaceDomainInfo constructNamespaceDomainInfo(String namespace, String zkClusterKey) {
		NamespaceDomainInfo namespaceInfo = new NamespaceDomainInfo();
		namespaceInfo.setNamespace(namespace);
		namespaceInfo.setZkCluster(zkClusterKey);
		namespaceInfo.setContent("");
		return namespaceInfo;
	}

	/**
	 * 获取所有域列表
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/namespaces/detail")
	public SuccessResponseEntity queryAllNamespaceInfo() {
		List<RegistryCenterConfiguration> namespaceInfoList = Lists.newLinkedList();
		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
			if (!zkCluster.isOffline()) {
				namespaceInfoList.addAll(zkCluster.getRegCenterConfList());
			} else {
				List<RegistryCenterConfiguration> configs = registryCenterService.findConfigsByZkCluster(zkCluster);
				if (configs != null && !configs.isEmpty()) {
					namespaceInfoList.addAll(configs);
				}
			}
		}

		return new SuccessResponseEntity(namespaceInfoList);
	}

	/**
	 * 导出指定的namespce
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/namespaces/export")
	public void exportNamespaceInfo(@RequestParam(required = false) List<String> namespaceList,
			final HttpServletResponse response) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.registryCenterExportNamespaces);
		File exportFile = registryCenterService.exportNamespaceInfo(namespaceList);
		SaturnConsoleUtils.exportFile(response, exportFile, EXPORT_FILE_NAME, true);
	}

	/**
	 * 刷新注册中心
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@RequestMapping(value = "/registryCenter/refresh", method = {RequestMethod.GET, RequestMethod.POST})
	public SuccessResponseEntity notifyRefreshRegCenter() throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.registryCenterAddNamespace);
		registryCenterService.notifyRefreshRegCenter();
		return new SuccessResponseEntity();
	}

	private List<ZkCluster> filterOnlineZkClusters(Collection<ZkCluster> zkClusters) {
		if (zkClusters == null) {
			return Lists.newLinkedList();
		}

		List<ZkCluster> onlineZkClusters = Lists.newLinkedList();
		for (ZkCluster zkCluster : zkClusters) {
			if (!zkCluster.isOffline()) {
				onlineZkClusters.add(zkCluster);
			}
		}

		return onlineZkClusters;
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/zkClusters")
	public SuccessResponseEntity createZkCluster(@AuditParam("zkClusterKey") @RequestParam String zkClusterKey,
			@AuditParam("alias") @RequestParam String alias,
			@AuditParam("connectString") @RequestParam String connectString) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.registryCenterAddZkCluster);
		registryCenterService.createZkCluster(zkClusterKey, alias, connectString.trim());
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PutMapping(value = "/zkClusters")
	public SuccessResponseEntity updateZkCluster(@AuditParam("zkClusterKey") @RequestParam String zkClusterKey,
			@AuditParam("newConnectString") @RequestParam String connectString) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.registryCenterAddZkCluster);
		registryCenterService.updateZkCluster(zkClusterKey, connectString.trim());
		return new SuccessResponseEntity();
	}

	/**
	 * 获取单个或所有zk集群信息
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters")
	public SuccessResponseEntity getZkClusters(@RequestParam(required = false) String status,
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleException {
		if (StringUtils.isNotBlank(zkClusterKey)) {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster == null) {
				throw new SaturnJobConsoleException(SaturnJobConsoleException.ERROR_CODE_NOT_EXISTED,
						String.format("ZK cluster[%s]不存在", zkClusterKey));
			}
			return new SuccessResponseEntity(zkCluster);
		}

		Collection<ZkCluster> zkClusters = registryCenterService.getZkClusterList();
		if (StringUtils.isBlank(status) || !"online".equals(status)) {
			return new SuccessResponseEntity(zkClusters);
		}

		List<ZkCluster> onlineZkCluster = filterOnlineZkClusters(zkClusters);
		return new SuccessResponseEntity(onlineZkCluster);
	}

	// 域迁移
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/namespaces/zkCluster/migrate")
	public SuccessResponseEntity migrateZkCluster(@AuditParam("namespaces") @RequestParam String namespaces,
			@AuditParam("zkClusterNew") @RequestParam String zkClusterKeyNew,
			@RequestParam(required = false, defaultValue = "false") boolean updateDBOnly)
			throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.registryCenterBatchMoveNamespaces);
		namespaceZkClusterMappingService
				.migrateNamespaceListToNewZk(namespaces, zkClusterKeyNew, getCurrentLoginUserName(), updateDBOnly);
		return new SuccessResponseEntity();
	}

	// 获取域迁移信息
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/namespaces/zkCluster/migrationStatus")
	public SuccessResponseEntity getZkClusterMigrationStatus() throws SaturnJobConsoleException {
		NamespaceMigrationOverallStatus namespaceMigrationOverallStatus = namespaceZkClusterMappingService
				.getNamespaceMigrationOverallStatus();
		if (namespaceMigrationOverallStatus == null) {
			throw new SaturnJobConsoleException("The namespace migration status is not existed in db");
		}
		return new SuccessResponseEntity(namespaceMigrationOverallStatus);
	}

}
