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

package com.vip.saturn.job.console.service.impl.statistics.analyzer;

import com.vip.saturn.job.console.domain.DomainStatistics;
import com.vip.saturn.job.console.domain.JobStatistics;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.ZkCluster;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author timmy.hu
 */
public class DomainStatisticsAnalyzer {

	private List<DomainStatistics> domainList = new ArrayList<DomainStatistics>();

	public DomainStatistics initDomain(ZkCluster zkCluster, RegistryCenterConfiguration config) {
		DomainStatistics domain = new DomainStatistics(config.getNamespace(), zkCluster.getZkAddr(),
				config.getNameAndNamespace());
		addDomain(domain);
		return domain;
	}

	private synchronized void addDomain(DomainStatistics domain) {
		domainList.add(domain);
	}

	public void analyzeProcessCount(DomainStatistics domainStatistics,
			ZkClusterDailyCountAnalyzer zkClusterDailyCountAnalyzer, List<String> jobs,
			Map<String, JobStatistics> jobMap, RegistryCenterConfiguration config) {
		long processCountOfThisDomainAllTime = 0;
		long errorCountOfThisDomainAllTime = 0;
		long processCountOfThisDomainThisDay = 0;
		long errorCountOfThisDomainThisDay = 0;
		for (String job : jobs) {
			String jobDomainKey = job + "-" + config.getNamespace();
			JobStatistics jobStatistics = jobMap.get(jobDomainKey);
			if (jobStatistics != null) {
				processCountOfThisDomainAllTime += jobStatistics.getProcessCountOfAllTime();
				errorCountOfThisDomainAllTime += jobStatistics.getErrorCountOfAllTime();
				processCountOfThisDomainThisDay += jobStatistics.getProcessCountOfTheDay();
				errorCountOfThisDomainThisDay += jobStatistics.getFailureCountOfTheDay();
			}
		}
		zkClusterDailyCountAnalyzer.incrTotalCount(processCountOfThisDomainThisDay);
		zkClusterDailyCountAnalyzer.incrErrorCount(errorCountOfThisDomainThisDay);
		domainStatistics.setErrorCountOfAllTime(errorCountOfThisDomainAllTime);
		domainStatistics.setProcessCountOfAllTime(processCountOfThisDomainAllTime);
		domainStatistics.setErrorCountOfTheDay(errorCountOfThisDomainThisDay);
		domainStatistics.setProcessCountOfTheDay(processCountOfThisDomainThisDay);
	}

	/**
	 * 统计稳定性
	 */
	public void analyzeShardingCount(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			DomainStatistics domainStatistics) {
		if (curatorFrameworkOp.checkExists(ExecutorNodePath.SHARDING_COUNT_PATH)) {
			String countStr = curatorFrameworkOp.getData(ExecutorNodePath.SHARDING_COUNT_PATH);
			if(StringUtils.isNotBlank(countStr)) {
				domainStatistics.setShardingCount(Integer.parseInt(countStr));
			}
		}
	}

	public List<DomainStatistics> getDomainList() {
		return new ArrayList<DomainStatistics>(domainList);
	}
}
