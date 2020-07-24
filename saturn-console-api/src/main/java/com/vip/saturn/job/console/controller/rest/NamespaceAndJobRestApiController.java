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
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.NamespaceAndJobService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Ray Leung
 */
@RequestMapping("/rest/v1")
public class NamespaceAndJobRestApiController extends AbstractRestController {

	@Autowired
	private NamespaceAndJobService namespaceAndJobService;

	@Audit(type = AuditType.REST)
	@RequestMapping(value = "/namespaces/createNamespaceAndImportJobs", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> create(@RequestBody Map<String, Object> reqParams, HttpServletRequest request)
			throws SaturnJobConsoleException {
		try {
			String namespace = checkAndGetParametersValueAsString(reqParams, "namespace", true);
			if (StringUtils.isBlank(namespace)) {
				throw new SaturnJobConsoleException("namespace is empty");
			}
			String zkClusterName = checkAndGetParametersValueAsString(reqParams, "zkCluster", true);
			if (StringUtils.isBlank(zkClusterName)) {
				throw new SaturnJobConsoleException("zkCluster is empty");
			}
			String srcNamespace = checkAndGetParametersValueAsString(reqParams, "srcNamespace", true);
			if (StringUtils.isBlank(srcNamespace)) {
				throw new SaturnJobConsoleException("srcNamespace is empty");
			}
			String createBy = checkAndGetParametersValueAsString(reqParams, "createBy", true);
			namespaceAndJobService.createNamespaceAndCloneJobs(srcNamespace, namespace, zkClusterName, createBy);
			return new ResponseEntity<>(HttpStatus.CREATED);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	@Audit(type = AuditType.REST)
	@RequestMapping(value = "/namespaces/asyncCreateNamespaceAndImportJobs", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> asyncCreate(@RequestBody Map<String, Object> reqParams, HttpServletRequest request)
			throws SaturnJobConsoleException {
		String namespace = checkAndGetParametersValueAsString(reqParams, "namespace", true);
		if (StringUtils.isBlank(namespace)) {
			throw new SaturnJobConsoleException("namespace is empty");
		}
		String zkClusterName = checkAndGetParametersValueAsString(reqParams, "zkCluster", true);
		if (StringUtils.isBlank(zkClusterName)) {
			throw new SaturnJobConsoleException("zkCluster is empty");
		}
		String srcNamespace = checkAndGetParametersValueAsString(reqParams, "srcNamespace", true);
		if (StringUtils.isBlank(srcNamespace)) {
			throw new SaturnJobConsoleException("srcNamespace is empty");
		}
		String createBy = checkAndGetParametersValueAsString(reqParams, "createBy", true);
		namespaceAndJobService.asyncCreateNamespaceAndCloneJobs(srcNamespace, namespace, zkClusterName, createBy);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}
}
