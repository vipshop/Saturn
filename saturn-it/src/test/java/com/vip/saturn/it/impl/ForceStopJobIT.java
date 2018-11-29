/**
 * vips Inc. Copyright (c) 2016 All Rights Reserved.
 */
package com.vip.saturn.it.impl;

import com.vip.saturn.it.base.AbstractSaturnIT;
import com.vip.saturn.it.base.FinishCheck;
import com.vip.saturn.it.job.LongtimeJavaJob;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author yangjuanying
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ForceStopJobIT extends AbstractSaturnIT {

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
		startExecutorList(3);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorListGracefully();
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

	/**
	 * 作业STOPPING时立即强制终止
	 */
	@Test
	public void test_A_forceStopITJob() throws Exception {
		final int shardCount = 3;
		final String jobName = "test_A_forceStopITJob";

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			LongtimeJavaJob.JobStatus status = new LongtimeJavaJob.JobStatus();
			status.runningCount = 0;
			status.sleepSeconds = 60;
			status.finished = false;
			status.beforeTimeout = false;
			status.timeout = false;
			LongtimeJavaJob.statusMap.put(key, status);
		}

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(LongtimeJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setTimeoutSeconds(0);
		jobConfig.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfig);
		Thread.sleep(1000);
		enableJob(jobName);
		Thread.sleep(1000);
		runAtOnce(jobName);
		Thread.sleep(2000);
		disableJob(jobName);
		Thread.sleep(1000);
		forceStopJob(jobName);
		Thread.sleep(1000);

		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean isOk() {
					Collection<LongtimeJavaJob.JobStatus> values = LongtimeJavaJob.statusMap.values();
					for (LongtimeJavaJob.JobStatus status : values) {
						if (!status.interrupted) {
							return false;
						}
						if (status.beforeKilled != 1) {
							return false;
						}
						if (!status.finished || status.killed != 2) {
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

		for (int j = 0; j < shardCount; j++) {
			String key = jobName + "_" + j;
			LongtimeJavaJob.JobStatus status = LongtimeJavaJob.statusMap.get(key);
			assertThat(status.runningCount).isEqualTo(0);

			String path = JobNodePath.getNodeFullPath(jobName, ExecutionNode.getCompletedNode(j));
			assertThat(regCenter.isExisted(path)).isEqualTo(true);
		}

		enableJob(jobName);
		Thread.sleep(2000);

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			LongtimeJavaJob.JobStatus status = new LongtimeJavaJob.JobStatus();
			status.runningCount = 0;
			status.sleepSeconds = 1;
			status.finished = false;
			status.beforeTimeout = false;
			status.timeout = false;
			LongtimeJavaJob.statusMap.put(key, status);
		}
		runAtOnce(jobName);
		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean isOk() {
					for (int i = 0; i < shardCount; i++) {
						String key = jobName + "_" + i;
						LongtimeJavaJob.JobStatus status = LongtimeJavaJob.statusMap.get(key);
						if (status.runningCount == 0) {
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

		LongtimeJavaJob.statusMap.clear();
	}
}
