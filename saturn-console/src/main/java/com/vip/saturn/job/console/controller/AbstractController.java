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
import com.vip.saturn.job.console.service.impl.RegistryCenterServiceImpl;
import com.vip.saturn.job.console.utils.ThreadLocalCuratorClient;

/**
 * @author chembo.huang
 *
 */
public class AbstractController {
	
    public static final String ACTIVATED_CONFIG_SESSION_KEY = "activated_config";
    public static final String REQUEST_NAMESPACE_PARAM = "nns";
    public static final String CURRENT_ZK = "current_zk";
    
    @Resource
    protected RegistryCenterService registryCenterService;
    @Resource
    private JobDimensionService jobDimensionService;
    @Resource
    private JobOperationService jobOperationService;
	
	@Value("${console.version}")
	protected String version;
	
	public static String getStackTrace(Throwable aThrowable) {
	    //add the class name and any message passed to constructor
	    final StringBuilder result = new StringBuilder("Trace: ");
	    result.append(aThrowable.toString());
	    final String NEW_LINE = "<br>";
	    result.append(NEW_LINE);

	    //add each element of the stack trace
	    for (StackTraceElement element : aThrowable.getStackTrace()) {
	        result.append(element);
	        result.append(NEW_LINE);
	    }
	    return result.toString();
	}
	
	public void setSession(final RegistryCenterClient client, final HttpSession session) {
		 ThreadLocalCuratorClient.setCuratorClient(client.getCuratorClient());
         RegistryCenterConfiguration conf = registryCenterService.findConfig(client.getNameAndNamespace());
         session.setAttribute(ACTIVATED_CONFIG_SESSION_KEY, conf);
         setCurrentZkAddr(conf.getZkAddressList(), session);
	}
	
	public void setCurrentZkAddr(String zkAddr, final HttpSession session) {
		session.setAttribute(CURRENT_ZK, zkAddr);
	}
	
	public String getCurrentZkAddr(final HttpSession session) {
		String zkAddr = (String)session.getAttribute(CURRENT_ZK);
		if (zkAddr == null) {
			Collection<ZkCluster> zks = RegistryCenterServiceImpl.ZKADDR_TO_ZKCLUSTER_MAP.values();
			for (ZkCluster zkCluster : zks) {
				setCurrentZkAddr(zkCluster.getZkAddr(), session);
				return zkCluster.getZkAddr();
			}
		}
		return zkAddr;
	}
	
	public RegistryCenterConfiguration getActivatedConfigInSession(final HttpSession session) {
		return (RegistryCenterConfiguration) session.getAttribute(ACTIVATED_CONFIG_SESSION_KEY);
	}
	
	public RegistryCenterClient getClientInSession(final HttpSession session) {
		RegistryCenterConfiguration reg = (RegistryCenterConfiguration) session.getAttribute(ACTIVATED_CONFIG_SESSION_KEY);
		if (reg == null) {
			return null;
		}
		return RegistryCenterServiceImpl.getCuratorByNameAndNamespace(reg.getNameAndNamespace());
	}
	
	public void setJobStatusAndIsEnabled(ModelMap model, String jobName) {
		model.put("jobStatus", jobDimensionService.getJobStatus(jobName));
		model.put("isEnabled", jobDimensionService.isJobEnabled(jobName));
	}

	/**
	 * 获取当前session连接的域名
	 */
	public String getNamespace() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		RegistryCenterConfiguration configuration = (RegistryCenterConfiguration) request.getSession().getAttribute(AbstractController.ACTIVATED_CONFIG_SESSION_KEY);
		if (configuration != null) {
			return configuration.getNamespace();
		}
		return null;
	}

}
