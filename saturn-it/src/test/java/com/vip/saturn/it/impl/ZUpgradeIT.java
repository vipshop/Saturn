/**
 * vips Inc. Copyright (c) 2016 All Rights Reserved.
 */
package com.vip.saturn.it.impl;

import static org.assertj.core.api.Assertions.fail;

import java.io.File;

import org.apache.commons.exec.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.SaturnAutoBasic.FinishCheck;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.utils.ScriptPidUtils;

public class ZUpgradeIT extends AbstractSaturnIT {
	public static String LONG_TIME_SH_PATH;

	@BeforeClass
	public static void setUp() throws Exception {
		startNamespaceShardingManagerList(1);

		File file1 = new File("src/test/resources/script/normal/longtime.sh");
		LONG_TIME_SH_PATH = file1.getAbsolutePath();

	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorList();
		stopNamespaceShardingManagerList();
	}

	@Test
	public void test_A() throws Exception {
		if (!OS.isFamilyUnix()) {
			return;
		}
		startOneNewExecutorList();

		final int shardCount = 3;
		final String jobName = "upgradeITJob";

		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0 0 1 * * ?");
		jobConfiguration.setJobType(JobType.SHELL_JOB.toString());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters(
				"0=sh " + LONG_TIME_SH_PATH + ",1=sh " + LONG_TIME_SH_PATH + ",2=sh " + LONG_TIME_SH_PATH);

		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		runAtOnce(jobName);
		Thread.sleep(2000);
		stopExecutor(0);

		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean docheck() {

					for (int i = 0; i < saturnExecutorList.size(); i++) {
						Main saturnContainer = saturnExecutorList.get(i);
						for (int j = 0; j < shardCount; j++) {
							long pid = ScriptPidUtils.getFirstPidFromFile(saturnContainer.getExecutorName(), jobName, ""+j);
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

		removeJob(jobConfiguration.getJobName());

		Thread.sleep(2000);
		forceRemoveJob(jobName);
	}

	@Test
	public void test_B() throws Exception {
		if (!OS.isFamilyUnix()) {
			return;
		}
		startOneNewExecutorList();
		final int shardCount = 3;
		final String jobName = "upgradeITJob2";

		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0 0 1 * * ?");
		jobConfiguration.setJobType(JobType.SHELL_JOB.toString());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters(
				"0=sh " + LONG_TIME_SH_PATH + ",1=sh " + LONG_TIME_SH_PATH + ",2=sh " + LONG_TIME_SH_PATH);

		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		runAtOnce(jobName);
		Thread.sleep(1000);
		disableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		stopExecutor(0);

		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean docheck() {

					for (int i = 0; i < saturnExecutorList.size(); i++) {
						Main saturnContainer = saturnExecutorList.get(i);
						if (saturnContainer != null && isOnline(saturnContainer.getExecutorName())) {
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

		for (int i = 0; i < saturnExecutorList.size(); i++) {
			Main saturnContainer = saturnExecutorList.get(i);
			for (int j = 0; j < shardCount; j++) {
				long pid = ScriptPidUtils.getFirstPidFromFile(saturnContainer.getExecutorName(), jobName, ""+j);
				if (pid <= 0 || !ScriptPidUtils.isPidRunning(pid)) {
					fail(pid + "should running");
				}
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

		disableJob(jobConfiguration.getJobName());
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
		
		removeJob(jobConfiguration.getJobName());

		Thread.sleep(2000);
		forceRemoveJob(jobName);
	}
}
