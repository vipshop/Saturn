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

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.domain.SaturnJunkData;
import com.vip.saturn.job.console.service.SaturnJunkDataService;

/**
 * @author yangjuanying
 */
@Controller
@RequestMapping("/")
@Deprecated
public class SaturnJunkDataController extends AbstractController {

	private static final Logger LOGGER = LoggerFactory.getLogger(SaturnJunkDataController.class);

	@Resource
	private SaturnJunkDataService saturnJunkDataService;

	@RequestMapping(value = "junkdata", method = RequestMethod.GET)
	public String junkdata(HttpServletRequest request, HttpSession session, ModelMap model) {
		return "junkdata";
	}

	@ResponseBody
	@RequestMapping(value = "getJunkdata", method = RequestMethod.GET)
	public RequestResult getJunkData(HttpServletRequest request, String newZkClusterKey) {
		RequestResult requestResult = new RequestResult();
		try {
			List<SaturnJunkData> junkData = saturnJunkDataService.getJunkData(newZkClusterKey);
			requestResult.setSuccess(true);
			requestResult.setObj(junkData);
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			if (t instanceof SaturnJobConsoleException) {
				requestResult.setMessage(t.getMessage());
			} else {
				requestResult.setMessage(t.toString());
			}
			LOGGER.error(t.getMessage(), t);
		}
		return requestResult;
	}

	@ResponseBody
	@RequestMapping(value = "removeJunkData", method = RequestMethod.POST)
	public RequestResult removeJunkData(HttpServletRequest request, SaturnJunkData saturnJunkData,
			HttpSession session) {
		RequestResult requestResult = new RequestResult();
		try {
			saturnJunkDataService.removeSaturnJunkData(saturnJunkData);
			LOGGER.info("[removeJunkData success, saturnJunkData is {}]", saturnJunkData);
			requestResult.setSuccess(true);
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			if (t instanceof SaturnJobConsoleException) {
				requestResult.setMessage(t.getMessage());
			} else {
				requestResult.setMessage(t.toString());
			}
			LOGGER.error("[removeJunkData error, saturnJunkData is " + saturnJunkData + "]", t);
		}
		return requestResult;
	}

	@ResponseBody
	@RequestMapping(value = "junkData/deleteRunningNode", method = RequestMethod.POST)
	public RequestResult deleteRunningNode(String namespace, String jobName, Integer item) {
		RequestResult requestResult = new RequestResult();
		try {
			if (namespace == null || namespace.trim().isEmpty()) {
				throw new SaturnJobConsoleException("The namespace can not be null or empty");
			}
			if (jobName == null || jobName.trim().isEmpty()) {
				throw new SaturnJobConsoleException("The jobName can not be null or empty");
			}
			if (item == null) {
				throw new SaturnJobConsoleException("The item can not be null");
			}
			saturnJunkDataService.deleteRunningNode(namespace, jobName, item);
			LOGGER.info("do junkData/deleteRunningNode success, namespace is {}, jobName is {}, item is {}", namespace,
					jobName, item);
			requestResult.setSuccess(true);
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			if (t instanceof SaturnJobConsoleException) {
				requestResult.setMessage(t.getMessage());
			} else {
				requestResult.setMessage(t.toString());
			}
			LOGGER.error(t.getMessage(), t);
		}
		return requestResult;
	}
}
