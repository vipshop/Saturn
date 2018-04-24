/**
 * vips Inc. Copyright (c) 2016 All Rights Reserved.
 */
package com.vip.saturn.it.impl;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.LongtimeJavaJob;
import com.vip.saturn.job.internal.config.JobConfiguration;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.Collection;

import static org.assertj.core.api.Assertions.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ZkStopStartIT extends AbstractSaturnIT {

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
		startExecutorList(3);
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
	public void test_A_JavaJob() throws InterruptedException, IOException {
		final int shardCount = 3;
		final String jobName = "zkStopStartJobJava";

		LongtimeJavaJob.statusMap.clear();
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			LongtimeJavaJob.JobStatus status = new LongtimeJavaJob.JobStatus();
			status.runningCount = 0;
			status.sleepSeconds = 60;
			status.finished = false;
			status.timeout = false;
			status.running = false;
			LongtimeJavaJob.statusMap.put(key, status);
		}

		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0 0 0/1 * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(LongtimeJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfiguration);
		Thread.sleep(2000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(2000);
		runAtOnce(jobName);
		Thread.sleep(2000);
		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {

					Collection<LongtimeJavaJob.JobStatus> values = LongtimeJavaJob.statusMap.values();
					for (LongtimeJavaJob.JobStatus status : values) {
						if (!status.running) {
							return false;
						}
					}
					return true;
				}

			}, 40);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		stopZkServer();

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

			}, 40);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		LongtimeJavaJob.statusMap.clear();
	}

}
