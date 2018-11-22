package com.vip.saturn.it.impl;

import com.vip.saturn.it.base.AbstractSaturnIT;
import com.vip.saturn.it.base.FinishCheck;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.internal.config.ConfigurationNode;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.server.ServerNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import org.junit.*;
import org.junit.runners.MethodSorters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeleteJobIT extends AbstractSaturnIT {

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorListGracefully();
		stopSaturnConsoleList();
	}

	@After
	public void after() throws Exception {
		stopExecutorListGracefully();
	}

	/**
	 * 多个Executor
	 */
	@Test
	public void test_A_multiExecutor() throws Exception {
		startExecutorList(3);

		int shardCount = 3;
		final String jobName = "test_A_multiExecutor";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfig);
		Thread.sleep(1000);
		enableReport(jobName);
		enableJob(jobName);
		Thread.sleep(4 * 1000);
		disableJob(jobName);
		Thread.sleep(1000);

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			assertThat(SimpleJavaJob.statusMap.get(key)).isGreaterThanOrEqualTo(1);
		}

		removeJob(jobName);

		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean isOk() {
					for (Main executor : saturnExecutorList) {
						if (executor == null) {
							continue;
						}
						if (regCenter.isExisted(ServerNode.getServerNode(jobName, executor.getExecutorName()))) {
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

	/**
	 * 只有一个Executor
	 */
	@Test
	public void test_B_oneExecutor() throws Exception {
		startOneNewExecutorList();

		final int shardCount = 3;
		final String jobName = "test_B_oneExecutor";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfig);
		Thread.sleep(1000);
		enableReport(jobName);
		enableJob(jobName);

		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {

					for (int i = 0; i < shardCount; i++) {
						String key = jobName + "_" + i;
						if (SimpleJavaJob.statusMap.get(key) < 1) {
							return false;
						}
					}
					return true;
				}

			}, 4);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		disableJob(jobName);

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

			}, 3);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		removeJob(jobName);

		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean isOk() {
					for (Main executor : saturnExecutorList) {
						if (executor == null) {
							continue;
						}
						if (regCenter.isExisted(ServerNode.getServerNode(jobName, executor.getExecutorName()))) {
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

	/**
	 * 补充删除作业时在启动Executor前有toDelete结点的IT： 如果有在启动Executor前作业有配置toDelete结点则会判断并删除$Jobs/jobName/servers/executorName
	 */
	@Test
	public void test_C_alreadyExistsToDelete() throws Exception {
		final int shardCount = 3;
		final String jobName = "test_C_alreadyExistsToDelete";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfig);
		Thread.sleep(1000);
		enableReport(jobName);

		// 使用hack的方式，直接新增toDelete结点
		zkUpdateJobNode(jobName, ConfigurationNode.TO_DELETE, "1");

		final String serverNodePath = JobNodePath.getServerNodePath(jobName, "executorName0");
		regCenter.persist(serverNodePath, "");

		startOneNewExecutorList();
		Thread.sleep(1000);
		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean isOk() {
					if (regCenter.isExisted(serverNodePath)) {
						return false;
					}
					return true;
				}

			}, 10);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		removeJob(jobName);
	}
}
