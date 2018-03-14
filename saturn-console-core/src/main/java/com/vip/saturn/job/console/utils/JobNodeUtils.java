package com.vip.saturn.job.console.utils;

import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;

import com.vip.saturn.job.console.domain.AbnormalJob;
import com.vip.saturn.job.console.domain.JobBriefInfo.JobType;
import com.vip.saturn.job.console.exception.JobConsoleException;

/**
 * @author timmy.hu
 */
public class JobNodeUtils {

	private static final String NOAH_CONTAINER_REGEX = "^(.*-noah)";

	private static final Pattern NOAH_CONTAINER_PATTERN = Pattern.compile(NOAH_CONTAINER_REGEX);

	public static List<String> getItems(CuratorFramework curatorClient, AbnormalJob abnormalJob) {
		String executionRootpath = JobNodePath.getExecutionNodePath(abnormalJob.getJobName());
		List<String> items = null;
		try {
			items = curatorClient.getChildren().forPath(executionRootpath);
		} catch (Exception ignore){
			return items;
		}
		return items;
	}

	public static boolean isEnabledPath(CuratorFramework curatorClient, AbnormalJob abnormalJob) {
		String enabledPath = JobNodePath.getConfigNodePath(abnormalJob.getJobName(), "enabled");
		return Boolean.valueOf(getData(curatorClient, enabledPath));
	}

	public static boolean isCronJob(CuratorFramework curatorClient, String jobName) {
		String jobType = getData(curatorClient, JobNodePath.getConfigNodePath(jobName, "jobType"));
		return JobType.JAVA_JOB.name().equals(jobType) || JobType.SHELL_JOB.name().equals(jobType);
	}

	public static boolean isEnabledReport(CuratorFramework curatorClient, String jobName) {
		String enabledReportPath = JobNodePath.getConfigNodePath(jobName, "enabledReport");
		String enabledReportVal = getData(curatorClient, enabledReportPath);
		return enabledReportVal == null || "true".equals(enabledReportVal);
	}

	public static int getShardingTotalCount(CuratorFramework curatorClient, String jobName) {
		String shardingTcPath = JobNodePath.getConfigNodePath(jobName, "shardingTotalCount");
		return Integer.parseInt(JobNodeUtils.getData(curatorClient, shardingTcPath));
	}

	public static String getData(final CuratorFramework curatorClient, final String znode) {
		try {
			if (checkExists(curatorClient, znode)) {
				byte[] getZnodeData = curatorClient.getData().forPath(znode);
				if (getZnodeData == null) {// executor的分片可能存在全部飘走的情况，sharding节点有可能获取到的是null，需要对null做判断，否则new
											// String时会报空指针异常
					return null;
				}
				return new String(getZnodeData, Charset.forName("UTF-8"));
			} else {
				return null;
			}
		} catch (final NoNodeException ignore) {
			return null;
			// CHECKSTYLE:OFF
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			throw new JobConsoleException(ex);
		}
	}

	public static boolean checkExists(final CuratorFramework curatorClient, final String znode) {
		try {
			return null != curatorClient.checkExists().forPath(znode);
			// CHECKSTYLE:OFF
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			throw new JobConsoleException(ex);
		}
	}

	public static int getCVersion(final CuratorFramework curatorClient, final String znode) {
		try {
			Stat stat = curatorClient.checkExists().forPath(znode);
			if (stat != null) {
				return stat.getCversion();
			} else {
				return 0;
			}
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			throw new JobConsoleException(ex);
		}
	}

	public static long getMtime(CuratorFramework curatorClient, String znode) {
		try {
			Stat stat = curatorClient.checkExists().forPath(znode);
			if (stat != null) {
				return stat.getMtime();
			} else {
				return 0L;
			}
		} catch (final Exception ex) {
			// CHECKSTYLE:ON
			throw new JobConsoleException(ex);
		}
	}

	public static long getLastCompleteTime(CuratorFramework curatorClient, String jobName, String shardingItemStr) {
		String lastCompleteTimePath = JobNodePath.getExecutionNodePath(jobName, shardingItemStr, "lastCompleteTime");
		String data = getData(curatorClient, lastCompleteTimePath);
		return StringUtils.isBlank(data) ? 0 : Long.parseLong(data.trim());
	}

	public static String getTimeZone(String jobName, CuratorFramework curatorClient) {
		String timeZoneStr = JobNodeUtils.getData(curatorClient, JobNodePath.getConfigNodePath(jobName, "timeZone"));
		if (timeZoneStr == null || timeZoneStr.trim().length() == 0) {
			timeZoneStr = SaturnConstants.TIME_ZONE_ID_DEFAULT;
		}
		return timeZoneStr;
	}

	/**
	 * 首先检查是否符合Noah容器的命名规则，如果不符合，则检查是否有task node.
	 *
	 * @param curatorClient
	 * @param executorName
	 * @return true，是容器executor；false，不是容器executor；
	 */
	public static boolean isExecutorInDocker(CuratorFramework curatorClient, String executorName) {
		if (NOAH_CONTAINER_PATTERN.matcher(executorName).matches()) {
			return true;
		}

		return JobNodeUtils.checkExists(curatorClient, ExecutorNodePath.get$ExecutorTaskNodePath(executorName));
	}

	public static boolean getLocalMode(CuratorFramework curatorClient, String jobName) {
		return Boolean
				.valueOf(JobNodeUtils.getData(curatorClient, JobNodePath.getConfigNodePath(jobName, "localMode")));
	}

	public static int getErrorCountAllTime(CuratorFramework curatorClient, String jobName) {
		String errorCountOfThisJobAllTimeStr = getData(curatorClient, JobNodePath.getErrorCountPath(jobName));
		return errorCountOfThisJobAllTimeStr == null ? 0 : Integer.valueOf(errorCountOfThisJobAllTimeStr);
	}

	public static int getProcessCountAllTime(CuratorFramework curatorClient, String jobName) {
		String processCountOfThisJobAllTimeStr = getData(curatorClient, JobNodePath.getProcessCountPath(jobName));
		return processCountOfThisJobAllTimeStr == null ? 0 : Integer.valueOf(processCountOfThisJobAllTimeStr);
	}
}
