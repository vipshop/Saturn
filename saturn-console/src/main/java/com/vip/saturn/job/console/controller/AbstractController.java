/**
 * 
 */
package com.vip.saturn.job.console.controller;

import java.util.Collection;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.service.JobOperationService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.SessionAttributeKeys;
import com.vip.saturn.job.console.utils.ThreadLocalCuratorClient;

/**
 * @author chembo.huang
 *
 */
public class AbstractController {

	public static final String REQUEST_NAMESPACE_PARAM = "nns";

	@Resource
	protected RegistryCenterService registryCenterService;
	@Resource
	private JobDimensionService jobDimensionService;
	@Resource
	private JobOperationService jobOperationService;

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

	public void setJobStatusAndIsEnabled(ModelMap model, String jobName) {
		model.put("jobStatus", jobDimensionService.getJobStatus(jobName));
		model.put("isEnabled", jobDimensionService.isJobEnabled(jobName));
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
