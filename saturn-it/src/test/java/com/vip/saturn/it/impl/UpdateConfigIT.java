package com.vip.saturn.it.impl;

import com.vip.saturn.it.base.AbstractSaturnIT;
import com.vip.saturn.it.base.FinishCheck;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.Calendar;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UpdateConfigIT extends AbstractSaturnIT {

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
		startExecutorList(1);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorListGracefully();
		stopSaturnConsoleList();
	}

	@Test
	public void updateCron() throws Exception {
		int shardCount = 1;
		String jobName = "updateCron";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setTimeZone(TimeZone.getDefault().getID());
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setShardingItemParameters("0=0");
		addJob(jobConfig);
		Thread.sleep(1000);
		enableJob(jobName);
		Thread.sleep(3 * 1000);
		disableJob(jobName);
		Thread.sleep(2 * 1000);

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			assertThat(SimpleJavaJob.statusMap.get(key)).isGreaterThanOrEqualTo(1);
		}

		zkUpdateJobNode(jobConfig.getJobName(), "config/cron", "0/1 * * * * ?");
		Thread.sleep(1000);
		enableJob(jobName);
		Thread.sleep(2 * 1000);

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			assertThat(SimpleJavaJob.statusMap.get(key)).isGreaterThanOrEqualTo(2);
		}

		SimpleJavaJob.statusMap.clear();
		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		Thread.sleep(2000);
	}

	@Test
	public void updatePauseDate() throws Exception {
		int shardCount = 1;
		String jobName = "updatePauseDate";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setTimeZone(TimeZone.getDefault().getID());
		jobConfig.setCron("*/1 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setShardingItemParameters("0=0");
		addJob(jobConfig);
		Thread.sleep(1000);
		enableJob(jobName);
		Thread.sleep(2 * 1000);
		disableJob(jobName);
		Thread.sleep(2 * 1000);

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			assertThat(SimpleJavaJob.statusMap.get(key)).isGreaterThanOrEqualTo(1);
		}

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
		String pauseDate =
				month + "/" + Calendar.getInstance().get(Calendar.DAY_OF_MONTH) + "-" + month + "/" + Calendar
						.getInstance().get(Calendar.DAY_OF_MONTH);
		String pauseTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + ":00" + "-" + Calendar.getInstance()
				.get(Calendar.HOUR_OF_DAY) + ":59";

		zkUpdateJobNode(jobName, "config/pausePeriodDate", pauseDate);
		zkUpdateJobNode(jobName, "config/pausePeriodTime", pauseTime);

		Thread.sleep(1000);
		enableJob(jobConfig.getJobName());
		Thread.sleep(2 * 1000);

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			assertThat(SimpleJavaJob.statusMap.get(key)).isEqualTo(0);
		}

		SimpleJavaJob.statusMap.clear();
		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		Thread.sleep(2000);
	}

	/**
	 * 作业配置正常显示日志时，zk的jobLog会有执行日志的IT
	 */
	@Test
	public void updateShowNormalLog() throws Exception {
		final int shardCount = 1;
		final String jobName = "updateShowNormalLog";

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setTimeZone(TimeZone.getDefault().getID());
		jobConfig.setCron("*/1 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setShardingItemParameters("0=0");
		addJob(jobConfig);
		Thread.sleep(1000);
		enableJob(jobName);
		Thread.sleep(2 * 1000);

		for (int i = 0; i < shardCount; i++) {
			String path = JobNodePath.getNodeFullPath(jobName, ExecutionNode.getJobLog(i));
			assertThat(regCenter.isExisted(path)).isEqualTo(false);
		}

		disableJob(jobName);
		Thread.sleep(2 * 1000);

		zkUpdateJobNode(jobName, "config/showNormalLog", "true");

		Thread.sleep(1 * 1000);
		enableJob(jobName);
		Thread.sleep(2000);

		doReport(jobName);

		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean isOk() {
					for (int i = 0; i < shardCount; i++) {
						String path = JobNodePath.getNodeFullPath(jobName, ExecutionNode.getJobLog(i));
						if (regCenter.isExisted(path)) {
							assertThat(regCenter.getDirectly(path)).isNotEmpty();
							continue;
						}
						return false;
					}
					return true;
				}

			}, 10);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		Thread.sleep(2000);
	}
}
