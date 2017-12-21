/**
 * Copyright 1999-2015 dangdang.com.
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

import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;
import com.vip.saturn.job.console.service.DashboardService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("dashboard")
public class DashboardController extends AbstractController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DashboardController.class);

	@Resource
	private RegistryCenterService registryCenterService;

	@Autowired
	private DashboardService dashboardService;

	@RequestMapping(value = "refresh", method = RequestMethod.GET)
	@ResponseBody
	public String refresh() {
		Date start = new Date();
		dashboardService.refreshStatistics2DB(true);
		return "done refresh. takes:" + (new Date().getTime() - start.getTime());
	}

	@RequestMapping(value = "count", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Integer> count(Boolean allZkCluster, String zkClusterKey) {
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

	@RequestMapping(value = "top10FailJob", method = RequestMethod.POST)
	@ResponseBody
	public String top10FailJob(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.top10FailureJobByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				SaturnStatistics ss = dashboardService.top10FailureJob(zkCluster.getZkAddr());
				return ss == null ? null : ss.getResult();
			}
			return null;
		}
	}

	@RequestMapping(value = "top10FailExe", method = RequestMethod.POST)
	@ResponseBody
	public String top10FailExe(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.top10FailureExecutorByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				SaturnStatistics ss = dashboardService.top10FailureExecutor(zkCluster.getZkAddr());
				return ss == null ? null : ss.getResult();
			}
			return null;
		}
	}

	@RequestMapping(value = "top10ActiveJob", method = RequestMethod.POST)
	@ResponseBody
	public String top10ActiveJob(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.top10AactiveJobByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				SaturnStatistics ss = dashboardService.top10AactiveJob(zkCluster.getZkAddr());
				return ss == null ? null : ss.getResult();
			}
			return null;
		}
	}

	@RequestMapping(value = "top10LoadJob", method = RequestMethod.POST)
	@ResponseBody
	public String top10LoadJob(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.top10LoadJobByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				SaturnStatistics ss = dashboardService.top10LoadJob(zkCluster.getZkAddr());
				return ss == null ? null : ss.getResult();
			}
			return null;
		}
	}

	@RequestMapping(value = "top10FailDomain", method = RequestMethod.POST)
	@ResponseBody
	public String top10FailDomain(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.top10FailureDomainByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				SaturnStatistics ss = dashboardService.top10FailureDomain(zkCluster.getZkAddr());
				return ss == null ? null : ss.getResult();
			}
			return null;
		}
	}

	@RequestMapping(value = "top10UnstableDomain", method = RequestMethod.POST)
	@ResponseBody
	public String top10UnstableDomain(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.top10UnstableDomainByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				SaturnStatistics ss = dashboardService.top10UnstableDomain(zkCluster.getZkAddr());
				return ss == null ? null : ss.getResult();
			}
			return null;
		}
	}

	@RequestMapping(value = "top10LoadExecutor", method = RequestMethod.POST)
	@ResponseBody
	public String top10LoadExecutor(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.top10LoadExecutorByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				SaturnStatistics ss = dashboardService.top10LoadExecutor(zkCluster.getZkAddr());
				return ss == null ? null : ss.getResult();
			}
			return null;
		}
	}

	@RequestMapping(value = "unnormalJob", method = RequestMethod.POST)
	@ResponseBody
	public String unnormalJob(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.allUnnormalJobByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				SaturnStatistics ss = dashboardService.allUnnormalJob(zkCluster.getZkAddr());
				return ss == null ? null : ss.getResult();
			}
			return null;
		}
	}

	@RequestMapping(value = "unableFailoverJob", method = RequestMethod.POST)
	@ResponseBody
	public String unableFailoverJob(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.allUnableFailoverJobByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				SaturnStatistics ss = dashboardService.allUnableFailoverJob(zkCluster.getZkAddr());
				return ss == null ? null : ss.getResult();
			}
			return null;
		}
	}

	@RequestMapping(value = "allTimeout4AlarmJob", method = RequestMethod.POST)
	@ResponseBody
	public String allTimeout4AlarmJob(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.allTimeout4AlarmJobByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				SaturnStatistics ss = dashboardService.allTimeout4AlarmJob(zkCluster.getZkAddr());
				return ss == null ? null : ss.getResult();
			}
			return null;
		}
	}

	@RequestMapping(value = "domainProcessCount", method = RequestMethod.POST)
	@ResponseBody
	public String domainProcessCount(Boolean allZkCluster, String zkClusterKey) {
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

	@RequestMapping(value = "cleanShardingCount", method = RequestMethod.POST)
	@ResponseBody
	public String cleanShardingCount(String nns) {
		try {
			dashboardService.cleanShardingCount(nns);
			return "ok";
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return e.getMessage();
		}
	}

	@RequestMapping(value = "cleanOneJobAnalyse", method = RequestMethod.POST)
	@ResponseBody
	public String cleanOneJobAnalyse(String nns, String job) {
		try {
			dashboardService.cleanOneJobAnalyse(job, nns);
			return "ok";
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return e.getMessage();
		}
	}

	@RequestMapping(value = "cleanOneJobExecutorCount", method = RequestMethod.POST)
	@ResponseBody
	public String cleanOneJobExecutorCount(String nns, String job) {
		try {
			dashboardService.cleanOneJobExecutorCount(job, nns);
			return "ok";
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return e.getMessage();
		}
	}

	@RequestMapping(value = "cleanAllJobAnalyse", method = RequestMethod.POST)
	@ResponseBody
	public String cleanAllJobAnalyse(String nns) {
		try {
			dashboardService.cleanAllJobAnalyse(nns);
			return "ok";
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return e.getMessage();
		}
	}

	@RequestMapping(value = "loadDomainRank", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Integer> loadDomainRank(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.loadDomainRankDistributionByAllZkCluster();
		} else {
			return dashboardService.loadDomainRankDistribution(zkClusterKey);
		}
	}

	@RequestMapping(value = "loadJobRank", method = RequestMethod.POST)
	@ResponseBody
	public Map<Integer, Integer> loadJobRank(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.loadJobRankDistributionByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				return dashboardService.loadJobRankDistribution(zkCluster.getZkAddr());
			}
			return new HashMap<>();
		}
	}

	@RequestMapping(value = "abnormalContainer", method = RequestMethod.POST)
	@ResponseBody
	public String abnormalContainer(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.abnormalContainerByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				SaturnStatistics ss = dashboardService.abnormalContainer(zkCluster.getZkAddr());
				return ss == null ? null : ss.getResult();
			}
			return null;
		}
	}

	@RequestMapping(value = "versionDomainNumber", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Long> versionDomainNumber(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.versionDomainNumberByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				return dashboardService.versionDomainNumber(zkCluster.getZkAddr());
			}
			return new HashMap<>();
		}
	}

	@RequestMapping(value = "versionExecutorNumber", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Long> versionExecutorNumber(Boolean allZkCluster, String zkClusterKey) {
		if (allZkCluster != null && allZkCluster) {
			return dashboardService.versionExecutorNumberByAllZkCluster();
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				return dashboardService.versionExecutorNumber(zkCluster.getZkAddr());
			}
			return new HashMap<>();
		}
	}

	@RequestMapping(value = "setUnnormalJobMonitorStatusToRead", method = RequestMethod.POST)
	@ResponseBody
	public String setUnnormalJobMonitorStatusToRead(Boolean allZkCluster, String zkClusterKey, String uuid) {
		if (allZkCluster != null && allZkCluster) {
			dashboardService.setUnnormalJobMonitorStatusToReadByAllZkCluster(uuid);
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				dashboardService.setUnnormalJobMonitorStatusToRead(zkCluster.getZkAddr(), uuid);
			}
		}
		return "ok";
	}

	@RequestMapping(value = "setTimeout4AlarmJobMonitorStatusToRead", method = RequestMethod.POST)
	@ResponseBody
	public String setTimeout4AlarmJobMonitorStatusToRead(Boolean allZkCluster, String zkClusterKey, String uuid) {
		if (allZkCluster != null && allZkCluster) {
			dashboardService.setTimeout4AlarmJobMonitorStatusToReadByAllZkCluster(uuid);
		} else {
			ZkCluster zkCluster = registryCenterService.getZkCluster(zkClusterKey);
			if (zkCluster != null) {
				dashboardService.setTimeout4AlarmJobMonitorStatusToRead(zkCluster.getZkAddr(), uuid);
			}
		}
		return "ok";
	}

}
