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
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.exception.SaturnJobConsoleGUIException;
import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;
import com.vip.saturn.job.console.service.DashboardService;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/console/dashboard")
public class DashboardController extends AbstractGUIController {

	private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

	@Autowired
	private DashboardService dashboardService;

	/**
	 * 域overview统计
	 *
	 * @param allZkCluster true，统计全域；false 或 null代表特定zk集群
	 * @param zkClusterKey 特定的zkClusterKey
	 */
	@GetMapping(value = "count")
	@ResponseBody
	public Map<String, Integer> count(@RequestParam(required = false) Boolean allZkCluster,
			@RequestParam(required = false) String zkClusterKey) {
		Map<String, Integer> countMap = new HashMap<>();
		int executorInDockerCount = 0;
		int executorNotInDockerCount = 0;
		int jobCount = 0;
		int domainCount = 0;
		if (allZkCluster != null && allZkCluster) {
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
		return countMap;
	}

	@GetMapping(value = "top10FailJob")
	@ResponseBody
	public String top10FailJob(@RequestParam(required = false) Boolean allZkCluster,
			@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleGUIException {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.top10FailureJobByAllZkCluster();
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10FailureJob(zkCluster.getZkAddr());
		return ss == null ? null : ss.getResult();
	}

	private ZkCluster checkAndGetZkCluster(String zkClusterKey) throws SaturnJobConsoleGUIException {
		ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
		if (zkCluster == null) {
			throw new SaturnJobConsoleGUIException("zk clsuter " + zkClusterKey + "不存在");
		}
		return zkCluster;
	}

	@GetMapping(value = "top10FailExecutor")
	@ResponseBody
	public String top10FailExecutor(@RequestParam(required = false) Boolean allZkCluster,
			@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleGUIException {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.top10FailureExecutorByAllZkCluster();
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10FailureExecutor(zkCluster.getZkAddr());
		return ss == null ? null : ss.getResult();
	}

	@GetMapping(value = "top10ActiveJob")
	@ResponseBody
	public String top10ActiveJob(@RequestParam(required = false) Boolean allZkCluster,
			@RequestParam(required = false) String zkClusterKey)
			throws SaturnJobConsoleGUIException {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.top10AactiveJobByAllZkCluster();
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10AactiveJob(zkCluster.getZkAddr());
		return ss == null ? null : ss.getResult();
	}

	@GetMapping(value = "top10LoadJob")
	@ResponseBody
	public String top10LoadJob(@RequestParam(required = false) Boolean allZkCluster,
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleGUIException {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.top10LoadJobByAllZkCluster();
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10LoadJob(zkCluster.getZkAddr());
		return ss == null ? null : ss.getResult();
	}

	@GetMapping(value = "top10FailDomain")
	@ResponseBody
	public String top10FailDomain(@RequestParam(required = false) Boolean allZkCluster,
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleGUIException {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.top10FailureDomainByAllZkCluster();
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10FailureDomain(zkCluster.getZkAddr());
		return ss == null ? null : ss.getResult();
	}

	@GetMapping(value = "top10UnstableDomain")
	@ResponseBody
	public String top10UnstableDomain(@RequestParam(required = false) Boolean allZkCluster,
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleGUIException {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.top10UnstableDomainByAllZkCluster();
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10UnstableDomain(zkCluster.getZkAddr());
		return ss == null ? null : ss.getResult();
	}

	@GetMapping(value = "top10LoadExecutor")
	@ResponseBody
	public String top10LoadExecutor(@RequestParam(required = false) Boolean allZkCluster,
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleGUIException {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.top10LoadExecutorByAllZkCluster();
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		SaturnStatistics ss = dashboardService.top10LoadExecutor(zkCluster.getZkAddr());
		return ss == null ? null : ss.getResult();
	}


	@GetMapping(value = "domainProcessCount")
	@ResponseBody
	public String domainProcessCount(@RequestParam(required = false) Boolean allZkCluster,
			@RequestParam(required = false) String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.allProcessAndErrorCountOfTheDayByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				SaturnStatistics ss = dashboardService.allProcessAndErrorCountOfTheDay(zkCluster.getZkAddr());
				return ss == null ? null : ss.getResult();
			}
			return null;
		}
	}

	@PostMapping(value = "cleanShardingCount")
	public SuccessResponseEntity cleanShardingCount(@RequestParam String nns) throws Exception {
		dashboardService.cleanShardingCount(nns);
		return new SuccessResponseEntity();
	}

	@PostMapping(value = "cleanOneJobAnalyse")
	public SuccessResponseEntity cleanOneJobAnalyse(@RequestParam String nns, @RequestParam String job)
			throws Exception {
		dashboardService.cleanOneJobAnalyse(job, nns);
		return new SuccessResponseEntity();
	}

	@PostMapping(value = "cleanOneJobExecutorCount")
	public SuccessResponseEntity cleanOneJobExecutorCount(@RequestParam String nns, @RequestParam String job)
			throws Exception {
		dashboardService.cleanOneJobExecutorCount(job, nns);
		return new SuccessResponseEntity();
	}

	@PostMapping(value = "cleanAllJobAnalyse")
	public SuccessResponseEntity cleanAllJobAnalyse(@RequestParam String nns) throws Exception {
		dashboardService.cleanAllJobAnalyse(nns);
		return new SuccessResponseEntity();
	}

	@GetMapping(value = "domainRank")
	@ResponseBody
	public Map<String, Integer> loadDomainRank(@RequestParam(required = false) Boolean allZkCluster,
			@RequestParam(required = false) String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.loadDomainRankDistributionByAllZkCluster();
		}
		return dashboardService.loadDomainRankDistribution(zkClusterKey);
	}

	@GetMapping(value = "jobRank")
	@ResponseBody
	public Map<Integer, Integer> loadJobRank(@RequestParam(required = false) Boolean allZkCluster,
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleGUIException {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.loadJobRankDistributionByAllZkCluster();
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		return dashboardService.loadJobRankDistribution(zkCluster.getZkAddr());

	}

	@GetMapping(value = "domainExecutorVersionNumber")
	@ResponseBody
	public Map<String, Long> versionDomainNumber(@RequestParam(required = false) Boolean allZkCluster,
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleGUIException {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.versionDomainNumberByAllZkCluster();
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		return dashboardService.versionDomainNumber(zkCluster.getZkAddr());
	}

	@GetMapping(value = "executorVersionNumber")
	@ResponseBody
	public Map<String, Long> versionExecutorNumber(@RequestParam(required = false) Boolean allZkCluster,
			@RequestParam(required = false) String zkClusterKey) throws SaturnJobConsoleGUIException {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.versionExecutorNumberByAllZkCluster();
		}

		ZkCluster zkCluster = checkAndGetZkCluster(zkClusterKey);
		return dashboardService.versionExecutorNumber(zkCluster.getZkAddr());
	}

}
