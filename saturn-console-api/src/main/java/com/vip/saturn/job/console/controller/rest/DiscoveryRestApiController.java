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
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * System config related operations.
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
	 * 获取所有系统配置信息。
	 */
	@Audit(type = AuditType.REST)
	@RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity getConfigs(HttpServletRequest request) throws SaturnJobConsoleException {
		String namespace = request.getParameter("namespace");
		Map<String, String> response = new HashMap<>(2);
		if (StringUtils.isBlank(namespace)) {
			response.put("env", getVmsEnv());
		} else {
			response.put("env", getVmsEnv());
			response.put("zkConnStr", getZkConnStr(namespace));
		}
		return ResponseEntity.ok(response);
	}

	private String getZkConnStr(String namespace) throws SaturnJobConsoleHttpException {
		if (StringUtils.isBlank(namespace)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
					String.format(MISSING_REQUEST_MSG, "namespace"));
		}
		String zkClusterKey = namespaceZkclusterMapping4SqlService.getZkClusterKey(namespace);

		if (zkClusterKey == null) {
			throw new SaturnJobConsoleHttpException(HttpStatus.NOT_FOUND.value(),
					"The namespace：[" + namespace + "] is not registered in Saturn.");
		}
		ZkClusterInfo zkClusterInfo = zkClusterInfoService.getByClusterKey(zkClusterKey);
		if (zkClusterInfo == null) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					"The clusterKey: [" + zkClusterKey + "] is not configured in db for " + namespace);
		}
		return zkClusterInfo.getConnectString();
	}

	private String getVmsEnv() {
		return systemConfigService.getValueDirectly("ENV");
	}
}
