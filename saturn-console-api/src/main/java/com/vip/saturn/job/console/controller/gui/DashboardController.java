/**
 * Copyright 1999-2015 dangdang.com. <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. </p>
 */

package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleGUIException;
import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;
import com.vip.saturn.job.console.service.DashboardService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/console/zkClusters/{zkClusterKey}/dashboard")
public class DashboardController extends AbstractGUIController {

	@Autowired
	private DashboardService dashboardService;

	/**
	 * 域overview统计
	 *
	 * @param zkClusterKey 如果非空，即返回特定的zkClusterKey的统计信息，否则返回全域信息
	 */
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "count")
	public SuccessResponseEntity count(@RequestParam(required = false) String zkClusterKey) {
		Map<String, Integer> countMap = new HashMap<>();
		int executorInDockerCount = 0;
		int executorNotInDockerCount = 0;
		int jobCount = 0;
		int domainCount = 0;
		if (StringUtils.isBlank(zkClusterKey)) {
			Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
			for (ZkCluster zkCluster : zkClusterList) {
				String zkAddr = zkCluster.getZkAddr();
				if (zkAddr != null) {
					executorInDockerCount += dashboardService.executorInDockerCount(zkAddr);
					executorNotInDockerCount += dashboardService.executorNotInDockerCount(zkAddr);
					jobCount += dashboardService.jobCount(zkAddr);
				}
				String zck = zkCluster.getZkClusterKey();
				if (zck != null) {
					domainCount += registryCenterService.domainCount(zck);
				}
			}
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				executorInDockerCount = dashboardService.executorInDockerCount(zkCluster.getZkAddr());
				executorNotInDockerCount = dashboardService.executorNotInDockerCount(zkCluster.getZkAddr());
				jobCount = dashboardService.jobCount(zkCluster.getZkAddr());
				domainCount = registryCenterService.domainCount(zkCluster.getZkClusterKey());
			}
		}
		countMap.put("executorInDockerCount", executorInDockerCount);
		countMap.put("executorNotInDockerCount", executorNotInDockerCount);
		countMap.put("jobCount", jobCount);
		countMap.put("domainCount", domainCount);
		return new SuccessResponseEntity(countMap);
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "top10FailJob")
	public SuccessResponseEntity top10FailJob(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleGUIException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(dashboardService.top10FailureJobByAllZkCluster());
		}

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
	@GetMapping(value = "top10FailExecutor")
	public SuccessResponseEntity top10FailExecutor(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleGUIException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(dashboardService.top10FailureExecutorByAllZkCluster());
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10FailureExecutor(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "top10ActiveJob")
	public SuccessResponseEntity top10ActiveJob(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleGUIException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(dashboardService.top10AactiveJobByAllZkCluster());
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10AactiveJob(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	@GetMapping(value = "top10LoadJob")
	public SuccessResponseEntity top10LoadJob(@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleGUIException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(dashboardService.top10LoadJobByAllZkCluster());
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10LoadJob(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "top10FailDomain")
	public SuccessResponseEntity top10FailDomain(
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleGUIException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(dashboardService.top10FailureDomainByAllZkCluster());
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10FailureDomain(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "top10UnstableDomain")
	public SuccessResponseEntity top10UnstableDomain(
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleGUIException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(dashboardService.top10UnstableDomainByAllZkCluster());
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10UnstableDomain(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "top10LoadExecutor")
	public SuccessResponseEntity top10LoadExecutor(
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleGUIException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(dashboardService.top10LoadExecutorByAllZkCluster());
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10LoadExecutor(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "domainProcessCount")
	public SuccessResponseEntity domainProcessCount(
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleGUIException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(dashboardService.allProcessAndErrorCountOfTheDayByAllZkCluster());
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.allProcessAndErrorCountOfTheDay(zkCluster.getZkAddr());
		return ss == null ? new SuccessResponseEntity() : new SuccessResponseEntity(ss.getResult());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "domainRank")
	public SuccessResponseEntity loadDomainRank(
			@RequestParam(required = false) String zkClusterKey) {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(dashboardService.loadDomainRankDistributionByAllZkCluster());
		}
		return new SuccessResponseEntity(dashboardService.loadDomainRankDistribution(zkClusterKey));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "jobRank")
	public SuccessResponseEntity loadJobRank(
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleGUIException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(dashboardService.loadJobRankDistributionByAllZkCluster());
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		return new SuccessResponseEntity(dashboardService.loadJobRankDistribution(zkCluster.getZkAddr()));

	}

	@GetMapping(value = "domainExecutorVersionNumber")
	public SuccessResponseEntity versionDomainNumber(
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleGUIException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(dashboardService.versionDomainNumberByAllZkCluster());
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		return new SuccessResponseEntity(dashboardService.versionDomainNumber(zkCluster.getZkAddr()));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "executorVersionNumber")
	public SuccessResponseEntity versionExecutorNumber(
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleGUIException {
		if (StringUtils.isBlank(zkClusterKey)) {
			return new SuccessResponseEntity(dashboardService.versionExecutorNumberByAllZkCluster());
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		return new SuccessResponseEntity(dashboardService.versionExecutorNumber(zkCluster.getZkAddr()));
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@PostMapping(value = "cleanShardingCount")
	public SuccessResponseEntity cleanShardingCount(@RequestParam String nns) throws Exception {
		dashboardService.cleanShardingCount(nns);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@PostMapping(value = "cleanOneJobAnalyse")
	public SuccessResponseEntity cleanOneJobAnalyse(@RequestParam String nns, @RequestParam String job)
			throws Exception {
		dashboardService.cleanOneJobAnalyse(job, nns);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@PostMapping(value = "cleanOneJobExecutorCount")
	public SuccessResponseEntity cleanOneJobExecutorCount(@RequestParam String nns, @RequestParam String job)
			throws Exception {
		dashboardService.cleanOneJobExecutorCount(job, nns);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@PostMapping(value = "cleanAllJobAnalyse")
	public SuccessResponseEntity cleanAllJobAnalyse(@RequestParam String nns) throws Exception {
		dashboardService.cleanAllJobAnalyse(nns);
		return new SuccessResponseEntity();
	}

}
