/**
 * vips Inc. Copyright (c) 2016 All Rights Reserved.
 */
package com.vip.saturn.it.impl;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.LongtimeJavaJob;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.internal.server.ServerNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import org.junit.*;
import org.junit.runners.MethodSorters;

import static org.assertj.core.api.Assertions.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunAtOnceJobIT extends AbstractSaturnIT {

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
		SimpleJavaJob.statusMap.clear();
	}

	@After
	public void after() {
		LongtimeJavaJob.statusMap.clear();
		SimpleJavaJob.statusMap.clear();
	}

	/**
	 * 作业STOPPING时立即强制终止
	 * @throws InterruptedException
	 */
	@Test
	public void test_C_normalTrigger() throws InterruptedException {
		final int shardCount = 3;
		final String jobName = "runAtOnceITJob";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("* * 1 * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setTimeoutSeconds(0);
		jobConfiguration.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		runAtOnce(jobName);
		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean docheck() {
					for (int i = 0; i < shardCount; i++) {
						String key = jobName + "_" + i;
						if (SimpleJavaJob.statusMap.get(key) != 1) {
							return false;
						}
					}
					return true;
				}

			}, 30);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		SimpleJavaJob.statusMap.clear();
	}

	@Test
	public void test_B_ignoreWhenIsRunning() throws InterruptedException {
		final int shardCount = 1;
		final String jobName = "runAtOnceITJob2";
		LongtimeJavaJob.JobStatus status = new LongtimeJavaJob.JobStatus();
		status.runningCount = 0;
		status.sleepSeconds = 3;
		status.finished = false;
		status.timeout = false;
		LongtimeJavaJob.statusMap.put(jobName + "_" + 0, status);
		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0 0 1 * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(LongtimeJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setTimeoutSeconds(0);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		runAtOnce(jobName);
		Thread.sleep(1000);
		// suppose to be ignored.
		runAtOnce(jobName);

		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {

					String path = JobNodePath.getNodeFullPath(jobName,
							String.format(ServerNode.RUNONETIME, "executorName0"));
					if (regCenter.isExisted(path)) {
						return false;
					}

					return true;
				}

			}, 20);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {

					if (LongtimeJavaJob.statusMap.get(jobName + "_" + 0).runningCount < 1) {
						return false;
					}
					return true;
				}

			}, 30);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		LongtimeJavaJob.statusMap.clear();

	}
}
