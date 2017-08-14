/**
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.controller;

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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("dashboard")
public class DashboardController  extends AbstractController {
	
	protected static Logger AUDITLOGGER = LoggerFactory.getLogger("AUDITLOG");
	protected static Logger LOGGER = LoggerFactory.getLogger(DashboardController.class);
    @Resource
    private RegistryCenterService registryCenterService;
   
    @Autowired
    private DashboardService dashboardService;
    
    @RequestMapping(value = "refresh", method = RequestMethod.GET)
    @ResponseBody
	public String refresh(HttpServletRequest request) {
			Date start = new Date();
			dashboardService.refreshStatistics2DB(true);
			return "done refresh. takes:" + (new Date().getTime() - start.getTime());
    }
    
    @RequestMapping(value = "count", method = RequestMethod.POST)
    @ResponseBody
	public Map<String,Integer> count(HttpServletRequest request) {
		HttpSession session = request.getSession();
		String zkAddr = getCurrentZkAddr(session);
		Map<String, Integer> countMap = new HashMap<String, Integer>();
		if (zkAddr != null) {
			countMap.put("executorInDockerCount", dashboardService.executorInDockerCount(zkAddr));
			countMap.put("executorNotInDockerCount", dashboardService.executorNotInDockerCount(zkAddr));
			countMap.put("jobCount", dashboardService.jobCount(zkAddr));
		}
		String zkClusterKey = getCurrentZkClusterKey(session);
		if (zkClusterKey != null) {
			countMap.put("domainCount", registryCenterService.domainCount(zkClusterKey));
		}
		return countMap;
    }
    
    @RequestMapping(value = "top10FailJob", method = RequestMethod.POST)
    @ResponseBody
	public String top10FailJob(HttpServletRequest request) {
    	SaturnStatistics ss = dashboardService.top10FailureJob(getCurrentZkAddr(request.getSession()));
    	return ss == null? null:ss.getResult();
    }
    
    @RequestMapping(value = "top10FailExe", method = RequestMethod.POST)
    @ResponseBody
	public String top10FailExe(HttpServletRequest request) {
    	SaturnStatistics ss = dashboardService.top10FailureExecutor(getCurrentZkAddr(request.getSession()));
    	return ss == null? null:ss.getResult();
    }
    
    @RequestMapping(value = "top10ActiveJob", method = RequestMethod.POST)
    @ResponseBody
	public String top10ActiveJob(HttpServletRequest request) {
    	SaturnStatistics ss = dashboardService.top10AactiveJob(getCurrentZkAddr(request.getSession()));
    	return ss == null? null:ss.getResult();
    }
    @RequestMapping(value = "top10LoadJob", method = RequestMethod.POST)
    @ResponseBody
	public String top10LoadJob(HttpServletRequest request) {
    	SaturnStatistics ss = dashboardService.top10LoadJob(getCurrentZkAddr(request.getSession()));
    	return ss == null? null:ss.getResult();
    }
    @RequestMapping(value = "top10FailDomain", method = RequestMethod.POST)
    @ResponseBody
	public String top10FailDomain(HttpServletRequest request) {
//    	return "[{\"domainName\":\"g.vip.com\",\"errorCountOfAllTime\":0,\"errorCountOfTheDay\":0,\"failureRateOfAllTime\":0.23,\"processCountOfAllTime\":0,\"processCountOfTheDay\":0,\"shardingCount\":39,\"zkList\":\"localhost:2181\"},{\"domainName\":\"g.1.com\",\"errorCountOfAllTime\":0,\"errorCountOfTheDay\":0,\"failureRateOfAllTime\":0.34,\"processCountOfAllTime\":0,\"processCountOfTheDay\":0,\"shardingCount\":35,\"zkList\":\"localhost:2181\"},{\"domainName\":\"yardman.api.vip.com\",\"errorCountOfAllTime\":0,\"errorCountOfTheDay\":0,\"failureRateOfAllTime\":0.1,\"processCountOfAllTime\":0,\"processCountOfTheDay\":0,\"shardingCount\":23,\"zkList\":\"localhost:2181\"},{\"domainName\":\"b2c-tools.vip.vip.com\",\"errorCountOfAllTime\":0,\"errorCountOfTheDay\":0,\"failureRateOfAllTime\":0.33,\"processCountOfAllTime\":0,\"processCountOfTheDay\":0,\"shardingCont\":23,\"zkList\":\"localhost:2181\"},{\"domainName\":\"chembo.demo\",\"errorCountOfAllTime\":0,\"errorCountOfTheDay\":0,\"failureRateOfAllTime\":0.12,\"processCountOfAllTime\":0,\"processCountOfTheDay\":0,\"shardingCount\":27,\"zkList\":\"localhost:2181\"}]";
    	SaturnStatistics ss = dashboardService.top10FailureDomain(getCurrentZkAddr(request.getSession()));
    	return ss == null? null:ss.getResult();
    }
    @RequestMapping(value = "top10UnstableDomain", method = RequestMethod.POST)
    @ResponseBody
	public String top10UnstableDomain(HttpServletRequest request) {
    	SaturnStatistics ss = dashboardService.top10UnstableDomain(getCurrentZkAddr(request.getSession()));
    	return ss == null? null:ss.getResult();
    }
    @RequestMapping(value = "top10LoadExecutor", method = RequestMethod.POST)
    @ResponseBody
	public String top10LoadExecutor(HttpServletRequest request) {
    	SaturnStatistics ss = dashboardService.top10LoadExecutor(getCurrentZkAddr(request.getSession()));
    	return ss == null? null:ss.getResult();
    }
    @RequestMapping(value = "unnormalJob", method = RequestMethod.POST)
    @ResponseBody
	public String unnormalJob(HttpServletRequest request) {
    	SaturnStatistics ss = dashboardService.allUnnormalJob(getCurrentZkAddr(request.getSession()));
    	return ss == null? null:ss.getResult();
    	//return "[{\"domainName\":\"g.1.com\",\"jobName\":\"j1\",\"nextFireTime\":1477037590349,\"nns\":\"/平台架构/任务调度系统/g.1.com\"},{\"domainName\":\"g.1.com\",\"jobName\":\"j2\",\"nextFireTime\":1477037590349,\"nns\":\"/平台架构/任务调度系统/g.1.com\"},{\"domainName\":\"g.1.com\",\"jobName\":\"j3\",\"nextFireTime\":1477037590349,\"nns\":\"/平台架构/任务调度系统/g.1.com\"},{\"domainName\":\"g.1.com\",\"jobName\":\"j4\",\"nextFireTime\":1477037590349,\"nns\":\"/平台架构/任务调度系统/g.1.com\"},{\"domainName\":\"g.1.com\",\"jobName\":\"j5\",\"nextFireTime\":1477037590349,\"nns\":\"/平台架构/任务调度系统/g.1.com\"}]";
    }
    
    @RequestMapping(value = "unableFailoverJob", method = RequestMethod.POST)
    @ResponseBody
	public String unableFailoverJob(HttpServletRequest request) {
    	SaturnStatistics ss = dashboardService.allUnableFailoverJob(getCurrentZkAddr(request.getSession()));
    	return ss == null? null:ss.getResult();
    }

	@RequestMapping(value = "allTimeout4AlarmJob", method = RequestMethod.POST)
	@ResponseBody
	public String allTimeout4AlarmJob(HttpServletRequest request) {
		SaturnStatistics ss = dashboardService.allTimeout4AlarmJob(getCurrentZkAddr(request.getSession()));
		return ss == null? null:ss.getResult();
	}
    
    @RequestMapping(value = "domainProcessCount", method = RequestMethod.POST)
    @ResponseBody
	public String domainProcessCount(HttpServletRequest request) {
    	SaturnStatistics ss = dashboardService.allProcessAndErrorCountOfTheDay(getCurrentZkAddr(request.getSession()));
    	return ss == null? null:ss.getResult();
    }
	
    @RequestMapping(value = "cleanShardingCount", method = RequestMethod.POST)
    @ResponseBody
	public String cleanShardingCount(HttpServletRequest request, String nns) {
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
	public String cleanOneJobAnalyse(HttpServletRequest request, String nns, String job) {
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
	public String cleanOneJobExecutorCount(HttpServletRequest request, String nns, String job) {
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
	public String cleanAllJobAnalyse(HttpServletRequest request, String nns) {
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
	public Map<String, Integer> loadDomainRank(HttpSession session) {
		return dashboardService.loadDomainRankDistribution(getCurrentZkClusterKey(session));
    }
    
    @RequestMapping(value = "loadJobRank", method = RequestMethod.POST)
    @ResponseBody
	public Map<Integer, Integer> loadJobRank(HttpSession session) {
		return dashboardService.loadJobRankDistribution(getCurrentZkAddr(session));
    }

	@RequestMapping(value = "abnormalContainer", method = RequestMethod.POST)
	@ResponseBody
	public String abnormalContainer(HttpServletRequest request) {
		SaturnStatistics ss = dashboardService.abnormalContainer(getCurrentZkAddr(request.getSession()));
		return ss == null? null:ss.getResult();
	}

	@RequestMapping(value = "versionDomainNumber", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Long> versionDomainNumber(HttpServletRequest request) {
		return dashboardService.versionDomainNumber(getCurrentZkAddr(request.getSession()));
	}

	@RequestMapping(value = "versionExecutorNumber", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Long> versionExecutorNumber(HttpServletRequest request) {
		return dashboardService.versionExecutorNumber(getCurrentZkAddr(request.getSession()));
	}
	
}
