package com.vip.saturn.job.console.controller.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.JobConfigMetaGroup;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import com.vip.saturn.job.console.service.SystemConfigService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * System config related operations.
 *
 * @author kfchu
 */
@Controller
@RequestMapping("/console/configs")
public class SystemConfigController extends AbstractGUIController {

	private static final String SYSTEM_CONFIG_META_FILE_NAME = "system-config-meta.yaml";

	private static final ObjectMapper YAML_OBJ_MAPPER = new ObjectMapper(new YAMLFactory());

	@Resource
	private SystemConfigService systemConfigService;

	/**
	 * 获取作业配置。可以by单个key或者key的列表。
	 *
	 * @param key 单个key
	 * @param keys key列表
	 * @return 配置值
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping
	public SuccessResponseEntity getConfig(@RequestParam(required = false) String key,
			@RequestParam(required = false) List<String> keys) throws SaturnJobConsoleException {
		if (StringUtils.isBlank(key) && (keys == null || keys.size() == 0)) {
			return new SuccessResponseEntity(systemConfigService.getSystemConfigsDirectly(null));
		}

		if (StringUtils.isNotBlank(key)) {
			return new SuccessResponseEntity(
					systemConfigService.getSystemConfigsDirectly(Lists.newArrayList(key)));
		}

		return new SuccessResponseEntity(systemConfigService.getSystemConfigsDirectly(keys));
	}

	/**
	 * 创建或者更新配置项。
	 * @param key 配置key
	 * @param value 配置值
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping
	public SuccessResponseEntity createOrUpdateConfig(@AuditParam(value = "key") @RequestParam String key,
			@AuditParam(value = "value") @RequestParam String value) throws SaturnJobConsoleException {
		SystemConfig systemConfig = new SystemConfig();
		systemConfig.setProperty(key);
		systemConfig.setValue(value);
		systemConfigService.insertOrUpdate(systemConfig);

		return new SuccessResponseEntity();
	}

	/**
	 * 获取配置分组，描述等信息。
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "meta")
	public SuccessResponseEntity getConfigMeta() throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		File systemConfigMetaFile = new File(classLoader.getResource(SYSTEM_CONFIG_META_FILE_NAME).getFile());
		Map<String, JobConfigMetaGroup> jobConfigGroups = YAML_OBJ_MAPPER.readValue(systemConfigMetaFile, Map.class);
		return new SuccessResponseEntity(jobConfigGroups);
	}


}
