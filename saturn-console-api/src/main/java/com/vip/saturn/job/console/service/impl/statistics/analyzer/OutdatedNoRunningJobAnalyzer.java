package com.vip.saturn.job.console.service.impl.statistics.analyzer;

import com.vip.saturn.job.console.domain.AbnormalJob;
import com.vip.saturn.job.console.domain.AbnormalShardingState;
import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.helper.DashboardConstants;
import com.vip.saturn.job.console.service.helper.DashboardServiceHelper;
import com.vip.saturn.job.console.utils.CronExpression;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.SaturnConstants;
import com.vip.saturn.job.integrate.service.ReportAlarmService;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OutdatedNoRunningJobAnalyzer {

	private static final Logger log = LoggerFactory.getLogger(OutdatedNoRunningJobAnalyzer.class);

	private Map<String/** domainName_jobName_shardingItemStr **/
			, AbnormalShardingState /** abnormal sharding state */
			> abnormalShardingStateCache = new ConcurrentHashMap<>();

	private ReportAlarmService reportAlarmService;

	private List<AbnormalJob> outdatedNoRunningJobs = new ArrayList<>();

	private static boolean isCronJob(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String jobName) {
		String jobType = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobType"));
		return JobType.JAVA_JOB.name().equals(jobType) || JobType.SHELL_JOB.name().equals(jobType);
	}

	private static boolean isEnabledPath(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			AbnormalJob abnormalJob) {
		String enabledPath = JobNodePath.getConfigNodePath(abnormalJob.getJobName(), "enabled");
		return Boolean.parseBoolean(curatorFrameworkOp.getData(enabledPath));
	}

	public static boolean isEnabledReport(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String jobName) {
		String enabledReportPath = JobNodePath.getConfigNodePath(jobName, "enabledReport");
		String enabledReportVal = curatorFrameworkOp.getData(enabledReportPath);
		return enabledReportVal == null || "true".equals(enabledReportVal);
	}

	private static long getLastCompleteTime(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String jobName,
			String shardingItemStr) {
		String lastCompleteTimePath = JobNodePath.getExecutionNodePath(jobName, shardingItemStr, "lastCompleteTime");
		String data = curatorFrameworkOp.getData(lastCompleteTimePath);
		return StringUtils.isBlank(data) ? 0 : Long.parseLong(data.trim());
	}

	/**
	 * 该时间是否在作业暂停时间段范围内。
	 * <p>
	 * 特别的，无论pausePeriodDate，还是pausePeriodTime，如果解析发生异常，则忽略该节点，视为没有配置该日期或时分段。
	 *
	 * @return 该时间是否在作业暂停时间段范围内。
	 */
	private static boolean isInPausePeriod(Date date, String pausePeriodDate, String pausePeriodTime,
			TimeZone timeZone) {
		Calendar calendar = Calendar.getInstance(timeZone);
		calendar.setTime(date);
		int iMon = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH begin from 0.
		int d = calendar.get(Calendar.DAY_OF_MONTH);
		int h = calendar.get(Calendar.HOUR_OF_DAY);
		int m = calendar.get(Calendar.MINUTE);

		boolean pausePeriodDateIsEmpty = (pausePeriodDate == null || pausePeriodDate.trim().isEmpty());
		boolean dateIn = false;
		if (!pausePeriodDateIsEmpty) {
			dateIn = isDateInPausePeriodDate(iMon, d, pausePeriodDate);
		}

		boolean timeIn = false;
		boolean pausePeriodTimeIsEmpty = (pausePeriodTime == null || pausePeriodTime.trim().isEmpty());
		if (!pausePeriodTimeIsEmpty) {
			timeIn = isTimeInPausePeriodTime(h, m, pausePeriodTime);
		}

		if (pausePeriodDateIsEmpty) {
			if (pausePeriodTimeIsEmpty) {
				return false;
			} else {
				return timeIn;
			}
		} else {
			if (pausePeriodTimeIsEmpty) {
				return dateIn;
			} else {
				return dateIn && timeIn;
			}
		}
	}

	private static boolean isDateInPausePeriodDate(int m, int d, String pausePeriodDate) {
		boolean dateIn = false;

		String[] periodsDate = pausePeriodDate.split(",");
		if (periodsDate == null) {
			return dateIn;
		}
		for (String period : periodsDate) {
			String[] tmp = period.trim().split("-");
			if (tmp == null || tmp.length != 2) {
				dateIn = false;
				break;
			}
			String left = tmp[0].trim();
			String right = tmp[1].trim();
			String[] sMdLeft = left.split("/");
			String[] sMdRight = right.split("/");
			if (sMdLeft != null && sMdLeft.length == 2 && sMdRight != null && sMdRight.length == 2) {
				try {
					int iMLeft = Integer.parseInt(sMdLeft[0]);
					int dLeft = Integer.parseInt(sMdLeft[1]);
					int iMRight = Integer.parseInt(sMdRight[0]);
					int dRight = Integer.parseInt(sMdRight[1]);
					boolean isBiggerThanLeft = m > iMLeft || (m == iMLeft && d >= dLeft);
					boolean isSmallerThanRight = m < iMRight || (m == iMRight && d <= dRight);
					dateIn = isBiggerThanLeft && isSmallerThanRight;
					if (dateIn) {
						break;
					}
				} catch (NumberFormatException e) {
					dateIn = false;
					break;
				}
			} else {
				dateIn = false;
				break;
			}
		}
		return dateIn;
	}

	private static boolean isTimeInPausePeriodTime(int hour, int min, String pausePeriodTime) {
		boolean timeIn = false;
		String[] periodsTime = pausePeriodTime.split(",");
		if (periodsTime == null) {
			return timeIn;
		}
		for (String period : periodsTime) {
			String[] tmp = period.trim().split("-");
			if (tmp == null || tmp.length != 2) {
				timeIn = false;
				break;
			}
			String left = tmp[0].trim();
			String right = tmp[1].trim();
			String[] hmLeft = left.split(":");
			String[] hmRight = right.split(":");
			if (hmLeft != null && hmLeft.length == 2 && hmRight != null && hmRight.length == 2) {
				try {
					int hLeft = Integer.parseInt(hmLeft[0]);
					int mLeft = Integer.parseInt(hmLeft[1]);
					int hRight = Integer.parseInt(hmRight[0]);
					int mRight = Integer.parseInt(hmRight[1]);
					boolean isBiggerThanLeft = hour > hLeft || (hour == hLeft && min >= mLeft);
					boolean isSmallerThanRight = hour < hRight || (hour == hRight && min <= mRight);
					timeIn = isBiggerThanLeft && isSmallerThanRight;
					if (timeIn) {
						break;
					}
				} catch (NumberFormatException e) {
					timeIn = false;
					break;
				}
			} else {
				timeIn = false;
				break;
			}
		}
		return timeIn;
	}

	public void analyze(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, List<AbnormalJob> oldAbnormalJobs,
			String jobName,
			String jobDegree, RegistryCenterConfiguration config) {
		AbnormalJob unnormalJob = new AbnormalJob(jobName, config.getNamespace(), config.getNameAndNamespace(),
				config.getDegree());
		unnormalJob.setJobDegree(jobDegree);
		checkOutdatedNoRunningJob(oldAbnormalJobs, curatorFrameworkOp, unnormalJob);
	}

	private synchronized boolean contains(AbnormalJob abnormalJob) {
		return outdatedNoRunningJobs.contains(abnormalJob);
	}

	private synchronized void addAbnormalJob(AbnormalJob abnormalJob) {
		outdatedNoRunningJobs.add(abnormalJob);
	}

	private void checkOutdatedNoRunningJob(List<AbnormalJob> oldAbnormalJobs,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			AbnormalJob abnormalJob) {
		try {
			if (!isCronJob(curatorFrameworkOp, abnormalJob.getJobName())) {
				return;
			}
			if (!isEnabledPath(curatorFrameworkOp, abnormalJob)) {
				return;
			}
			if (!isEnabledReport(curatorFrameworkOp, abnormalJob.getJobName())) {
				return;
			}
			doCheckAndHandleOutdatedNoRunningJob(oldAbnormalJobs, curatorFrameworkOp, abnormalJob);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * 检查和处理问题作业
	 */
	private void doCheckAndHandleOutdatedNoRunningJobByShardingItem(List<AbnormalJob> oldAbnormalJobs,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, AbnormalJob abnormalJob, String enabledPath,
			String item) {
		if (contains(abnormalJob)) {
			return;
		}
		String jobName = abnormalJob.getJobName();
		int cversion = getCversion(curatorFrameworkOp, JobNodePath.getExecutionItemNodePath(jobName, item));
		long nextFireTime = checkShardingItemState(curatorFrameworkOp, abnormalJob, enabledPath, item);
		if (nextFireTime != -1 && doubleCheckShardingState(abnormalJob, item, cversion)) {
			if (abnormalJob.getCause() == null) {
				abnormalJob.setCause(AbnormalJob.Cause.NOT_RUN.name());
			}
			handleOutdatedNoRunningJob(oldAbnormalJobs, curatorFrameworkOp, abnormalJob, nextFireTime);
		}
	}

	private int getCversion(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String path) {
		int cversion = 0;
		Stat stat = curatorFrameworkOp.getStat(path);
		if (stat != null) {
			cversion = stat.getCversion();
		}
		return cversion;
	}

	/**
	 * 符合连续两次告警返回true，否则返回false ALLOW_DELAY_MILLIONSECONDS * 1.5 钝化触发检查的时间窗口精度 告警触发条件： 1、上次告警+本次检查窗口告警（连续2次）
	 * 2、上次告警CVersion与本次一致（说明当前本次检查窗口时间内没有子节点变更）
	 */
	private boolean doubleCheckShardingState(AbnormalJob abnormalJob, String shardingItemStr, int zkNodeCVersion) {
		String key = abnormalJob.getDomainName() + "_" + abnormalJob.getJobName() + "_" + shardingItemStr;
		long nowTime = System.currentTimeMillis();

		if (abnormalShardingStateCache.containsKey(key)) {
			AbnormalShardingState abnormalShardingState = abnormalShardingStateCache.get(key);
			if (abnormalShardingState != null
					&& abnormalShardingState.getAlertTime()
					+ DashboardConstants.ALLOW_DELAY_MILLIONSECONDS * 1.5 > nowTime
					&& abnormalShardingState.getZkNodeCVersion() == zkNodeCVersion) {
				abnormalShardingStateCache.put(key, new AbnormalShardingState(nowTime, zkNodeCVersion));// 更新告警
				return true;
			} else {
				abnormalShardingStateCache.put(key, new AbnormalShardingState(nowTime, zkNodeCVersion));// 更新无效（过时）告警
				return false;
			}
		} else {
			abnormalShardingStateCache.put(key, new AbnormalShardingState(nowTime, zkNodeCVersion));// 新增告警信息
			return false;
		}
	}

	private void doCheckAndHandleOutdatedNoRunningJob(List<AbnormalJob> oldAbnormalJobs,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			AbnormalJob abnormalJob) throws Exception {
		String jobName = abnormalJob.getJobName();
		String enabledPath = JobNodePath.getConfigNodePath(abnormalJob.getJobName(), "enabled");
		List<String> items = curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(abnormalJob.getJobName()));
		if (items != null && !items.isEmpty()) { // 有分片
			int shardingTotalCount = Integer
					.parseInt(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "shardingTotalCount")));
			for (String item : items) {
				int each = Integer.parseInt(item);
				if (each < shardingTotalCount) { // 过滤历史遗留分片
					doCheckAndHandleOutdatedNoRunningJobByShardingItem(oldAbnormalJobs, curatorFrameworkOp, abnormalJob,
							enabledPath, item);
				}
			}
		} else { // 无分片。还没有开始第一次的作业执行。
			abnormalJob.setCause(AbnormalJob.Cause.NO_SHARDS.name());
			long nextFireTimeAfterThis = curatorFrameworkOp.getMtime(enabledPath);
			Long nextFireTime = getNextFireTimeAfterSpecifiedTimeExcludePausePeriod(
					nextFireTimeAfterThis, jobName, curatorFrameworkOp);
			// 下次触发时间是否小于当前时间+延时, 是则为过时未跑有异常
			if (nextFireTime != null
					&& nextFireTime + DashboardConstants.ALLOW_DELAY_MILLIONSECONDS < System.currentTimeMillis()) {
				handleOutdatedNoRunningJob(oldAbnormalJobs, curatorFrameworkOp, abnormalJob, nextFireTime);
			}
		}
	}

	private void handleOutdatedNoRunningJob(List<AbnormalJob> oldAbnormalJobs,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, AbnormalJob abnormalJob, Long nextFireTime) {
		String jobName = abnormalJob.getJobName();
		String timeZone = getTimeZone(jobName, curatorFrameworkOp);
		// 补充异常信息
		fillAbnormalJobInfo(curatorFrameworkOp, abnormalJob, abnormalJob.getCause(), timeZone, nextFireTime);
		// 如果有必要，上报hermes
		registerAbnormalJobIfNecessary(oldAbnormalJobs, abnormalJob, timeZone, nextFireTime);
		addAbnormalJob(abnormalJob);
		log.info("Job sharding alert with DomainName: {}, JobName: {}, ShardingItem: {}, Cause: {}",
				abnormalJob.getDomainName(), jobName, 0, abnormalJob.getCause());
	}

	private void fillAbnormalJobInfo(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, AbnormalJob abnormalJob,
			String cause, String timeZone, long nextFireTimeExcludePausePeriod) {
		if (executorNotReady(curatorFrameworkOp, abnormalJob)) {
			cause = AbnormalJob.Cause.EXECUTORS_NOT_READY.name();
		}
		abnormalJob.setCause(cause);
		abnormalJob.setTimeZone(timeZone);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
		abnormalJob.setNextFireTimeWithTimeZoneFormat(sdf.format(nextFireTimeExcludePausePeriod));
		abnormalJob.setNextFireTime(nextFireTimeExcludePausePeriod);
	}

	private boolean executorNotReady(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, AbnormalJob abnormalJob) {
		String jobName = abnormalJob.getJobName();
		String serverNodePath = JobNodePath.getServerNodePath(jobName);
		if (curatorFrameworkOp.checkExists(serverNodePath)) {
			List<String> servers = curatorFrameworkOp.getChildren(serverNodePath);
			if (servers != null && !servers.isEmpty()) {
				for (String server : servers) {
					if (curatorFrameworkOp.checkExists(JobNodePath.getServerStatus(jobName, server))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private void registerAbnormalJobIfNecessary(List<AbnormalJob> oldAbnormalJobs, AbnormalJob abnormalJob,
			String timeZone, Long nextFireTime) {
		AbnormalJob oldAbnormalJob = DashboardServiceHelper.findEqualAbnormalJob(abnormalJob, oldAbnormalJobs);
		if (oldAbnormalJob != null) {
			abnormalJob.setRead(oldAbnormalJob.isRead());
			if (oldAbnormalJob.getUuid() != null) {
				abnormalJob.setUuid(oldAbnormalJob.getUuid());
			} else {
				abnormalJob.setUuid(UUID.randomUUID().toString());
			}
		} else {
			abnormalJob.setUuid(UUID.randomUUID().toString());
		}
		if (!abnormalJob.isRead()) {
			try {
				reportAlarmService.dashboardAbnormalJob(abnormalJob.getDomainName(), abnormalJob.getJobName(), timeZone,
						nextFireTime);
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
			}
		}
	}

	/**
	 * 判断分片状态
	 * <p>
	 * 逻辑： 1、注意：针对stock-update域的不上报节点信息但又有分片残留的情况，分片节点下只有两个子节点，返回正常 2、有running节点，返回正常
	 * 3.1、有completed节点，但马上就取不到Mtime，节点有变动说明正常 3.2、根据Mtime计算下次触发时间，比较下次触发时间是否小于当前时间+延时, 是则为过时未跑有异常
	 * 4、既没有running又没completed视为异常
	 *
	 * @return -1：状态正常，非-1：状态异常
	 */
	private long checkShardingItemState(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			AbnormalJob abnormalJob, String enabledPath, String shardingItemStr) {
		List<String> itemChildren = curatorFrameworkOp
				.getChildren(JobNodePath.getExecutionItemNodePath(abnormalJob.getJobName(), shardingItemStr));

		// 注意：针对stock-update域的不上报节点信息但又有分片残留的情况，分片节点下只有两个子节点，返回正常
		if (itemChildren.size() == 2) {
			return -1;
		}
		// 有running节点，返回正常
		if (itemChildren.contains("running")) {
			return -1;
		}

		// 有completed节点：尝试取分片节点的Mtime时间
		// 1、能取到则根据Mtime计算下次触发时间，比较下次触发时间是否小于当前时间+延时, 是则为过时未跑有异常
		// 2、取不到（为0）说明completed节点刚好被删除了，节点有变动说明正常（上一秒还在，下一秒不在了）
		if (itemChildren.contains("completed")) {
			return checkShardingItemStateWhenIsCompleted(curatorFrameworkOp, abnormalJob, enabledPath, shardingItemStr);
		} else { // 既没有running又没completed视为异常
			return checkShardingItemStateWhenNotCompleted(curatorFrameworkOp, abnormalJob, enabledPath,
					shardingItemStr);
		}
	}

	private long checkShardingItemStateWhenNotCompleted(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			AbnormalJob abnormalJob, String enabledPath, String shardingItemStr) {
		if (abnormalJob.getNextFireTimeAfterEnabledMtimeOrLastCompleteTime() == 0) {
			long nextFireTimeAfterThis = curatorFrameworkOp.getMtime(enabledPath);
			long lastCompleteTime = getLastCompleteTime(curatorFrameworkOp, abnormalJob.getJobName(), shardingItemStr);
			if (nextFireTimeAfterThis < lastCompleteTime) {
				nextFireTimeAfterThis = lastCompleteTime;
			}

			abnormalJob.setNextFireTimeAfterEnabledMtimeOrLastCompleteTime(
					getNextFireTimeAfterSpecifiedTimeExcludePausePeriod(nextFireTimeAfterThis, abnormalJob.getJobName(),
							curatorFrameworkOp));
		}
		Long nextFireTime = abnormalJob.getNextFireTimeAfterEnabledMtimeOrLastCompleteTime();
		// 下次触发时间是否小于当前时间+延时, 是则为过时未跑有异常
		if (nextFireTime != null
				&& nextFireTime + DashboardConstants.ALLOW_DELAY_MILLIONSECONDS < System.currentTimeMillis()) {
			return nextFireTime;
		}
		return -1;
	}

	private long checkShardingItemStateWhenIsCompleted(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp,
			AbnormalJob abnormalJob, String enabledPath, String shardingItemStr) {
		long currentTime = System.currentTimeMillis();
		String completedPath = JobNodePath.getExecutionNodePath(abnormalJob.getJobName(), shardingItemStr, "completed");
		long completedMtime = curatorFrameworkOp.getMtime(completedPath);

		if (completedMtime > 0) {
			// 对比minCompletedMtime与enabled mtime, 取最大值
			long nextFireTimeAfterThis = curatorFrameworkOp.getMtime(enabledPath);
			if (nextFireTimeAfterThis < completedMtime) {
				nextFireTimeAfterThis = completedMtime;
			}

			Long nextFireTimeExcludePausePeriod = getNextFireTimeAfterSpecifiedTimeExcludePausePeriod(
					nextFireTimeAfterThis, abnormalJob.getJobName(), curatorFrameworkOp);
			// 下次触发时间是否小于当前时间+延时, 是则为过时未跑有异常
			if (nextFireTimeExcludePausePeriod != null
					&& nextFireTimeExcludePausePeriod + DashboardConstants.ALLOW_DELAY_MILLIONSECONDS < currentTime) {
				// 为了避免误报情况，加上一个delta，然后再计算
				if (!doubleCheckShardingStateAfterAddingDeltaInterval(curatorFrameworkOp, abnormalJob,
						nextFireTimeAfterThis, nextFireTimeExcludePausePeriod, currentTime)) {
					log.debug("still has problem after adding delta interval");
					return nextFireTimeExcludePausePeriod;
				} else {
					return -1;
				}
			}
		}
		return -1;
	}

	/**
	 * 为了避免executor时钟比Console快的现象，加上一个修正值，然后计算新的nextFireTime + ALLOW_DELAY_MILLIONSECONDS 依然早于当前时间。
	 *
	 * @return false: 依然有异常；true: 修正后没有异常。
	 */
	private boolean doubleCheckShardingStateAfterAddingDeltaInterval(
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, AbnormalJob abnormalJob,
			long nextFireTimeAfterThis, Long nextFireTimeExcludePausePeriod, long currentTime) {
		Long nextFireTimeExcludePausePeriodWithDelta = getNextFireTimeAfterSpecifiedTimeExcludePausePeriod(
				nextFireTimeAfterThis + DashboardConstants.INTERVAL_DELTA_IN_SECOND, abnormalJob.getJobName(),
				curatorFrameworkOp);

		if (nextFireTimeExcludePausePeriod.equals(nextFireTimeExcludePausePeriodWithDelta)
				|| nextFireTimeExcludePausePeriodWithDelta
						+ DashboardConstants.ALLOW_DELAY_MILLIONSECONDS < currentTime) {
			log.debug("still not work after adding delta interval");
			return false;
		}

		return true;
	}

	public Long getNextFireTimeAfterSpecifiedTimeExcludePausePeriod(long nextFireTimeAfterThis, String jobName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		String cronPath = JobNodePath.getConfigNodePath(jobName, "cron");
		String cronVal = curatorFrameworkOp.getData(cronPath);
		CronExpression cronExpression = null;
		try {
			cronExpression = new CronExpression(cronVal);
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
			return null;
		}
		String timeZoneStr = getTimeZone(jobName, curatorFrameworkOp);
		TimeZone timeZone = TimeZone.getTimeZone(timeZoneStr);
		cronExpression.setTimeZone(timeZone);

		Date nextFireTime = cronExpression.getTimeAfter(new Date(nextFireTimeAfterThis));
		String pausePeriodDatePath = JobNodePath.getConfigNodePath(jobName, "pausePeriodDate");
		String pausePeriodDate = curatorFrameworkOp.getData(pausePeriodDatePath);
		String pausePeriodTimePath = JobNodePath.getConfigNodePath(jobName, "pausePeriodTime");
		String pausePeriodTime = curatorFrameworkOp.getData(pausePeriodTimePath);

		while (nextFireTime != null && isInPausePeriod(nextFireTime, pausePeriodDate, pausePeriodTime, timeZone)) {
			nextFireTime = cronExpression.getTimeAfter(nextFireTime);
		}
		if (null == nextFireTime) {
			return null;
		}
		return nextFireTime.getTime();
	}

	private String getTimeZone(String jobName, CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
		String timeZoneStr = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeZone"));
		if (timeZoneStr == null || timeZoneStr.trim().length() == 0) {
			timeZoneStr = SaturnConstants.TIME_ZONE_ID_DEFAULT;
		}
		return timeZoneStr;
	}

	public List<AbnormalJob> getOutdatedNoRunningJobs() {
		return new ArrayList<AbnormalJob>(outdatedNoRunningJobs);
	}

	public void setAbnormalShardingStateCache(Map<String, AbnormalShardingState> abnormalShardingStateCache) {
		this.abnormalShardingStateCache = abnormalShardingStateCache;
	}

	public void setReportAlarmService(ReportAlarmService reportAlarmService) {
		this.reportAlarmService = reportAlarmService;
	}
}
