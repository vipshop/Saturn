package com.vip.saturn.it.impl;

import static org.assertj.core.api.Assertions.fail;

import java.util.Collection;

import org.junit.*;
import org.junit.runners.MethodSorters;

import sun.misc.Signal;

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
		startSaturnConsoleList(1);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorList();
		stopSaturnConsoleList();
	}

	@Before
	public void before() {
		LongtimeJavaJob.statusMap.clear();
	}

	@After
	public void after() {
		LongtimeJavaJob.statusMap.clear();
	}

	@Test
	public void test_A_TERM_Signal() throws Exception {
		SystemEnvProperties.VIP_SATURN_SHUTDOWN_TIMEOUT = 10;
		startExecutorList(1);
		final int shardCount = 3;
		final String jobName = "test_A_TERM_Signal";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			LongtimeJavaJob.JobStatus status = new LongtimeJavaJob.JobStatus();
			status.runningCount = 0;
			status.sleepSeconds = 8;
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
		Thread.sleep(50);

		ShutdownHandler.exitAfterHandler(false);
		Signal.raise(new Signal("TERM"));

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

		// wait executor shutdown completely
		Thread.sleep(2000);
		forceRemoveJob(jobName);

		stopExecutorList();
	}

	@Test
	public void test_B_INT_Signal() throws Exception {
		SystemEnvProperties.VIP_SATURN_SHUTDOWN_TIMEOUT = 5;
		startExecutorList(1);
		final int shardCount = 3;
		final String jobName = "test_B_INT_Signal";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			LongtimeJavaJob.JobStatus status = new LongtimeJavaJob.JobStatus();
			status.runningCount = 0;
			status.sleepSeconds = 3;
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
		Thread.sleep(50);

		ShutdownHandler.exitAfterHandler(false);
		Signal.raise(new Signal("INT"));

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

		// wait executor shutdown completely
		Thread.sleep(2000);
		forceRemoveJob(jobName);

		stopExecutorList();

	}
}
