package com.vip.saturn.it.impl;

import static org.assertj.core.api.Assertions.fail;

import java.util.Collection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.LongtimeJavaJob;
import com.vip.saturn.job.basic.ShutdownHandler;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.utils.SystemEnvProperties;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShutdownGracefullyIT extends AbstractSaturnIT {

	@BeforeClass
	public static void setUp() throws Exception {
		startNamespaceShardingManagerList(1);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		// stopExecutorList();
		stopNamespaceShardingManagerList();
	}

	@Test
	public void test_A_JavaJob() throws Exception {
		SystemEnvProperties.VIP_SATURN_SHUTDOWN_TIMEOUT = 40;
		startExecutorList(1);
		final int shardCount = 3;
		final String jobName = "timeoutITJobJava";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			LongtimeJavaJob.JobStatus status = new LongtimeJavaJob.JobStatus();
			status.runningCount = 0;
			status.sleepSeconds = 30;
			status.finished = false;
			status.timeout = false;
			LongtimeJavaJob.statusMap.put(key, status);
		}

		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0 0 1 * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(LongtimeJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		runAtOnce(jobName);

		new ShutdownHandler(false).handle(null);

		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {

					Collection<LongtimeJavaJob.JobStatus> values = LongtimeJavaJob.statusMap.values();
					for (LongtimeJavaJob.JobStatus status : values) {
						if (!status.finished) {
							return false;
						}
					}
					return true;
				}

			}, 10);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void test_B_JavaJob() throws Exception {
		SystemEnvProperties.VIP_SATURN_SHUTDOWN_TIMEOUT = 70;
		startExecutorList(1);
		final int shardCount = 3;
		final String jobName = "gracefulShutdownItJobJava";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			LongtimeJavaJob.JobStatus status = new LongtimeJavaJob.JobStatus();
			status.runningCount = 0;
			status.sleepSeconds = 62;
			status.finished = false;
			status.timeout = false;
			LongtimeJavaJob.statusMap.put(key, status);
		}

		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0 0 1 * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(LongtimeJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		runAtOnce(jobName);

		new ShutdownHandler(false).handle(null);

		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {

					Collection<LongtimeJavaJob.JobStatus> values = LongtimeJavaJob.statusMap.values();
					for (LongtimeJavaJob.JobStatus status : values) {
						if (!status.finished) {
							return false;
						}
					}
					return true;
				}

			}, 5);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}
}
