package com.vip.saturn.job.console.marathon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.domain.container.ContainerConfig;
import com.vip.saturn.job.console.domain.container.ContainerStatus;
import com.vip.saturn.job.console.domain.container.ContainerToken;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.ContainerRestService;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author hebelala
 */
@Service("marathonRestAdapter")
public class MarathonRestAdapter implements ContainerRestService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MarathonRestAdapter.class);

	private String getUserName(ContainerToken containerToken) {
		try {
			return containerToken.getKeyValue().get("userName");
		} catch (Exception e) {
			return "";
		}
	}

	private String getPassword(ContainerToken containerToken) {
		try {
			return containerToken.getKeyValue().get("password");
		} catch (Exception e) {
			return "";
		}
	}

	@Override
	public String serializeContainerToken(ContainerToken containerToken) throws SaturnJobConsoleException {
		if (containerToken != null) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("userName", getUserName(containerToken));
			jsonObject.put("password", getPassword(containerToken));
			return jsonObject.toJSONString();
		} else {
			return null;
		}
	}

	@Override
	public ContainerToken deserializeContainerToken(String containerTokenStr) throws SaturnJobConsoleException {
		if (containerTokenStr != null) {
			Map<String, String> keyValue = JSON.parseObject(containerTokenStr,
					new TypeReference<Map<String, String>>() {
					}.getType());
			ContainerToken containerToken = new ContainerToken();
			containerToken.setKeyValue(keyValue);
			return containerToken;
		} else {
			return null;
		}
	}

	@Override
	public void checkContainerTokenNotNull(ContainerToken containerToken) throws SaturnJobConsoleException {
		if (containerToken == null || containerToken.getKeyValue() == null) {
			throw new SaturnJobConsoleException("Please input userName and password");
		}
		if (containerToken.getKeyValue().get("userName") == null) {
			throw new SaturnJobConsoleException("Please input userName");
		}
		if (containerToken.getKeyValue().get("password") == null) {
			throw new SaturnJobConsoleException("Please input password");
		}
	}

	@Override
	public boolean containerTokenEquals(ContainerToken ctNew, ContainerToken ctOld) throws SaturnJobConsoleException {
		if (ctNew == null && ctOld == null) {
			return true;
		} else if (ctNew == null && ctOld != null || ctNew != null && ctOld == null) {
			return false;
		} else if (ctNew.equals(ctOld)) {
			return true;
		} else {
			String unNew = getUserName(ctNew);
			String unOld = getUserName(ctOld);
			String pwNew = getPassword(ctNew);
			String pwOld = getPassword(ctOld);
			return unNew.equals(unOld) && pwNew.equals(pwOld);
		}
	}

	@Override
	public String getContainerScaleJobShardingItemParameters(ContainerToken containerToken, String appId,
			Integer instances) throws SaturnJobConsoleException {
		try {
			String auth = Base64.encodeBase64String(
					(getUserName(containerToken) + ":" + getPassword(containerToken)).getBytes("UTF-8"));
			String url = "";
			if (SaturnEnvProperties.VIP_SATURN_DCOS_REST_URI != null
					&& SaturnEnvProperties.VIP_SATURN_DCOS_REST_URI.endsWith("/")) {
				url = SaturnEnvProperties.VIP_SATURN_DCOS_REST_URI + "v2/apps/" + appId + "?force=true";
			} else {
				url = SaturnEnvProperties.VIP_SATURN_DCOS_REST_URI + "/v2/apps/" + appId + "?force=true";
			}
			return "0=curl -X PUT -H \"Content-Type:application/json\" -H \"Authorization:Basic " + auth
					+ "\" --data '{\"instances\":" + instances + "}' " + url;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		}
	}

	@Override
	public ContainerStatus getContainerStatus(ContainerToken containerToken, String appId)
			throws SaturnJobConsoleException {
		return MarathonRestClient.getContainerStatus(getUserName(containerToken), getPassword(containerToken), appId);
	}

	@Override
	public void deploy(ContainerToken containerToken, ContainerConfig containerConfig)
			throws SaturnJobConsoleException {
		MarathonRestClient.deploy(getUserName(containerToken), getPassword(containerToken), containerConfig);
	}

	@Override
	public void scale(ContainerToken containerToken, String appId, Integer instances) throws SaturnJobConsoleException {
		MarathonRestClient.scale(getUserName(containerToken), getPassword(containerToken), appId, instances);
	}

	@Override
	public void destroy(ContainerToken containerToken, String appId) throws SaturnJobConsoleException {
		MarathonRestClient.destroy(getUserName(containerToken), getPassword(containerToken), appId);
	}

	@Override
	public int count(ContainerToken containerToken, String appId) throws SaturnJobConsoleException {
		return MarathonRestClient.count(getUserName(containerToken), getPassword(containerToken), appId);
	}

	@Override
	public String info(ContainerToken containerToken, String appId) throws SaturnJobConsoleException {
		return MarathonRestClient.info(getUserName(containerToken), getPassword(containerToken), appId);
	}

	@Override
	public String getRegistryCatalog() throws SaturnJobConsoleException {
		return MarathonRestClient.getRegistryCatalog();
	}

	@Override
	public String getRegistryRepositoryTags(String repository) throws SaturnJobConsoleException {
		return MarathonRestClient.getRegistryRepositoriesTagsList(repository);
	}
}
