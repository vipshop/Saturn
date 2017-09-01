package com.vip.saturn.it.impl;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.internal.config.ConfigurationNode;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.server.ServerNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeleteJobIT extends AbstractSaturnIT {

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

	/**
	 * 全部结点存活时删除
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void test_A() throws InterruptedException {
		int shardCount = 3;
		final String jobName = "deleteITJob";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(4 * 1000);
		disableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			assertThat(SimpleJavaJob.statusMap.get(key)).isGreaterThanOrEqualTo(1);
		}

		removeJob(jobConfiguration.getJobName());
		Thread.sleep(5000);

		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean docheck() {
					if (regCenter.isExisted(JobNodePath.getJobNameFullPath(jobName))) {
						return false;
					}
					return true;
				}

			}, 30);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * 部分结点存活
	 *
	 * @throws Exception
	 */
	@Test
	public void test_B() throws Exception {
		final int shardCount = 3;
		final String jobName = "deleteITJob";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}
		stopExecutor(0);
		stopExecutor(1);
		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());

		try {
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {

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

		disableJob(jobConfiguration.getJobName());

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

			}, 3);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		removeJob(jobConfiguration.getJobName());

		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean docheck() {
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
	 *
	 * @throws Exception
	 */
	@Test
	public void test_C() throws Exception {
		final int shardCount = 3;
		final String jobName = "deleteITJob";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		stopExecutorList();
		Thread.sleep(1000);
		configJob(jobName, ConfigurationNode.TO_DELETE, 1);

		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfiguration);
		Thread.sleep(1000);

		final String serverNodePath = JobNodePath.getServerNodePath(jobName, "executorName0");
		regCenter.persist(serverNodePath, "");

		startExecutorList(1);
		Thread.sleep(1000);
		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean docheck() {
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

	}
}
