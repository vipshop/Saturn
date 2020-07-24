/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

package com.vip.saturn.job.console.controller.rest;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.controller.AbstractController;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.StatisticsRefreshService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * RESTful APIs of dashboard refresh.
 *
 * @author timmy.hu
 */
@RequestMapping("/rest/v1")
public class DashboardRefreshRestApiController extends AbstractRestController {

	@Resource
	private StatisticsRefreshService statisticsRefreshService;

	/**
	 * 根据ZK集群key，刷新该集群的dashboard信息
	 */
	@Audit(type = AuditType.REST)
	@RequestMapping(value = "/dashboard/refresh", method = {RequestMethod.POST,
			RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> dashboardRefresh(String zkClusterKey, HttpServletRequest request)
			throws SaturnJobConsoleException {
		try {
			checkMissingParameter("zkClusterKey", zkClusterKey);
			long beforeRefresh = System.currentTimeMillis();
			statisticsRefreshService.refresh(zkClusterKey, true);
			long afterRefresh = System.currentTimeMillis();
			long takeTime = afterRefresh - beforeRefresh;
			return new ResponseEntity<Object>(takeTime, HttpStatus.OK);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

}
