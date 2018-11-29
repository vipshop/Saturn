package com.vip.saturn.it.impl;

import com.vip.saturn.it.base.AbstractSaturnIT;
import com.vip.saturn.it.base.FinishCheck;
import com.vip.saturn.it.job.LongtimeJavaJob;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.sharding.ShardingNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.utils.ItemUtils;
import org.assertj.core.api.Condition;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author hebelala
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShardingWithTrafficIT extends AbstractSaturnIT {

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorListGracefully();
		stopSaturnConsoleList();
	}

	/**
	 * 一般流量摘取和恢复流程：<br> 两个启用状态的作业，两台机A、B；<br> 摘取B的流量，结果B的分片被分配到A；<br> 下线B，分片分配依然不变；<br> 上线B，分配分配依然不变；<br>
	 * 恢复B的流量，结果平均分配分片到A、B。
	 */
	@Test
	public void test_A_NormalFlow() throws Exception {
		String jobName = "test_A_NormalFlow";
		String jobName2 = "test_A_NormalFlow2";

		final JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(2);
		jobConfig.setShardingItemParameters("0=0,1=1");

		final JobConfig jobConfig2 = new JobConfig();
		jobConfig2.setJobName(jobName2);
		jobConfig2.setCron("9 9 9 9 9 ? 2099");
		jobConfig2.setJobType(JobType.JAVA_JOB.toString());
		jobConfig2.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig2.setShardingTotalCount(2);
		jobConfig2.setShardingItemParameters("0=0,1=1");

		addJob(jobConfig);
		Thread.sleep(1000L);

		addJob(jobConfig2);
		Thread.sleep(1000L);

		enableJob(jobName);
		Thread.sleep(1000L);

		enableJob(jobName2);
		Thread.sleep(1000L);

		Main executor1 = startOneNewExecutorList();
		String executorName1 = executor1.getExecutorName();

		Main executor2 = startOneNewExecutorList();
		String executorName2 = executor2.getExecutorName();

		runAtOnceAndWaitShardingCompleted(jobName);
		runAtOnceAndWaitShardingCompleted(jobName2);
		isItemsBalanceOk(jobName, jobName2, executorName1, executorName2);

		extractTraffic(executorName2);
		Thread.sleep(1000L);

		runAtOnceAndWaitShardingCompleted(jobName);
		runAtOnceAndWaitShardingCompleted(jobName2);
		isItemsToExecutor1(jobName, jobName2, executorName1, executorName2);

		stopExecutorGracefully(1);
		Thread.sleep(1000L);

		runAtOnceAndWaitShardingCompleted(jobName);
		runAtOnceAndWaitShardingCompleted(jobName2);
		isItemsToExecutor1(jobName, jobName2, executorName1, executorName2);

		executor2 = startExecutor(1);
		executorName2 = executor2.getExecutorName();

		runAtOnceAndWaitShardingCompleted(jobName);
		runAtOnceAndWaitShardingCompleted(jobName2);
		isItemsToExecutor1(jobName, jobName2, executorName1, executorName2);

		recoverTraffic(executorName2);
		Thread.sleep(1000L);

		runAtOnceAndWaitShardingCompleted(jobName);
		runAtOnceAndWaitShardingCompleted(jobName2);
		isItemsBalanceOk(jobName, jobName2, executorName1, executorName2);

		// 清理，不影响其他Test
		disableJob(jobName);
		disableJob(jobName2);
		Thread.sleep(1000L);
		removeJob(jobName);
		removeJob(jobName2);
		Thread.sleep(1000L);
		stopExecutorListGracefully();
	}

	private void isItemsBalanceOk(String jobName, String jobName2, String executorName1, String executorName2) {
		List<Integer> itemsJ1E1 = ItemUtils.toItemList(regCenter
				.getDirectly(JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executorName1))));
		List<Integer> itemsJ1E2 = ItemUtils.toItemList(regCenter
				.getDirectly(JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executorName2))));
		List<Integer> itemsJ2E1 = ItemUtils.toItemList(regCenter
				.getDirectly(JobNodePath.getNodeFullPath(jobName2, ShardingNode.getShardingNode(executorName1))));
		List<Integer> itemsJ2E2 = ItemUtils.toItemList(regCenter
				.getDirectly(JobNodePath.getNodeFullPath(jobName2, ShardingNode.getShardingNode(executorName2))));

		List<Integer> allItems = new ArrayList<>();
		allItems.addAll(itemsJ1E1);
		allItems.addAll(itemsJ2E1);
		assertThat(allItems).hasSize(2);
		allItems.clear();
		allItems.addAll(itemsJ1E2);
		allItems.addAll(itemsJ2E2);
		assertThat(allItems).hasSize(2);
		allItems.clear();
		allItems.addAll(itemsJ1E1);
		allItems.addAll(itemsJ2E1);
		allItems.addAll(itemsJ1E2);
		allItems.addAll(itemsJ2E2);
		assertThat(allItems).hasSize(4).haveExactly(2, new Condition<Integer>() {
			@Override
			public boolean matches(Integer value) {
				return value == 0;
			}
		}).haveExactly(2, new Condition<Integer>() {
			@Override
			public boolean matches(Integer value) {
				return value == 1;
			}
		});
	}

	private void isItemsToExecutor1(String jobName, String jobName2, String executorName1, String executorName2) {
		List<Integer> itemsJ1E1 = ItemUtils.toItemList(regCenter
				.getDirectly(JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executorName1))));
		List<Integer> itemsJ1E2 = ItemUtils.toItemList(regCenter
				.getDirectly(JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executorName2))));
		List<Integer> itemsJ2E1 = ItemUtils.toItemList(regCenter
				.getDirectly(JobNodePath.getNodeFullPath(jobName2, ShardingNode.getShardingNode(executorName1))));
		List<Integer> itemsJ2E2 = ItemUtils.toItemList(regCenter
				.getDirectly(JobNodePath.getNodeFullPath(jobName2, ShardingNode.getShardingNode(executorName2))));

		List<Integer> allItems = new ArrayList<>();
		allItems.addAll(itemsJ1E1);
		allItems.addAll(itemsJ2E1);
		assertThat(allItems).hasSize(4).haveExactly(2, new Condition<Integer>() {
			@Override
			public boolean matches(Integer value) {
				return value == 0;
			}
		}).haveExactly(2, new Condition<Integer>() {
			@Override
			public boolean matches(Integer value) {
				return value == 1;
			}
		});
		allItems.clear();
		allItems.addAll(itemsJ1E2);
		allItems.addAll(itemsJ2E2);
		assertThat(allItems).isEmpty();
	}

	@Test
	public void test_B_NoTrafficNotTakeOverFailoverShard() throws Exception {
		final String jobName = "test_B_NoTrafficNotTakeOverFailoverShard";

		LongtimeJavaJob.JobStatus status = new LongtimeJavaJob.JobStatus();
		status.runningCount = 0;
		status.sleepSeconds = 10;
		status.finished = false;
		status.timeout = false;
		LongtimeJavaJob.statusMap.put(jobName + "_0", status);

		final JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(LongtimeJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");

		addJob(jobConfig);
		Thread.sleep(1000L);

		enableJob(jobName);
		Thread.sleep(1000L);

		startOneNewExecutorList();

		Main executor2 = startOneNewExecutorList();
		String executorName2 = executor2.getExecutorName();

		extractTraffic(executorName2);
		Thread.sleep(1000L);

		runAtOnceAndWaitShardingCompleted(jobName);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return regCenter.isExisted(JobNodePath.getNodeFullPath(jobName, ExecutionNode.getRunningNode(0)));
			}
		}, 4);

		stopExecutor(0);
		Thread.sleep(100L);

		List<String> items = regCenter.getChildrenKeys(JobNodePath.getNodeFullPath(jobName, "leader/failover/items"));
		assertThat(items).isNotNull().containsOnly("0");

		assertThat(!isFailoverAssigned(jobName, 0));

		LongtimeJavaJob.statusMap.clear();

		disableJob(jobName);
		Thread.sleep(1000L);
		removeJob(jobName);
		Thread.sleep(1000L);
		stopExecutorListGracefully();
	}
}

