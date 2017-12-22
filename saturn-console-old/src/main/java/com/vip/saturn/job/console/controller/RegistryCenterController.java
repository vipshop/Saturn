/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.NamespaceVersionMapping;
import com.vip.saturn.job.console.mybatis.entity.ReleaseVersionInfo;
import com.vip.saturn.job.console.mybatis.service.NamespaceVersionMappingService;
import com.vip.saturn.job.console.mybatis.service.ReleaseVersionInfoService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.service.InitRegistryCenterService;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.utils.ThreadLocalCuratorClient;

@RestController
@RequestMapping("registry_center")
public class RegistryCenterController extends AbstractController {

	protected static Logger LOGGER = LoggerFactory.getLogger(RegistryCenterController.class);

	public static final String ZK_CONFIG_KEY = "zk_config_key";

	public static final String TITLE_FORMAT = "<a href='job_detail?jobName=%s&nns=%s' target='contentFrame' title='%s'>%s</a>";

	public static final String SHELL_ICON_CLASS = "devicon devicon-linux-plain";
	public static final String JAVA_ICON_CLASS = "devicon devicon-java-plain";
	public static final String MSG_ICON_CLASS = "fa fa-reorder";

	@Resource
	private JobDimensionService jobDimensionService;

	@Resource
	private ReleaseVersionInfoService releaseVersionInfoService;

	@Resource
	private NamespaceVersionMappingService namespaceVersionMappingService;

	@RequestMapping(value = "getNamespaceInfo", method = RequestMethod.GET)
	public RequestResult getNamespaceInfo(final HttpSession session) {
		RequestResult requestResult = new RequestResult();
		try {
			Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
			requestResult.setObj(zkClusterList);
			requestResult.setSuccess(true);
		} catch (Exception e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "refreshRegCenter", method = RequestMethod.GET)
	public RequestResult refreshRegCenter(final String nameAndNamespace, final HttpSession session,
			final HttpServletRequest request) {
		return registryCenterService.refreshRegCenter();
	}

	@RequestMapping(value = "connect", method = RequestMethod.POST)
	public Map<String, Object> connect(final String nameAndNamespace, final HttpSession session) {
		RegistryCenterClient client = registryCenterService.connect(nameAndNamespace);
		Map<String, Object> model = new HashMap<String, Object>();
		setSession(client, session);
		model.put("isSuccesssful", client.isConnected());
		if (client.isConnected()) {
			model.put("namespace", client.getCuratorClient().getNamespace());
		}
		return model;
	}

	private String replaceQuotation(String str) {
		if (str == null) {
			return "";
		} else {
			return str.replaceAll("\'", "&#39;").replaceAll("\"", "&#34;");
		}
	}

	private List<TreeNode> connectAndLoadJobs(HttpServletRequest request, String nns, final HttpSession session) {
		setSession(registryCenterService.connect(nns), session);
		// set threadlocal session.
		RegistryCenterClient client = getClientInSession(session);
		if (null == client || !client.isConnected()) {
			return null;
		}
		ThreadLocalCuratorClient.setCuratorClient(client.getCuratorClient());
		Collection<JobBriefInfo> jobs = jobDimensionService.getAllJobsBriefInfo(null, null);
		List<TreeNode> nodes = new ArrayList<>();
		for (Iterator<JobBriefInfo> iterator = jobs.iterator(); iterator.hasNext();) {
			JobBriefInfo jobBriefInfo = (JobBriefInfo) iterator.next();
			TreeNode node = new TreeNode(String.format(TITLE_FORMAT, jobBriefInfo.getJobName(), nns,
					replaceQuotation(jobBriefInfo.getDescription()), jobBriefInfo.getJobName()));
			node.setFolder(false);
			switch (jobBriefInfo.getJobType()) {
			case MSG_JOB:
				node.setIcon(MSG_ICON_CLASS);
				break;
			case JAVA_JOB:
				node.setIcon(JAVA_ICON_CLASS);
				break;
			case SHELL_JOB:
				node.setIcon(SHELL_ICON_CLASS);
			default:
				break;
			}
			nodes.add(node);
		}
		return nodes;
	}

	@RequestMapping(value = "loadTree", method = RequestMethod.GET)
	public List<TreeNode> loadTree(HttpServletRequest request, Integer id, String fp, final HttpSession session)
			throws ExecutionException {
		if (!Strings.isNullOrEmpty(fp)) {
			return connectAndLoadJobs(request, fp, session);
		}
		TreeNode resultNode = InitRegistryCenterService.getDomainRootTreeNode();
		return resultNode.getChildren();
	}

	@RequestMapping(value = "jobs", method = RequestMethod.GET)
	public Collection<JobBriefInfo> getAllJobsBriefInfo() {
		return jobDimensionService.getAllJobsBriefInfo(null, null);
	}

	@RequestMapping(value = "updateNamespaceVersionMapping", method = {RequestMethod.POST, RequestMethod.GET})
	public RequestResult updateNamespaceVersionMapping(HttpServletRequest request, String namespace, String versionNumber, Boolean isForced) {
		RequestResult requestResult = new RequestResult();
		if(StringUtils.isBlank(namespace)) {
			requestResult.setSuccess(false);
			requestResult.setMessage("the namespace is required");
			return requestResult;
		}
		if(StringUtils.isBlank(versionNumber)) {
			requestResult.setSuccess(false);
			requestResult.setMessage("the versionNumber is required");
			return requestResult;
		}
		if(isForced == null) {
			requestResult.setSuccess(false);
			requestResult.setMessage("the isForced is required");
			return requestResult;
		}
		try {
			ReleaseVersionInfo releaseVersionInfo = releaseVersionInfoService.selectByVersionNumber(versionNumber);
			if (releaseVersionInfo == null) {
				throw new Exception("the versionNumber " + versionNumber + " does not exist");
			}
			namespaceVersionMappingService.insertOrUpdate(namespace, versionNumber, isForced, "");
			requestResult.setSuccess(true);
		} catch (Throwable t) {
			LOGGER.error(t.getMessage(), t);
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "getNamespaceVersionMappingList", method = RequestMethod.GET)
	public RequestResult getNamespaceVersionMappingList(HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			List<NamespaceVersionMapping> namespaceVersionMappingList = namespaceVersionMappingService.selectAllWithNotDeleted();
			requestResult.setObj(namespaceVersionMappingList);
			requestResult.setSuccess(true);
		} catch (Throwable t) {
			LOGGER.error(t.getMessage(), t);
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}
}
