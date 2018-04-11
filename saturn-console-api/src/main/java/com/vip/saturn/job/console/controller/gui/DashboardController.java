/**
 * Copyright 1999-2015 dangdang.com. <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at <p>
 * http://www.apache.org/licenses/LICENSE-2.0 <p> Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License. </p>
 */
package com.vip.saturn.job.console.controller.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;
import com.vip.saturn.job.console.service.DashboardService;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

import static com.vip.saturn.job.console.exception.SaturnJobConsoleException.ERROR_CODE_NOT_EXISTED;

@RequestMapping("/console/dashboard")
public class DashboardController extends AbstractGUIController {

	@Autowired
	private DashboardService dashboardService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/count")
	public SuccessResponseEntity getStatistics(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		Map<String, Integer> countMap = Maps.newHashMap();
		int executorInDockerCount = 0;
		int executorNotInDockerCount = 0;
		int jobCount = 0;
		int domainCount = 0;

		Collection<ZkCluster> zkClusters = null;
		if (StringUtils.isNotBlank(zkClusterKey)) {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			zkClusters = Lists.newArrayList();
			zkClusters.add(zkCluster);
		} else {
			zkClusters = registryCenterService.getZkClusterList();
		}

		for (ZkCluster zkCluster : zkClusters) {
			// 不统计离线的zkcluster
			if (zkCluster.isOffline()) {
				continue;
			}

			String zkAddr = zkCluster.getZkAddr();
			if (zkAddr != null) {
				executorInDockerCount += dashboardService.executorInDockerCount(zkAddr);
				executorNotInDockerCount += dashboardService.executorNotInDockerCount(zkAddr);
				jobCount += dashboardService.jobCount(zkAddr);
			}
			domainCount += registryCenterService.domainCount(zkCluster.getZkClusterKey());
		}

		countMap.put("executorInDockerCount", executorInDockerCount);
		countMap.put("executorNotInDockerCount", executorNotInDockerCount);
		countMap.put("jobCount", jobCount);
		countMap.put("domainCount", domainCount);
		return new SuccessResponseEntity(countMap);
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/top10FailJob")
	public SuccessResponseEntity top10FailJob(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isNotBlank(zkClusterKey)) {
			ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
			SaturnStatistics ss = dashboardService.top10FailureJob(zkCluster.getZkAddr());
			return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
		}

		return new SuccessResponseEntity(dashboardService.top10FailureJobByAllZkCluster());
	}

	private ZkCluster checkAndGetZkCluster(String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
		if (zkCluster == null) {
			throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED, "zk clsuter " + zkClusterKey + "不存在");
		}
		return zkCluster;
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/top10FailExecutor")
	public SuccessResponseEntity top10FailExecutor(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isNotBlank(zkClusterKey)) {
			ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
			SaturnStatistics ss = dashboardService.top10FailureExecutor(zkCluster.getZkAddr());
			return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
		}
		return new SuccessResponseEntity(dashboardService.top10FailureExecutorByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/top10ActiveJob")
	public SuccessResponseEntity top10ActiveJob(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isNotBlank(zkClusterKey)) {
			ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
			SaturnStatistics ss = dashboardService.top10AactiveJob(zkCluster.getZkAddr());
			return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
		}
		return new SuccessResponseEntity(dashboardService.top10AactiveJobByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/top10LoadJob")
	public SuccessResponseEntity top10LoadJob(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isNotBlank(zkClusterKey)) {
			ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
			SaturnStatistics ss = dashboardService.top10LoadJob(zkCluster.getZkAddr());
			return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
		}
		return new SuccessResponseEntity(dashboardService.top10LoadJobByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/top10FailDomain")
	public SuccessResponseEntity top10FailDomain(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isNotBlank(zkClusterKey)) {
			ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
			SaturnStatistics ss = dashboardService.top10FailureDomain(zkCluster.getZkAddr());
			return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
		}
		return new SuccessResponseEntity(dashboardService.top10FailureDomainByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/top10UnstableDomain")
	public SuccessResponseEntity top10UnstableDomain(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isNotBlank(zkClusterKey)) {
			ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
			SaturnStatistics ss = dashboardService.top10UnstableDomain(zkCluster.getZkAddr());
			return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
		}
		return new SuccessResponseEntity(dashboardService.top10UnstableDomainByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/top10LoadExecutor")
	public SuccessResponseEntity top10LoadExecutor(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isNotBlank(zkClusterKey)) {
			ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
			SaturnStatistics ss = dashboardService.top10LoadExecutor(zkCluster.getZkAddr());
			return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
		}
		return new SuccessResponseEntity(dashboardService.top10LoadExecutorByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/domainProcessCount")
	public SuccessResponseEntity domainProcessCount(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isNotBlank(zkClusterKey)) {
			ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
			SaturnStatistics ss = dashboardService.allProcessAndErrorCountOfTheDay(zkCluster.getZkAddr());
			return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
		}
		return new SuccessResponseEntity(dashboardService.allProcessAndErrorCountOfTheDayByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/domainRank")
	public SuccessResponseEntity loadDomainRank(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isNotBlank(zkClusterKey)) {
			return new SuccessResponseEntity(dashboardService.loadDomainRankDistribution(zkClusterKey));
		}
		return new SuccessResponseEntity(dashboardService.loadDomainRankDistributionByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/jobRank")
	public SuccessResponseEntity loadJobRank(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isNotBlank(zkClusterKey)) {
			ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
			return new SuccessResponseEntity(dashboardService.loadJobRankDistribution(zkCluster.getZkAddr()));
		}
		return new SuccessResponseEntity(dashboardService.loadJobRankDistributionByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/domainExecutorVersionNumber")
	public SuccessResponseEntity versionDomainNumber(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isNotBlank(zkClusterKey)) {
			ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
			return new SuccessResponseEntity(dashboardService.versionDomainNumber(zkCluster.getZkAddr()));
		}
		return new SuccessResponseEntity(dashboardService.versionDomainNumberByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/executorVersionNumber")
	public SuccessResponseEntity versionExecutorNumber(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleException {
		if (StringUtils.isNotBlank(zkClusterKey)) {
			ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
			return new SuccessResponseEntity(dashboardService.versionExecutorNumber(zkCluster.getZkAddr()));
		}
		return new SuccessResponseEntity(dashboardService.versionExecutorNumberByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/namespaces/{namespace:.+}/shardingCount/clean")
	public SuccessResponseEntity cleanShardingCount(@PathVariable String namespace) throws SaturnJobConsoleException {
		assertIsSystemAdmin();
		dashboardService.cleanShardingCount(namespace);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/namespaces/{namespace:.+}/jobs/{jobName}/jobAnalyse/clean")
	public SuccessResponseEntity cleanJobAnalyse(@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobName") @PathVariable String jobName) throws SaturnJobConsoleException {
		assertIsSystemAdmin();
		dashboardService.cleanOneJobAnalyse(namespace, jobName);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/namespaces/{namespace:.+}/jobAnalyse/clean")
	public SuccessResponseEntity cleanJobsAnalyse(@AuditParam("namespace") @PathVariable String namespace)
			throws SaturnJobConsoleException {
		assertIsSystemAdmin();
		dashboardService.cleanAllJobAnalyse(namespace);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping(value = "/namespaces/{namespace:.+}/jobs/{jobName}/jobExecutorCount/clean")
	public SuccessResponseEntity cleanJobExecutorCount(@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobName") @PathVariable String jobName) throws SaturnJobConsoleException {
		assertIsSystemAdmin();
		dashboardService.cleanOneJobExecutorCount(namespace, jobName);
		return new SuccessResponseEntity();
	}

}
