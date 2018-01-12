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
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleGUIException;
import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;
import com.vip.saturn.job.console.service.DashboardService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/console")
public class DashboardController extends AbstractGUIController {

	@Autowired
	private DashboardService dashboardService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/zkClusterKeys")
	public SuccessResponseEntity getZkClusterKeys() throws SaturnJobConsoleException {
		Collection<ZkCluster> zkClusters = registryCenterService.getZkClusterList();
		List<String> zkClusterKeys = Lists.newArrayList();
		for (ZkCluster zkCluster : zkClusters) {
			zkClusterKeys.add(zkCluster.getZkClusterKey());
		}
		return new SuccessResponseEntity(zkClusterKeys);
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/dashboard/count")
	public SuccessResponseEntity countByAllZkClusters() throws SaturnJobConsoleException {
		Map<String, Integer> countMap = Maps.newHashMap();
		int executorInDockerCount = 0;
		int executorNotInDockerCount = 0;
		int jobCount = 0;
		int domainCount = 0;

		Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
		for (ZkCluster zkCluster : zkClusterList) {
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
	@GetMapping(value = "/zkClusters/{zkClusterKey:.+}/dashboard/count")
	public SuccessResponseEntity countByZkCluster(@PathVariable String zkClusterKey) throws SaturnJobConsoleException {
		Map<String, Integer> countMap = Maps.newHashMap();
		int executorInDockerCount = 0;
		int executorNotInDockerCount = 0;
		int jobCount = 0;
		int domainCount = 0;
		ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
		if (zkCluster != null) {
			executorInDockerCount = dashboardService.executorInDockerCount(zkCluster.getZkAddr());
			executorNotInDockerCount = dashboardService.executorNotInDockerCount(zkCluster.getZkAddr());
			jobCount = dashboardService.jobCount(zkCluster.getZkAddr());
			domainCount = registryCenterService.domainCount(zkCluster.getZkClusterKey());
		}
		countMap.put("executorInDockerCount", executorInDockerCount);
		countMap.put("executorNotInDockerCount", executorNotInDockerCount);
		countMap.put("jobCount", jobCount);
		countMap.put("domainCount", domainCount);
		return new SuccessResponseEntity(countMap);
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/dashboard/top10FailJob")
	public SuccessResponseEntity top10FailJobOfAllZkClusters() throws SaturnJobConsoleException {
		return new SuccessResponseEntity(dashboardService.top10FailureJobByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/{zkClusterKey:.+}/dashboard/top10FailJob")
	public SuccessResponseEntity top10FailJobOfZkCluster(@PathVariable String zkClusterKey)
			throws SaturnJobConsoleException {
		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10FailureJob(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	private ZkCluster checkAndGetZkCluster(String zkClusterKey) throws SaturnJobConsoleGUIException {
		ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
		if (zkCluster == null) {
			throw new SaturnJobConsoleGUIException("zk clsuter " + zkClusterKey + "不存在");
		}
		return zkCluster;
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/dashboard/top10FailExecutor")
	public SuccessResponseEntity top10FailExecutorOfAllZkClusters() throws SaturnJobConsoleException {
		return new SuccessResponseEntity(dashboardService.top10FailureExecutorByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/{zkClusterKey:.+}/dashboard/top10FailExecutor")
	public SuccessResponseEntity top10FailExecutorOfZkCluster(@PathVariable String zkClusterKey)
			throws SaturnJobConsoleException {
		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10FailureExecutor(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/dashboard/top10ActiveJob")
	public SuccessResponseEntity top10ActiveJobOfAllZkClusters() throws SaturnJobConsoleException {
		return new SuccessResponseEntity(dashboardService.top10AactiveJobByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/{zkClusterKey:.+}/dashboard/top10ActiveJob")
	public SuccessResponseEntity top10ActiveJobOfZkCluster(@PathVariable String zkClusterKey)
			throws SaturnJobConsoleException {
		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10AactiveJob(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/dashboard/top10LoadJob")
	public SuccessResponseEntity top10LoadJobOfAllZkClusters() throws SaturnJobConsoleException {
		return new SuccessResponseEntity(dashboardService.top10LoadJobByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/{zkClusterKey:.+}/dashboard/top10LoadJob")
	public SuccessResponseEntity top10LoadJobOfZkCluster(@PathVariable String zkClusterKey)
			throws SaturnJobConsoleException {
		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10LoadJob(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/dashboard/top10FailDomain")
	public SuccessResponseEntity top10FailDomainOfAllZkClusters() throws SaturnJobConsoleException {
		return new SuccessResponseEntity(dashboardService.top10FailureDomainByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/{zkClusterKey:.+}/dashboard/top10FailDomain")
	public SuccessResponseEntity top10FailDomainOfZkCluster(
			@PathVariable String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10FailureDomain(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/dashboard/top10UnstableDomain")
	public SuccessResponseEntity top10UnstableDomainOfAllZkClusters() throws SaturnJobConsoleException {
		return new SuccessResponseEntity(dashboardService.top10UnstableDomainByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/{zkClusterKey:.+}/dashboard/top10UnstableDomain")
	public SuccessResponseEntity top10UnstableDomainOfZkCluster(
			@PathVariable String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10UnstableDomain(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/dashboard/top10LoadExecutor")
	public SuccessResponseEntity top10LoadExecutorOfAllZkCluster() throws SaturnJobConsoleException {
		return new SuccessResponseEntity(dashboardService.top10LoadExecutorByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/{zkClusterKey:.+}/dashboard/top10LoadExecutor")
	public SuccessResponseEntity top10LoadExecutorOfZkCluster(
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10LoadExecutor(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/dashboard/domainProcessCount")
	public SuccessResponseEntity domainProcessCountOfAllZkClusters() throws SaturnJobConsoleException {
		return new SuccessResponseEntity(dashboardService.allProcessAndErrorCountOfTheDayByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/{zkClusterKey:.+}/dashboard/domainProcessCount")
	public SuccessResponseEntity domainProcessCountOfZkCluster(@PathVariable String zkClusterKey)
			throws SaturnJobConsoleException {
		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.allProcessAndErrorCountOfTheDay(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/dashboard/domainRank")
	public SuccessResponseEntity loadDomainRankOfAllZkClusters() throws SaturnJobConsoleException {
		return new SuccessResponseEntity(dashboardService.loadDomainRankDistributionByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/{zkClusterKey:.+}/dashboard/domainRank")
	public SuccessResponseEntity loadDomainRankOfZkCluster(@PathVariable String zkClusterKey)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(dashboardService.loadDomainRankDistribution(zkClusterKey));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/dashboard/jobRank")
	public SuccessResponseEntity loadJobRankOfAllZkClusters(
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(dashboardService.loadJobRankDistributionByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/{zkClusterKey:.+}/dashboard/jobRank")
	public SuccessResponseEntity loadJobRankOfZkCluster(
			@PathVariable String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		return new SuccessResponseEntity(dashboardService.loadJobRankDistribution(zkCluster.getZkAddr()));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/dashboard/domainExecutorVersionNumber")
	public SuccessResponseEntity versionDomainNumberOfAllZkClusters() throws SaturnJobConsoleException {
		return new SuccessResponseEntity(dashboardService.versionDomainNumberByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/{zkClusterKey:.+}/dashboard/domainExecutorVersionNumber")
	public SuccessResponseEntity versionDomainNumberOfZkCluster(@PathVariable String zkClusterKey)
			throws SaturnJobConsoleException {
		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		return new SuccessResponseEntity(dashboardService.versionDomainNumber(zkCluster.getZkAddr()));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/dashboard/executorVersionNumber")
	public SuccessResponseEntity versionExecutorNumberOfAllZkClusters() throws SaturnJobConsoleException {
		return new SuccessResponseEntity(dashboardService.versionExecutorNumberByAllZkCluster());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/zkClusters/{zkClusterKey:.+}/dashboard/executorVersionNumber")
	public SuccessResponseEntity versionExecutorNumberOfZkCluster(
			@PathVariable String zkClusterKey) throws SaturnJobConsoleException {
		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		return new SuccessResponseEntity(dashboardService.versionExecutorNumber(zkCluster.getZkAddr()));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@PostMapping(value = "/namespaces/{namespace:.+}/shardingCount/clean")
	public SuccessResponseEntity cleanShardingCount(@PathVariable String namespace) throws SaturnJobConsoleException {
		dashboardService.cleanShardingCount(namespace);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@PostMapping(value = "/namespaces/{namespace:.+}/jobs/{jobName}/jobAnalyse/clean")
	public SuccessResponseEntity cleanJobAnalyse(@PathVariable String namespace, @PathVariable String jobName)
			throws SaturnJobConsoleException {
		dashboardService.cleanOneJobAnalyse(jobName, namespace);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@PostMapping(value = "/namespaces/{namespace:.+}/jobAnalyse/clean")
	public SuccessResponseEntity cleanJobsAnalyse(@PathVariable String namespace) throws SaturnJobConsoleException {
		dashboardService.cleanAllJobAnalyse(namespace);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@PostMapping(value = "/namespaces/{namespace:.+}/jobs/{jobName}/jobExecutorCount/clean")
	public SuccessResponseEntity cleanJobExecutorCount(@PathVariable String namespace, @PathVariable String jobName)
			throws SaturnJobConsoleException {
		dashboardService.cleanOneJobExecutorCount(jobName, namespace);
		return new SuccessResponseEntity();
	}

}
