package com.vip.saturn.it.impl;

import org.apache.curator.framework.CuratorFramework;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.utils.SystemEnvProperties;

/**
 * Created by xiaopeng.he on 2016/8/22.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExecutorCleanIT extends AbstractSaturnIT {

	@BeforeClass
	public static void setUp() throws Exception {
		stopExecutorList();
		startSaturnConsoleList(1);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorList();
		stopSaturnConsoleList();
		SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = false;
	}

	private void assertDelete(final String jobName, final String executorName) throws Exception {
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				try {
					return !regCenter.isExisted(SaturnExecutorsNode.getExecutorNodePath(executorName))
							&& !regCenter
									.isExisted(SaturnExecutorsNode.getJobServersExecutorNodePath(jobName, executorName))
							&& !executorName
									.equals(regCenter.get(SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName)));
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
		}, 10);
	}

	private void assertNoDelete(final String jobName, final String executorName) throws Exception {
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				try {
					return regCenter.isExisted(SaturnExecutorsNode.getExecutorNodePath(executorName))
							&& regCenter
									.isExisted(SaturnExecutorsNode.getJobServersExecutorNodePath(jobName, executorName))
							&& executorName
									.equals(regCenter.get(SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName)));
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
		}, 10);
	}

	private void assertNoDelete2(final String jobName, final String executorName) throws Exception {
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				try {
					return regCenter.isExisted(SaturnExecutorsNode.getExecutorNodePath(executorName)) && regCenter
							.isExisted(SaturnExecutorsNode.getJobServersExecutorNodePath(jobName, executorName));
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
		}, 10);
	}

	@Test
	public void test_A_Clean() throws Exception {
		SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = true;

		startOneNewExecutorList();

		final String executorName = saturnExecutorList.get(0).getExecutorName();

		final JobConfiguration job = new JobConfiguration("test_A_Clean");
		job.setCron("0/2 * * * * ?");
		job.setJobType(JobType.JAVA_JOB.toString());
		job.setJobClass(SimpleJavaJob.class.getCanonicalName());
		job.setShardingTotalCount(1);
		job.setShardingItemParameters("0=0");
		job.setPreferList(executorName);
		addJob(job);
		Thread.sleep(1000);
		enableJob(job.getJobName());
		Thread.sleep(3 * 1000);

		stopExecutorList();

		Thread.sleep(2000);

		assertDelete(job.getJobName(), executorName);

	}

	@Test
	public void test_B_NoClean() throws Exception {
		SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = false;

		startOneNewExecutorList();

		final String executorName = saturnExecutorList.get(0).getExecutorName();

		final JobConfiguration job = new JobConfiguration("test_B_NoClean");
		job.setCron("0/2 * * * * ?");
		job.setJobType(JobType.JAVA_JOB.toString());
		job.setJobClass(SimpleJavaJob.class.getCanonicalName());
		job.setShardingTotalCount(1);
		job.setShardingItemParameters("0=0");
		job.setPreferList(executorName);
		addJob(job);
		Thread.sleep(1000);
		enableJob(job.getJobName());
		Thread.sleep(3 * 1000);

		stopExecutorList();

		Thread.sleep(1000);

		assertNoDelete(job.getJobName(), executorName);

	}

	@Test
	public void test_C_Clean_When_SessionTimeoutAndReconnect() throws Exception {
		SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = true;

		startOneNewExecutorList();

		final String executorName = saturnExecutorList.get(0).getExecutorName();

		final JobConfiguration job = new JobConfiguration("test_C_SessionTimeoutAndReconnect");
		job.setCron("0/2 * * * * ?");
		job.setJobType(JobType.JAVA_JOB.toString());
		job.setJobClass(SimpleJavaJob.class.getCanonicalName());
		job.setShardingTotalCount(1);
		job.setShardingItemParameters("0=0");
		job.setPreferList(executorName);
		addJob(job);
		Thread.sleep(1000);
		enableJob(job.getJobName());
		Thread.sleep(3 * 1000);

		killSession((CuratorFramework) getExecutorRegistryCenter(saturnExecutorList.get(0)).getRawClient());

		assertDelete(job.getJobName(), executorName);

		Thread.sleep(40 * 1000);

		assertNoDelete2(job.getJobName(), executorName);

		// stopExecutorList();
	}

}
