package com.vip.saturn.job.console.controller.rest;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.controller.gui.ConsoleConfigController;
import com.vip.saturn.job.console.domain.JobConfigMeta;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import com.vip.saturn.job.console.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * System config related operations.
 *
 * @author kfchu
 */
@RequestMapping("/rest/v1/discovery")
public class DiscoveryRestApiController extends AbstractRestController {

	@Resource
	private SystemConfigService systemConfigService;

	@Autowired
	private ConsoleConfigController consoleConfigController;

	/**
	 * 获取所有系统配置信息。
	 */
	@Audit(type = AuditType.REST)
	@RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity getConfigs() throws IOException, SaturnJobConsoleException {
		//获取配置meta
		Map<String, List<JobConfigMeta>> jobConfigs = consoleConfigController.getSystemConfigMeta();
		//返回所有配置信息
		List<SystemConfig> systemConfigs = systemConfigService.getSystemConfigsDirectly(null);
		//剔除EXECUTOR_CONFIGS
		consoleConfigController.removeExecutorConfigs(systemConfigs);
		return ResponseEntity.ok(consoleConfigController.genSystemConfigInfo(jobConfigs, systemConfigs));
	}
}
