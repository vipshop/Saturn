package com.vip.saturn.it.impl;

import com.vip.saturn.it.base.AbstractSaturnIT;
import com.vip.saturn.it.base.FinishCheck;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.internal.sharding.ShardingNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import com.vip.saturn.job.utils.ItemUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author hebelala
 */
public class ShardingWithChangingLeaderIT extends AbstractSaturnIT {

	@BeforeClass
	public static void setUp() throws Exception {
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorListGracefully();
		stopSaturnConsoleList();
	}

	@Test
	public void test_A_StopNamespaceShardingManagerLeader() throws Exception {
		startSaturnConsoleList(2);
		Thread.sleep(1000);

		final String jobName = "test_A_StopNamespaceShardingManagerLeader";
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");

		addJob(jobConfig);
		Thread.sleep(1000);

		Main executor = startOneNewExecutorList();
		Thread.sleep(1000);

		String hostValue = regCenter.get(SaturnExecutorsNode.LEADER_HOSTNODE_PATH);
		assertThat(hostValue).isNotNull();

		stopSaturnConsole(0);
		Thread.sleep(1000);

		String hostValue2 = regCenter.get(SaturnExecutorsNode.LEADER_HOSTNODE_PATH);
		assertThat(hostValue2).isNotNull().isNotEqualTo(hostValue);

		enableJob(jobName);
		Thread.sleep(1000);
		runAtOnce(jobName);
		Thread.sleep(1000);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				if (isNeedSharding(jobName)) {
					return false;
				}
				return true;
			}
		}, 10);
		List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor.getExecutorName()))));
		assertThat(items).contains(0);

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		Thread.sleep(1000);
		stopExecutorListGracefully();
		stopSaturnConsoleList();
	}

}
