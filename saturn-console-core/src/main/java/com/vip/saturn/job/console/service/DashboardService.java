/**
 * 
 */
package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.mybatis.entity.SaturnStatistics;

import java.util.Map;

/**
 * @author chembo.huang
 *
 */
public interface DashboardService {

	void refreshStatistics2DB(boolean force);

	int executorInDockerCount(String key);

	int executorNotInDockerCount(String key);

	int jobCount(String key);

	/**
	 * 失败率top10的域列表
	 */
	SaturnStatistics top10FailureDomain(String key);

	/**
	 * 根据失败率top10的作业列表
	 */
	SaturnStatistics top10FailureJob(String key);

	/**
	 * 最活跃作业的作业列表(即当天执行次数最多的作业)
	 */
	SaturnStatistics top10AactiveJob(String key);

	/**
	 * 负荷最重的top10的Executor列表
	 */
	SaturnStatistics top10LoadExecutor(String key);

	/**
	 * 负荷最重的top10的作业列表以及其成功率
	 */
	SaturnStatistics top10LoadJob(String key);

	/**
	 * 稳定性最差的top10的域列表
	 */
	SaturnStatistics top10UnstableDomain(String key);

	/**
	 * 全域当天处理总数，失败总数
	 */
	SaturnStatistics allProcessAndErrorCountOfTheDay(String key);

	/**
	 * 异常作业列表 (如下次调度时间已经过了，但是作业没有被调度)<br>
	 * 根据$Jobs/xx/config/cron计算出下次执行时间，如果大于当前时间且作业不在running，则为异常
	 */
	SaturnStatistics allUnnormalJob(String key);

	SaturnStatistics allTimeout4AlarmJob(String key);

	/**
	 * 无法高可用作业列表 当只有一个可用的物理机Executor运行该作业分片时，如果该Executor宕机将造成无法飘移分片到其他Executor
	 */
	SaturnStatistics allUnableFailoverJob(String key);

	SaturnStatistics top10FailureExecutor(String key);

	/**
	 * 清除该域下的/$SaturnExecutors/sharding/count
	 * @param nns
	 */
	void cleanShardingCount(String nns) throws Exception;

	void cleanOneJobAnalyse(String jobName, String nns) throws Exception;

	void cleanAllJobAnalyse(String nns) throws Exception;

	void cleanAllJobExecutorCount(String nns) throws Exception;

	void cleanOneJobExecutorCount(String jobName, String nns) throws Exception;

	Map<String, Integer> loadDomainRankDistribution(String key);

	Map<Integer, Integer> loadJobRankDistribution(String key);

	SaturnStatistics abnormalContainer(String key);

	Map<String, Long> versionDomainNumber(String key);

	Map<String, Long> versionExecutorNumber(String key);

	void setUnnormalJobMonitorStatusToRead(String key, String uuid);
}
