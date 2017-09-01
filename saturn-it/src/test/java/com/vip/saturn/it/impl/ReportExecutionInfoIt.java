package com.vip.saturn.it.impl;

import static org.assertj.core.api.Assertions.assertThat;

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
public class ReportExecutionInfoIt extends AbstractSaturnIT {

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
		startExecutorList(1);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorList();
		stopSaturnConsoleList();
	}

	@Test
	public void test_Report() throws Exception {
		final JobConfiguration jobConfiguration = new JobConfiguration("reportJob");
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
		forceRemoveJob(jobConfiguration.getJobName());
	}

}
