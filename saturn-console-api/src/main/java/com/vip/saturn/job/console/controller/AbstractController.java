package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.SessionAttributeKeys;
import com.vip.saturn.job.console.utils.ThreadLocalCuratorClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collection;
import java.util.Map;

/**
 * //TODO 删除session中存储的域、集群等信息，这些信息从每个请求中获取
 * @author chembo.huang
 */
public class AbstractController {

	public static final String REQUEST_NAMESPACE_PARAM = "nns";

	public static final String BAD_REQ_MSG_PREFIX = "Invalid request.";

	public static final String INVALID_REQUEST_MSG = BAD_REQ_MSG_PREFIX + " Parameter: {%s} %s";

	public static final String MISSING_REQUEST_MSG = BAD_REQ_MSG_PREFIX + " Missing parameter: {%s}";

	@Resource
	protected RegistryCenterService registryCenterService;

	@Value("${console.version}")
	protected String version;

	public static String getStackTrace(Throwable aThrowable) {
		// add the class name and any message passed to constructor
		final StringBuilder result = new StringBuilder("Trace: ");
		result.append(aThrowable.toString());
		final String NEW_LINE = "<br>";
		result.append(NEW_LINE);

		// add each element of the stack trace
		for (StackTraceElement element : aThrowable.getStackTrace()) {
			result.append(element);
			result.append(NEW_LINE);
		}
		return result.toString();
	}

	public static String getClientIp(HttpServletRequest request) {
		String remoteAddr = "";

		if (request != null) {
			remoteAddr = request.getHeader("X-FORWARDED-FOR");
			if (remoteAddr == null || "".equals(remoteAddr)) {
				remoteAddr = request.getRemoteAddr();
			}
		}

		return remoteAddr;
	}

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

	public void setSession(final RegistryCenterClient client, final HttpSession session) {
		ThreadLocalCuratorClient.setCuratorClient(client.getCuratorClient());
		RegistryCenterConfiguration conf = registryCenterService.findConfig(client.getNameAndNamespace());
		if (conf == null) {
			return;
		}
		session.setAttribute(SessionAttributeKeys.ACTIVATED_CONFIG_SESSION_KEY, conf);
		setCurrentZkClusterKey(conf.getZkClusterKey(), session);
	}

	public void setCurrentZkClusterKey(String zkClusterKey, final HttpSession session) {
		session.setAttribute(SessionAttributeKeys.CURRENT_ZK_CLUSTER_KEY, zkClusterKey);
	}

	public String getCurrentZkClusterKey(final HttpSession session) {
		String zkClusterKey = (String) session.getAttribute(SessionAttributeKeys.CURRENT_ZK_CLUSTER_KEY);
		if (zkClusterKey == null) {
			// if zkKey doesn't exist in map, use the first online one in map.
			Collection<ZkCluster> zks = registryCenterService.getZkClusterList();
			for (ZkCluster tmp : zks) {
				ZkCluster zkCluster = tmp;
				if (!zkCluster.isOffline()) {
					setCurrentZkClusterKey(zkCluster.getZkClusterKey(), session);
					return zkCluster.getZkClusterKey();
				}
			}
		}
		return zkClusterKey;
	}

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

	public RegistryCenterConfiguration getActivatedConfigInSession(final HttpSession session) {
		return (RegistryCenterConfiguration) session.getAttribute(SessionAttributeKeys.ACTIVATED_CONFIG_SESSION_KEY);
	}

	public RegistryCenterClient getClientInSession(final HttpSession session) {
		RegistryCenterConfiguration reg = (RegistryCenterConfiguration) session
				.getAttribute(SessionAttributeKeys.ACTIVATED_CONFIG_SESSION_KEY);
		if (reg == null) {
			return null;
		}
		return registryCenterService.getCuratorByNameAndNamespace(reg.getNameAndNamespace());
	}

	public String getNamespace() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.getRequest();
		RegistryCenterConfiguration configuration = (RegistryCenterConfiguration) request.getSession()
				.getAttribute(SessionAttributeKeys.ACTIVATED_CONFIG_SESSION_KEY);
		if (configuration != null) {
			return configuration.getNamespace();
		}
		return null;
	}

}
