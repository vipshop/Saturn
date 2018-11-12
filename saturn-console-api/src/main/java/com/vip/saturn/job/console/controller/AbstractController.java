package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.SessionAttributeKeys;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Collection;
import java.util.Map;

/**
 * @author chembo.huang
 */
public class AbstractController {

	public static final String BAD_REQ_MSG_PREFIX = "Invalid request.";

	public static final String INVALID_REQUEST_MSG = BAD_REQ_MSG_PREFIX + " Parameter: {%s} %s";

	public static final String MISSING_REQUEST_MSG = BAD_REQ_MSG_PREFIX + " Missing parameter: {%s}";

	@Resource
	protected RegistryCenterService registryCenterService;

	@Value("${console.version}")
	protected String version;

	public static String checkAndGetParametersValueAsString(Map<String, Object> map, String key, boolean isMandatory)
			throws SaturnJobConsoleException {
		if (map.containsKey(key)) {
			String value = (String) map.get(key);
			return StringUtils.isBlank(value) ? null : value;
		} else {
			if (isMandatory) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, key));
			}
			return null;
		}
	}

	public static Integer checkAndGetParametersValueAsInteger(Map<String, Object> map, String key, boolean isMandatory)
			throws SaturnJobConsoleException {
		if (map.containsKey(key)) {
			return (Integer) map.get(key);
		} else {
			if (isMandatory) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, key));
			}
			return null;
		}
	}

	public static Boolean checkAndGetParametersValueAsBoolean(Map<String, Object> map, String key, boolean isMandatory)
			throws SaturnJobConsoleException {
		if (map.containsKey(key)) {
			return (Boolean) map.get(key);
		} else {
			if (isMandatory) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, key));
			}
			return null;
		}
	}

	public static void checkMissingParameter(String name, String value) throws SaturnJobConsoleException {
		if (StringUtils.isBlank(value)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
					String.format(MISSING_REQUEST_MSG, name));
		}
	}

	/**
	 * @deprecated session do not store zk cluster information
	 */
	@Deprecated
	public void setCurrentZkClusterKey(String zkClusterKey, final HttpSession session) {
		session.setAttribute(SessionAttributeKeys.CURRENT_ZK_CLUSTER_KEY, zkClusterKey);
	}

	/**
	 * @deprecated session do not store zk cluster information
	 */
	@Deprecated
	public String getCurrentZkAddr(final HttpSession session) {
		String zkClusterKey = (String) session.getAttribute(SessionAttributeKeys.CURRENT_ZK_CLUSTER_KEY);
		if (zkClusterKey != null) {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				return zkCluster.getZkAddr();
			}
		}
		// if zkClusterKey doesn't exist in map, use the first online one in map.
		Collection<ZkCluster> zks = registryCenterService.getZkClusterList();
		for (ZkCluster tmp : zks) {
			ZkCluster zkCluster = tmp;
			if (!zkCluster.isOffline()) {
				setCurrentZkClusterKey(zkCluster.getZkClusterKey(), session);
				return zkCluster.getZkClusterKey();
			}
		}
		return null;
	}

}
