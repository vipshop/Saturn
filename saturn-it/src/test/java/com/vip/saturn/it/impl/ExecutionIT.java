package com.vip.saturn.it.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.vip.saturn.it.job.LongtimeJavaJob;
import com.vip.saturn.job.internal.storage.JobNodePath;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.job.internal.config.JobConfiguration;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExecutionIT extends AbstractSaturnIT {

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorList();
		stopSaturnConsoleList();
	}

	@Test
	public void test_A_report() throws Exception {
		startExecutorList(1);

		final JobConfiguration jobConfiguration = new JobConfiguration("test_A_report");
		jobConfiguration.setCron("* * * * * ? 2099");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");

		addJob(jobConfiguration);
		configJob(jobConfiguration.getJobName(), "config/enabledReport", "true");
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		runAtOnce(jobConfiguration.getJobName());
		Thread.sleep(1000);

		assertThat(getJobNode(jobConfiguration, "execution/0/lastBeginTime")).isNull();
		assertThat(getJobNode(jobConfiguration, "execution/0/nextFireTime")).isNull();
		assertThat(getJobNode(jobConfiguration, "execution/0/lastCompleteTime")).isNull();
		assertThat(getJobNode(jobConfiguration, "execution/0/jobMsg")).isNull();
		assertThat(getJobNode(jobConfiguration, "execution/0/jobLog")).isNull();
		assertThat(getJobNode(jobConfiguration, "execution/0/completed")).isNotNull();

		doReport(jobConfiguration);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				if (getJobNode(jobConfiguration, "execution/0/lastBeginTime") != null
						&& getJobNode(jobConfiguration, "execution/0/nextFireTime") != null
						&& getJobNode(jobConfiguration, "execution/0/lastCompleteTime") != null
						&& getJobNode(jobConfiguration, "execution/0/jobMsg") != null
						&& getJobNode(jobConfiguration, "execution/0/jobLog") != null) {
					return true;
				}
				return false;
			}
		}, 3);

		removeJobNode(jobConfiguration, "execution/0/lastBeginTime");
		removeJobNode(jobConfiguration, "execution/0/nextFireTime");
		removeJobNode(jobConfiguration, "execution/0/lastCompleteTime");
		removeJobNode(jobConfiguration, "execution/0/jobMsg");
		removeJobNode(jobConfiguration, "execution/0/jobLog");
		removeJobNode(jobConfiguration, "execution/0/completed");

		disableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		updateJobConfig(jobConfiguration, "enabledReport", false);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		runAtOnce(jobConfiguration.getJobName());
		Thread.sleep(1000);
		assertThat(getJobNode(jobConfiguration, "execution/0/completed")).isNull();
		doReport(jobConfiguration);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				if (getJobNode(jobConfiguration, "execution/0/lastBeginTime") != null
						&& getJobNode(jobConfiguration, "execution/0/nextFireTime") != null
						&& getJobNode(jobConfiguration, "execution/0/lastCompleteTime") != null
						&& getJobNode(jobConfiguration, "execution/0/jobMsg") != null
						&& getJobNode(jobConfiguration, "execution/0/jobLog") != null) {
					return true;
				}
				return false;
			}
		}, 3);

		disableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		stopExecutorList();
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

	@Test
	public void test_B_executionStatus() throws Exception {
		startExecutorList(1);

		final String jobName = "test_B_executionStatus";
		final int shardCount = 1;
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			LongtimeJavaJob.JobStatus status = new LongtimeJavaJob.JobStatus();
			status.runningCount = 0;
			status.sleepSeconds = 20;
			status.finished = false;
			status.timeout = false;
			LongtimeJavaJob.statusMap.put(key, status);
		}

		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("* * * * * ? 2099");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(LongtimeJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setTimeoutSeconds(0);
		jobConfiguration.setEnabledReport(true);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		runAtOnce(jobName);
		Thread.sleep(2000);

		assertThat(getJobNode(jobConfiguration, "execution/0/running")).isEqualTo("executorName0");

		regCenter.remove(JobNodePath.getNodeFullPath(jobName, "execution/0/running"));
		Thread.sleep(1000);

		// itself take over 0 item
		assertThat(getJobNode(jobConfiguration, "execution/0/failover")).isEqualTo("executorName0");

		// wait the last finish, until the failover lifecycle begin
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, "execution/0/running"));
			}
		}, 30);

		assertThat(getJobNode(jobConfiguration, "execution/0/running")).isEqualTo("executorName0");

		// wait the failover lifecycle finish
		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				return regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, "execution/0/completed"));
			}

		}, 30);

		assertThat(getJobNode(jobConfiguration, "execution/0/completed")).isEqualTo("executorName0");

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		Thread.sleep(2000);

		LongtimeJavaJob.statusMap.clear();

		stopExecutorList();
		Thread.sleep(1000);
		forceRemoveJob(jobName);
	}

}
