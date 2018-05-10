package com.vip.saturn.job.console.controller.gui;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import com.vip.saturn.job.console.utils.PermissionKeys;
import com.vip.saturn.job.console.utils.SaturnSelfNodePath;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.*;

/**
 * Controller for manage the global config of executor.
 *
 * @author hebelala
 */
@RequestMapping("/console/configs/executor")
public class ExecutorConfigController extends AbstractGUIController {

	private static final Logger log = LoggerFactory.getLogger(ExecutorConfigController.class);

	@Resource
	private SystemConfigService systemConfigService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping
	public SuccessResponseEntity createOrUpdateConfig(@AuditParam(value = "key") @RequestParam String key,
			@AuditParam(value = "value") @RequestParam String value) throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.systemConfig);

		String executorConfigsJsonInDB = systemConfigService.getValueDirectly(SystemConfigProperties.EXECUTOR_CONFIGS);
		JSONObject jsonObject = parseExecutorConfigJson(executorConfigsJsonInDB);
		if (jsonObject == null) {
			jsonObject = new JSONObject();
		}

		// I'm sure that key and value is not null, because @RequestParam required is true
		jsonObject.put(key.trim(), value.trim());

		String configStr = jsonObject.toJSONString();
		log.info("Start to update executor config data {}", configStr);
		// update zk
		List<ZkCluster> onlineZkClusterList = registryCenterService.getOnlineZkClusterList();
		for (ZkCluster zkCluster : onlineZkClusterList) {
			updateConfigInZk(configStr, zkCluster);
		}

		// update db
		SystemConfig systemConfig = new SystemConfig();
		systemConfig.setProperty(SystemConfigProperties.EXECUTOR_CONFIGS);
		systemConfig.setValue(configStr);
		systemConfigService.insertOrUpdate(systemConfig);
		log.info("Update executor config to db successfully");

		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping
	public SuccessResponseEntity getConfigs() throws SaturnJobConsoleException {
		assertIsPermitted(PermissionKeys.systemConfig);

		String executorConfigsJsonInDB = systemConfigService.getValueDirectly(SystemConfigProperties.EXECUTOR_CONFIGS);
		JSONObject jsonObject = parseExecutorConfigJson(executorConfigsJsonInDB);
		if (jsonObject == null) {
			return new SuccessResponseEntity(Lists.newArrayList());
		}

		List<Map<String, String>> result = new ArrayList<>();
		Set<String> keys = jsonObject.keySet();
		for (String key : keys) {
			Map<String, String> element = new HashMap<>();
			element.put("key", key);
			element.put("value", jsonObject.getString(key));
			result.add(element);
		}
		return new SuccessResponseEntity(result);
	}

	/**
	 * Parse executor config json string, if parsing fail or json string is blank then return null;
	 */
	private JSONObject parseExecutorConfigJson(String jsonStr) {
		if (StringUtils.isBlank(jsonStr)) {
			return null;
		}

		try {
			return JSON.parseObject(jsonStr.trim());
		} catch (Throwable t) {
			log.error("Fail to parse data from json string" + jsonStr, t);
			return null;
		}
	}

	private void updateConfigInZk(String configStr, ZkCluster zkCluster) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = zkCluster.getCuratorFrameworkOp();
		if (curatorFrameworkOp == null) {
			return;
		}
		// 对比数据，如果不相等，则更新
		String dataInZk = curatorFrameworkOp.getData(SaturnSelfNodePath.SATURN_EXECUTOR_CONFIG);
		if (configStr.equals(dataInZk)) {
			return;
		}

		curatorFrameworkOp.update(SaturnSelfNodePath.SATURN_EXECUTOR_CONFIG, configStr);
		log.info("Update executor config to zkcluster {} successfully", zkCluster.getZkClusterKey());
	}
}
