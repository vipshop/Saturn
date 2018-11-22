package com.vip.saturn.it.impl;

import com.vip.saturn.it.base.AbstractSaturnIT;
import com.vip.saturn.it.base.FinishCheck;
import com.vip.saturn.it.job.LongtimeJavaJob;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.utils.ScriptPidUtils;
import org.apache.commons.exec.OS;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TimeoutJobIT extends AbstractSaturnIT {
	public static String LONG_TIME_SH_PATH;

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
		startExecutorList(3);

		File file1 = new File("src/test/resources/script/normal/longtime.sh");
		LONG_TIME_SH_PATH = file1.getAbsolutePath();

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
	public void test_A_JavaJob() throws Exception {
		final int shardCount = 3;
		final String jobName = "test_A_JavaJob";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			LongtimeJavaJob.JobStatus status = new LongtimeJavaJob.JobStatus();
			status.runningCount = 0;
			status.sleepSeconds = 30;
			status.finished = false;
			status.timeout = false;
			status.beforeTimeout = false;
			LongtimeJavaJob.statusMap.put(key, status);
		}

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(LongtimeJavaJob.class.getCanonicalName());
		jobConfig.setTimeoutSeconds(3);
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfig);
		Thread.sleep(1000);
		enableJob(jobConfig.getJobName());
		Thread.sleep(1000);
		runAtOnce(jobName);

		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {

					Collection<LongtimeJavaJob.JobStatus> values = LongtimeJavaJob.statusMap.values();
					for (LongtimeJavaJob.JobStatus status : values) {
						if (!status.finished || !status.timeout || !status.beforeTimeout) {
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

		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {

					for (int j = 0; j < shardCount; j++) {
						if (!regCenter
								.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getTimeoutNode(j)))) {
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

		Thread.sleep(1000);
		for (int j = 0; j < shardCount; j++) {
			String key = jobName + "_" + j;
			LongtimeJavaJob.JobStatus status = LongtimeJavaJob.statusMap.get(key);
			assertThat(status.runningCount).isEqualTo(0);
			assertThat(regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getTimeoutNode(j))))
					.isEqualTo(true);
			assertThat(regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getCompletedNode(j))))
					.isEqualTo(true);
		}

		disableJob(jobConfig.getJobName());
		Thread.sleep(1000);
		removeJob(jobConfig.getJobName());
		LongtimeJavaJob.statusMap.clear();
	}

	@Test
	public void test_B_shJob() throws Exception {
		// bacause ScriptPidUtils.isPidRunning don't support mac
		if (!OS.isFamilyUnix() || OS.isFamilyMac()) {
			return;
		}
		final int shardCount = 3;
		final String jobName = "test_B_shJob";

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.SHELL_JOB.toString());
		jobConfig.setJobClass(LongtimeJavaJob.class.getCanonicalName());
		jobConfig.setTimeoutSeconds(3);
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setShardingItemParameters(
				"0=sh " + LONG_TIME_SH_PATH + ",1=sh " + LONG_TIME_SH_PATH + ",2=sh " + LONG_TIME_SH_PATH);
		addJob(jobConfig);
		Thread.sleep(1000);
		enableJob(jobConfig.getJobName());
		Thread.sleep(1000);
		runAtOnce(jobName);

		// wait executor receive the runAtOnce event
		Thread.sleep(500L);

		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean isOk() {

					for (int i = 0; i < saturnExecutorList.size(); i++) {
						Main saturnContainer = saturnExecutorList.get(i);
						for (int j = 0; j < shardCount; j++) {
							long pid = ScriptPidUtils
									.getFirstPidFromFile(saturnContainer.getExecutorName(), jobName, "" + j);
							if (pid > 0 && ScriptPidUtils.isPidRunning(pid)) {
								return false;
							}
						}
					}

					return true;
				}

			}, 10);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {

					for (int j = 0; j < shardCount; j++) {
						if (!regCenter
								.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getCompletedNode(j)))) {
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

		disableJob(jobConfig.getJobName());
		Thread.sleep(1000);
		removeJob(jobConfig.getJobName());
		LongtimeJavaJob.statusMap.clear();
	}
}
