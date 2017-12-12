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

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.utils.SessionAttributeKeys;

@Controller
@RequestMapping("/")
public class HomeController extends AbstractController {

	@Resource
	private JobDimensionService jobDimensionService;

	@Resource
	private ExecutorService executorService;

	@RequestMapping(method = RequestMethod.GET)
	public String homepage(final ModelMap model, HttpServletRequest request) {
		RegistryCenterClient client = getClientInSession(request.getSession());
		if (null != client && client.isConnected()) {
			String ns = client.getCuratorClient().getNamespace();
			model.addAttribute("ns", ns);
		} else {
			model.addAttribute("ns", "");
		}
		model.put("version", version);
		return "home";
	}

	@RequestMapping(value = "dashboard", method = RequestMethod.GET)
	public String dashboard(final ModelMap model, HttpServletRequest request) {
		return "dashboard";
	}

	@RequestMapping(value = "registry_center_page", method = RequestMethod.GET)
	public String registryCenterPage(final ModelMap model, HttpServletRequest request) {
		return "registry_center";
	}

	@RequestMapping(value = "404", method = RequestMethod.GET)
	public String notFoundPage(final ModelMap model) {
		return "404";
	}

	@RequestMapping(value = "500", method = RequestMethod.GET)
	public String errorPage(final ModelMap model, HttpServletRequest req) {
		String code = null, message = null, type = null;
		Object codeObj, messageObj, typeObj;
		Throwable throwable;

		// todo handle org.springframework.web.bind.MissingServletRequestParameterException

		codeObj = req.getAttribute("javax.servlet.error.status_code");
		messageObj = req.getAttribute("javax.servlet.error.message");
		typeObj = req.getAttribute("javax.servlet.error.exception_type");
		throwable = (Throwable) req.getAttribute("javax.servlet.error.exception");

		// Convert the attributes to string values
		if (codeObj != null)
			code = codeObj.toString();
		if (messageObj != null)
			message = messageObj.toString();
		if (typeObj != null)
			type = typeObj.toString();

		// The error reason is either the status code or exception type
		String reason = (code != null ? code : type);

		model.addAttribute("message", "<H4>" + reason + "</H4>" + "<H4>" + message + "</H4>" + "<P>"
				+ ((throwable != null) ? getStackTrace(throwable) : "") + "</P>");
		return "500";
	}

	@RequestMapping(value = "job_detail", method = RequestMethod.GET)
	public String jobDetail(@RequestParam final String jobName, final ModelMap model, final String nns,
			final HttpSession session, HttpServletRequest request) {
		model.put("jobName", jobName);
		model.put("jobStatus", jobDimensionService.getJobStatus(jobName));
		model.put("isEnabled", jobDimensionService.isJobEnabled(jobName));
		setSession(registryCenterService.connect(nns), session);
		String jobRate = jobDimensionService.geJobRunningInfo(jobName);
		String jobType = jobDimensionService.getJobType(jobName);
		model.put("jobRate", jobRate);
		model.put("regname", nns);
		model.put("jobType", jobType);
		return "job_detail";
	}

	@RequestMapping(value = "overview", method = RequestMethod.GET)
	public String overview(String name, final ModelMap model, HttpServletRequest request, final HttpSession session) {
		model.put("containerType", SaturnEnvProperties.CONTAINER_TYPE);
		if (StringUtils.isNoneEmpty(name)) {
			setSession(registryCenterService.connect(name), session);
			RegistryCenterClient client = getClientInSession(session);
			if (null == client || !client.isConnected()) {
				return "redirect:registry_center_page";
			}
			renderShellExecutorInfos(model);
			return "overview";
		}
		RegistryCenterConfiguration config = (RegistryCenterConfiguration) request.getSession()
				.getAttribute(SessionAttributeKeys.ACTIVATED_CONFIG_SESSION_KEY);
		if (config == null) {
			return "redirect:registry_center_page";
		} else {
			setSession(registryCenterService.connect(config.getNameAndNamespace()), session);
			renderShellExecutorInfos(model);
			return "overview";
		}
	}

	private void renderShellExecutorInfos(final ModelMap model) {
		List<String> aliveExecutorNames = executorService.getAliveExecutorNames();
		if (aliveExecutorNames != null && !aliveExecutorNames.isEmpty()) {
			model.put("aliveExecutors", aliveExecutorNames.toString());
		} else {
			model.put("aliveExecutors", "");
		}
	}

	@RequestMapping(value = "loadZks", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> loadZks(final HttpSession session) throws IOException, ParseException {
		Map<String, Object> model = new HashMap<>();
		model.put("clusters", registryCenterService.getZkClusterList());
		model.put("currentZk", getCurrentZkClusterKey(session));
		return model;
	}

}
