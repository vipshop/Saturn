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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;
import com.vip.saturn.job.console.mybatis.service.NamespaceZkClusterMapping4SqlService;
import com.vip.saturn.job.console.mybatis.service.ZkClusterInfoService;

/**
 * Discover zk connection string by namespace.
 *
 * @deprecated It is deprecated as it is replaced by DiscoveryRestAPIController. Executor &gt;= 3.1.2 will not call this API.
 *
 * @author hebelala
 */
@Deprecated
@RequestMapping("/rest/v1")
public class ZkDiscoveryRestApiController extends AbstractRestController {

	@Resource
	private ZkClusterInfoService zkClusterInfoService;

	@Resource
	private NamespaceZkClusterMapping4SqlService namespaceZkclusterMapping4SqlService;

	@Audit(type = AuditType.REST)
	@RequestMapping(value = "/discoverZk", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> discoverZk(String namespace, HttpServletRequest request) throws SaturnJobConsoleException {
		HttpHeaders headers = new HttpHeaders();
		try {
			checkMissingParameter("namespace", namespace);

			String zkClusterKey = namespaceZkclusterMapping4SqlService.getZkClusterKey(namespace);

			if (zkClusterKey == null) {
				throw new SaturnJobConsoleHttpException(HttpStatus.NOT_FOUND.value(),
						"The namespace：[" + namespace + "] is not registered in Saturn.");
			}
			ZkClusterInfo zkClusterInfo = zkClusterInfoService.getByClusterKey(zkClusterKey);
			if (zkClusterInfo == null) {
				throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
						"The clusterKey: [" + zkClusterKey + "] is not configured in db for " + namespace);
			}
			return new ResponseEntity<Object>(zkClusterInfo.getConnectString(), headers, HttpStatus.OK);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

}
