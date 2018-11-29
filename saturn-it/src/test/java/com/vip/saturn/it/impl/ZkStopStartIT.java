package com.vip.saturn.it.impl;

import com.vip.saturn.it.base.AbstractSaturnIT;
import com.vip.saturn.it.base.FinishCheck;
import com.vip.saturn.it.job.LongtimeJavaJob;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobType;
import org.junit.*;
import org.junit.runners.MethodSorters;

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

	@Test
	public void test_A_zkStopStartJobJava() throws Exception {
		final int shardCount = 3;
		final String jobName = "test_A_zkStopStartJobJava";

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

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(LongtimeJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfig);
		Thread.sleep(2000);
		enableJob(jobName);
		Thread.sleep(2000);
		runAtOnce(jobName);
		Thread.sleep(2000);
		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {

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
				public boolean isOk() {

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
