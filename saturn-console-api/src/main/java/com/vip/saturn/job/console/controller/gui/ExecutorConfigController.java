package com.vip.saturn.job.console.controller.gui;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import com.vip.saturn.job.console.utils.PermissionKeys;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author hebelala
 */
@RequestMapping("/console/configs/executor")
public class ExecutorConfigController extends AbstractGUIController {

	@Resource
	private SystemConfigService systemConfigService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping
	public SuccessResponseEntity createOrUpdateConfig(@AuditParam(value = "key") @RequestParam String key,
			@AuditParam(value = "value") @RequestParam String value) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.systemConfig);
		String executorConfigsJson = systemConfigService.getValueDirectly(SystemConfigProperties.EXECUTOR_CONFIGS);
		JSONObject jsonObject = null;
		if (StringUtils.isNotBlank(executorConfigsJson)) {
			jsonObject = JSON.parseObject(executorConfigsJson.trim());
		}
		if (jsonObject == null) {
			jsonObject = new JSONObject();
		}
		// I'm sure that key and value is not null, because @RequestParam required is true
		jsonObject.put(key.trim(), value.trim());

		SystemConfig systemConfig = new SystemConfig();
		systemConfig.setProperty(SystemConfigProperties.EXECUTOR_CONFIGS);
		systemConfig.setValue(jsonObject.toJSONString());
		systemConfigService.insertOrUpdate(systemConfig);

		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping
	public SuccessResponseEntity getConfigs() throws IOException, SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.systemConfig);
		String executorConfigsJson = systemConfigService.getValueDirectly(SystemConfigProperties.EXECUTOR_CONFIGS);
		JSONObject jsonObject = null;
		if (StringUtils.isNotBlank(executorConfigsJson)) {
			jsonObject = JSON.parseObject(executorConfigsJson.trim());
		}
		if (jsonObject == null) {
			jsonObject = new JSONObject();
		}
		return new SuccessResponseEntity(jsonObject);
	}

}
