package com.vip.saturn.job.console.controller.rest;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.controller.gui.AbstractGUIController;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;
import com.vip.saturn.job.console.mybatis.service.NamespaceZkClusterMapping4SqlService;
import com.vip.saturn.job.console.mybatis.service.ZkClusterInfoService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.utils.SaturnConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * API for executor discover namespace info.<br>
 * Executor &gt;= 3.1.2 will call this API to discover.
 *
 * @author ray.leung
 */
@RequestMapping("/rest/v1/discovery")
public class DiscoveryRestApiController extends AbstractGUIController {

	@Resource
	private SystemConfigService systemConfigService;

	@Resource
	private ZkClusterInfoService zkClusterInfoService;

	@Resource
	private NamespaceZkClusterMapping4SqlService namespaceZkclusterMapping4SqlService;

	/**
	 * 发现namespace相关注册信息.
	 */
	@Audit(type = AuditType.REST)
	@RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity discover(String namespace) throws SaturnJobConsoleException {
		checkMissingParameter("namespace", namespace);
		Map<String, String> response = new HashMap<>(2);
		response.put("zkConnStr", getZkConnStr(namespace));
		response.put("env", getEnvConfig());
		return ResponseEntity.ok(response);
	}

	/**
	 * 返回ZK连接串
	 */
	private String getZkConnStr(String namespace) throws SaturnJobConsoleHttpException {
		if (StringUtils.isBlank(namespace)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
					String.format(MISSING_REQUEST_MSG, "namespace"));
		}
		String zkClusterKey = namespaceZkclusterMapping4SqlService.getZkClusterKey(namespace);

		if (zkClusterKey == null) {
			throw new SaturnJobConsoleHttpException(HttpStatus.NOT_FOUND.value(), "The namespace：[" + namespace + "] is not registered in Saturn.");
		}
		ZkClusterInfo zkClusterInfo = zkClusterInfoService.getByClusterKey(zkClusterKey);
		if (zkClusterInfo == null) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					"The clusterKey: [" + zkClusterKey + "] is not configured in db for " + namespace);
		}
		return zkClusterInfo.getConnectString();
	}

	/**
	 * 返回console所在环境信息
	 */
	private String getEnvConfig() {
		return systemConfigService.getValueDirectly(SaturnConstants.SYSTEM_CONFIG_ENV);
	}
}
