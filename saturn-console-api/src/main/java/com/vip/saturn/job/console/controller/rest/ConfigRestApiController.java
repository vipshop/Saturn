package com.vip.saturn.job.console.controller.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.domain.JobConfigMeta;
import com.vip.saturn.job.console.domain.SystemConfigVo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

/**
 * System config related operations.
 *
 * @author kfchu
 */
@RequestMapping("/rest/v1/config")
public class ConfigRestApiController extends AbstractRestController {

	protected static final ObjectMapper YAML_OBJ_MAPPER = new ObjectMapper(new YAMLFactory());

	@Resource
	private SystemConfigService systemConfigService;

	@Value(value = "classpath:system-config-meta.yaml")
	private org.springframework.core.io.Resource configYaml;

	/**
	 * 获取所有系统配置信息。
	 */
	@Audit(type = AuditType.REST)
	@RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity getConfigs() throws IOException, SaturnJobConsoleException {
		//获取配置meta
		Map<String, List<JobConfigMeta>> jobConfigGroups = getSystemConfigMeta();
		//返回所有配置信息
		List<SystemConfig> systemConfigs = systemConfigService.getSystemConfigsDirectly(null);
		//剔除EXECUTOR_CONFIGS
		removeExecutorConfigs(systemConfigs);

		return ResponseEntity.ok(genSystemConfigInfo(jobConfigGroups, systemConfigs));
	}

	/**
	 * 移除Executor全局配置，该配置在单独的页面管理
	 * @param systemConfigs 全量的系统配置数据
	 */
	private void removeExecutorConfigs(List<SystemConfig> systemConfigs) {
		if (systemConfigs == null) {
			return;
		}
		Iterator<SystemConfig> iterator = systemConfigs.iterator();
		while (iterator.hasNext()) {
			SystemConfig systemConfig = iterator.next();
			if (SystemConfigProperties.EXECUTOR_CONFIGS.equals(systemConfig.getProperty())) {
				iterator.remove();
			}
		}
	}

	/**
	 * 根据Yaml定义，返回作业配置。
	 *
	 * @param jobConfigGroups 来自yaml定义的配置展现分类定义
	 * @param systemConfigs 数据库的系统配置项
	 * @return 配置分组与配置信息map
	 */
	private Map<String, List<SystemConfigVo>> genSystemConfigInfo(Map<String, List<JobConfigMeta>> jobConfigGroups,
			List<SystemConfig> systemConfigs) {
		Map<String, SystemConfig> systemConfigMap = convertList2Map(systemConfigs);
		Map<String, List<SystemConfigVo>> jobConfigDisplayInfoMap = Maps.newHashMap();
		Set<String> categorizedConfigKeySet = Sets.newHashSet();

		for (Map.Entry<String, List<JobConfigMeta>> group : jobConfigGroups.entrySet()) {
			List<JobConfigMeta> jobConfigMetas = group.getValue();
			List<SystemConfigVo> jobConfigVos = Lists.newArrayListWithCapacity(jobConfigMetas.size());
			for (JobConfigMeta configMeta : jobConfigMetas) {
				String configName = configMeta.getName();
				SystemConfig systemConfig = systemConfigMap.get(configName);
				String value = systemConfig != null ? systemConfig.getValue() : null;
				jobConfigVos.add(new SystemConfigVo(configName, value, configMeta.getDesc_zh()));
				categorizedConfigKeySet.add(configName);
			}

			jobConfigDisplayInfoMap.put(group.getKey(), jobConfigVos);
		}

		// 将所有没有在yaml定义的配置放到Others组别
		if (categorizedConfigKeySet.size() != systemConfigs.size()) {
			List<SystemConfigVo> unCategorizedJobConfigVos = getUncategorizedSystemConfigs(systemConfigs,
					categorizedConfigKeySet);
			jobConfigDisplayInfoMap.put("other_configs", unCategorizedJobConfigVos);
		}

		return jobConfigDisplayInfoMap;
	}

	private List<SystemConfigVo> getUncategorizedSystemConfigs(List<SystemConfig> systemConfigList,
			Set<String> configKeySet) {
		List<SystemConfigVo> unCategorizedJobConfigVos = Lists.newArrayList();
		for (SystemConfig systemConfig : systemConfigList) {
			String property = systemConfig.getProperty();
			if (configKeySet.contains(property)) {
				continue;
			}
			unCategorizedJobConfigVos.add(new SystemConfigVo(property, systemConfig.getValue(), ""));
		}

		return unCategorizedJobConfigVos;
	}

	protected Map<String, List<JobConfigMeta>> getSystemConfigMeta() throws IOException {
		TypeReference<HashMap<String, List<JobConfigMeta>>> typeRef = new TypeReference<HashMap<String, List<JobConfigMeta>>>() {
		};

		return YAML_OBJ_MAPPER.readValue(configYaml.getInputStream(), typeRef);
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

	public SystemConfigService getSystemConfigService() {
		return systemConfigService;
	}
}
