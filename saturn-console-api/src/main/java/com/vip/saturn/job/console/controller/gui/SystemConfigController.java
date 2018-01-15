package com.vip.saturn.job.console.controller.gui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.JobConfigMeta;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.SystemConfigVo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import com.vip.saturn.job.console.service.SystemConfigService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
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
	 * 创建或者更新配置项。
	 * @param key 配置key
	 * @param value 配置值
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit(name = "createOrUpdateSystemConfig")
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
	 * 获取所有系统配置信息。
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping
	public SuccessResponseEntity getConfigs() throws IOException, SaturnJobConsoleException {
		//获取配置meta
		Map<String, List<JobConfigMeta>> jobConfigGroups = getSystemConfigMeta();
		//返回所有配置信息
		List<SystemConfig> systemConfigs = systemConfigService.getSystemConfigsDirectly(null);

		return new SuccessResponseEntity(genSystemConfigInfo(jobConfigGroups, systemConfigs));
	}

	private Map<String, List<SystemConfigVo>> genSystemConfigInfo(Map<String, List<JobConfigMeta>> jobConfigGroups,
			List<SystemConfig> systemConfigs) {
		Map<String, SystemConfig> systemConfigMap = convertList2Map(systemConfigs);
		Map<String, List<SystemConfigVo>> jobConfigDisplayInfoMap = Maps.newHashMap();
		for (String group : jobConfigGroups.keySet()) {
			List<JobConfigMeta> jobConfigMetas = jobConfigGroups.get(group);
			List<SystemConfigVo> jobConfigVos = Lists.newArrayListWithCapacity(jobConfigMetas.size());
			for (JobConfigMeta configMeta : jobConfigMetas) {
				SystemConfig systemConfig = systemConfigMap.get(configMeta.getName());
				String value = systemConfig != null ? systemConfig.getValue() : null;
				jobConfigVos.add(new SystemConfigVo(configMeta.getName(), value, configMeta.getDesc_zh()));
			}

			jobConfigDisplayInfoMap.put(group, jobConfigVos);
		}
		return jobConfigDisplayInfoMap;
	}

	private Map<String, List<JobConfigMeta>> getSystemConfigMeta() throws IOException {
		ClassLoader classLoader = getClass().getClassLoader();
		File systemConfigMetaFile = new File(classLoader.getResource(SYSTEM_CONFIG_META_FILE_NAME).getFile());
		TypeReference<HashMap<String, List<JobConfigMeta>>> typeRef
				= new TypeReference<HashMap<String, List<JobConfigMeta>>>() {
		};

		return YAML_OBJ_MAPPER.readValue(systemConfigMetaFile, typeRef);
	}

	Map<String, SystemConfig> convertList2Map(List<SystemConfig> configList) {
		Map<String, SystemConfig> configMap = Maps.newHashMap();
		for (SystemConfig config : configList) {
			if (configMap.containsKey(config.getProperty())) {
				continue;
			}
			configMap.put(config.getProperty(), config);
		}

		return configMap;
	}


}
