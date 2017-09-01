package com.vip.saturn.it.impl;

import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.util.Random;

import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.utils.ScriptPidUtils;
import org.apache.commons.exec.OS;
import org.junit.*;
import org.junit.runners.MethodSorters;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.job.internal.config.JobConfiguration;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScriptJobIT extends AbstractSaturnIT {
	public static String NORMAL_SH_PATH;
	public static String LONG_TIME_SH_PATH;

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);

		NORMAL_SH_PATH = new File("src/test/resources/script/normal/normal_0.sh").getAbsolutePath();
		LONG_TIME_SH_PATH = new File("src/test/resources/script/normal/longtime.sh").getAbsolutePath();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorList();
		stopSaturnConsoleList();
	}

	@After
	public void after() throws Exception {
		stopExecutorList();
	}

	@Test
	public void test_A_Normalsh() throws Exception {
		if (!OS.isFamilyUnix()) {
			return;
		}
		startExecutorList(1);
		String jobName = "test_A_Normalsh";
		final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("*/4 * * * * ?");
		jobConfiguration.setJobType(JobType.SHELL_JOB.toString());
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setProcessCountIntervalSeconds(1);
		jobConfiguration.setShardingItemParameters("0=sh " + NORMAL_SH_PATH);
		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobName);
		Thread.sleep(1000);

		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean docheck() {
					String count = getJobNode(jobConfiguration,
							"servers/" + saturnExecutorList.get(0).getExecutorName() + "/processSuccessCount");
					log.info("success count: {}", count);
					int cc = Integer.parseInt(count);
					if (cc > 0) {
						return true;
					}
					return false;
				}
			}, 15);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		Thread.sleep(2000);
		forceRemoveJob(jobName);
	}

	/**
	 * 作业启用状态，关闭Executor，将强停作业
	 */
	@Test
	public void test_B_ForceStop() throws Exception {
		if (!OS.isFamilyUnix()) {
			return;
		}

		final int shardCount = 3;
		final String jobName = "test_B_ForceStop_" + new Random().nextInt(100); // 避免多个IT同时跑该作业

		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0 0 1 * * ?");
		jobConfiguration.setJobType(JobType.SHELL_JOB.toString());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters(
				"0=sh " + LONG_TIME_SH_PATH + ",1=sh " + LONG_TIME_SH_PATH + ",2=sh " + LONG_TIME_SH_PATH);

		addJob(jobConfiguration);
		Thread.sleep(1000);
		startOneNewExecutorList(); // 将会删除该作业的一些pid垃圾数据
		Thread.sleep(1000);
		final String executorName = saturnExecutorList.get(0).getExecutorName();
		enableJob(jobName);
		Thread.sleep(1000);
		runAtOnce(jobName);
		Thread.sleep(2000);
		stopExecutor(0);

		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean docheck() {

					for (int j = 0; j < shardCount; j++) {
						long pid = ScriptPidUtils.getFirstPidFromFile(executorName, jobName, "" + j);
						if (pid > 0 && ScriptPidUtils.isPidRunning(pid)) {
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

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		Thread.sleep(2000);
		forceRemoveJob(jobName);
	}

	/**
	 * 作业禁用状态，关闭Executor，分片进程不强杀。 下次启动Executor，将其正在运行分片，重新持久化分片状态（running节点），并监听其状态（运行完，删除running节点，持久化completed节点）
	 */
	@Test
	public void test_C_ReuseItem() throws Exception {
		if (!OS.isFamilyUnix()) {
			return;
		}

		final int shardCount = 3;
		final String jobName = "test_C_ReuseItem" + new Random().nextInt(100); // 避免多个IT同时跑该作业

		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0 0 1 * * ?");
		jobConfiguration.setJobType(JobType.SHELL_JOB.toString());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters(
				"0=sh " + LONG_TIME_SH_PATH + ",1=sh " + LONG_TIME_SH_PATH + ",2=sh " + LONG_TIME_SH_PATH);

		addJob(jobConfiguration);
		Thread.sleep(1000);
		startOneNewExecutorList(); // 将会删除该作业的一些pid垃圾数据
		Thread.sleep(1000);
		final String executorName = saturnExecutorList.get(0).getExecutorName();
		enableJob(jobName);
		Thread.sleep(1000);
		runAtOnce(jobName);
		Thread.sleep(1000);
		disableJob(jobName);
		Thread.sleep(1000);
		stopExecutor(0);

		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isOnline(executorName);
				}

			}, 10);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		for (int j = 0; j < shardCount; j++) {
			long pid = ScriptPidUtils.getFirstPidFromFile(executorName, jobName, "" + j);
			if (pid < 0 || !ScriptPidUtils.isPidRunning(pid)) {
				fail("item " + j + ", pid " + pid + " should running");
			}
		}

		startOneNewExecutorList();
		Thread.sleep(2000);

		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {

					for (int j = 0; j < shardCount; j++) {
						if (!regCenter
								.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getRunningNode(j)))) {
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

		disableJob(jobName);
		Thread.sleep(1000);

		forceStopJob(jobName);
		Thread.sleep(1000);

		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {

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

		removeJob(jobName);
		Thread.sleep(2000);
		forceRemoveJob(jobName);
	}

}
