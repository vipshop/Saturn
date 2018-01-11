package com.vip.saturn.it.impl;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.internal.sharding.ShardingNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.sharding.entity.Executor;
import com.vip.saturn.job.utils.ItemUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Created by Ivy01.li on 2016/9/12.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShardingWithLoadIT extends AbstractSaturnIT {
	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopSaturnConsoleList();
	}

	// 添加指定配置和分片的SimpleJavaJob
	public void addSimpleJavaJob(String jobName, int shardCount, JobConfiguration jobConfiguration) {
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}
		jobConfiguration.setShardingTotalCount(shardCount);
		addJob(jobConfiguration);
	}

	@Test
	public void A_JavaMultiJobWithLoad() throws Exception {
		String preferList = null;
		multiJobSharding(preferList);
		stopExecutorList();
	}

	@Test
	// 仅对作业3增加优先节点1,2
	public void B_JavaMultiJobWithLoadWithPreferList() throws Exception {
		String preferList = "executorName" + "0," + "executorName" + 1;
		multiJobSharding(preferList);
		stopExecutorList();
	}

	public void multiJobSharding(String preferListJob3) throws Exception {
		// 作业1，负荷为1,分片数为2
		String jobName1 = "JOB_LOAD1_SHARDING2";
		final JobConfiguration jobConfiguration1 = new JobConfiguration(jobName1);
		jobConfiguration1.setCron("* * 1 * * ?");
		jobConfiguration1.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration1.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration1.setLoadLevel(1);
		jobConfiguration1.setShardingItemParameters("0=0,1=1,2=2");
		addSimpleJavaJob(jobName1, 2, jobConfiguration1);

		// 作业2，负荷为2,分片数为2
		String jobName2 = "JOB_LOAD2_SHARDING2";
		final JobConfiguration jobConfiguration2 = new JobConfiguration(jobName2);
		jobConfiguration2.setCron("* * 1 * * ?");
		jobConfiguration2.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration2.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration2.setLoadLevel(2);
		jobConfiguration2.setShardingItemParameters("0=0,1=1");
		addSimpleJavaJob(jobName2, 2, jobConfiguration2);

		// 作业3，负荷为1,分片数为3
		String jobName3 = "JOB_LOAD1_SHARDING3";
		final JobConfiguration jobConfiguration3 = new JobConfiguration(jobName3);
		jobConfiguration3.setCron("* * 1 * * ?");
		jobConfiguration3.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration3.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration3.setLoadLevel(1);
		jobConfiguration3.setShardingItemParameters("0=0,1=1,2=2");
		// 没有preferlist or 设置preferlist1,2
		if (null != preferListJob3) {
			jobConfiguration3.setPreferList(preferListJob3);
		}
		addSimpleJavaJob(jobName3, 3, jobConfiguration3);

		Thread.sleep(1 * 1000);
		// 启用作业1,2,3
		enableJob(jobName1);
		enableJob(jobName2);
		enableJob(jobName3);
		Thread.sleep(1 * 1000);

		Main executor1 = startOneNewExecutorList();// 启动第1台executor
		Thread.sleep(1000);
		runAtOnce(jobName1);
		runAtOnce(jobName2);
		runAtOnce(jobName3);
		Thread.sleep(1000);

		waitForFinish(new AbstractSaturnIT.FinishCheck() {

			@Override
			public boolean docheck() {
				if (isNeedSharding(jobConfiguration1) || isNeedSharding(jobConfiguration2)
						|| isNeedSharding(jobConfiguration3)) {
					return false;
				}
				return true;
			}

		}, 30);
		List<Integer> itemsJob1Exe1 = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName1, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(itemsJob1Exe1).contains(0, 1);

		List<Integer> itemsJob2Exe1 = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName2, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(itemsJob2Exe1).contains(0, 1);

		List<Integer> itemsJob3Exe1 = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName3, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(itemsJob3Exe1).contains(0, 1, 2);

		Main executor2 = startOneNewExecutorList();// 启动第2台executor
		Thread.sleep(1000);
		runAtOnce(jobName1);
		runAtOnce(jobName2);
		runAtOnce(jobName3);
		Thread.sleep(1000);
		waitForFinish(new AbstractSaturnIT.FinishCheck() {

			@Override
			public boolean docheck() {
				if (isNeedSharding(jobConfiguration1) || isNeedSharding(jobConfiguration2)
						|| isNeedSharding(jobConfiguration3)) {
					return false;
				}
				return true;
			}

		}, 60);

		// 大负荷作业Job2分片都到节点2
		List<Integer> itemsJob2Exe2 = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName2, ShardingNode.getShardingNode(executor2.getExecutorName()))));
		System.out.println("job2 at exe2 :" + itemsJob2Exe2);

		List<Integer> itemsJob1Exe2 = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName1, ShardingNode.getShardingNode(executor2.getExecutorName()))));
		System.out.println("job1 at exe2:" + itemsJob1Exe2);

		List<Integer> itemsJob3Exe2 = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName3, ShardingNode.getShardingNode(executor2.getExecutorName()))));
		System.out.println("job3 at exe2:" + itemsJob3Exe2);

		int totalLoadOfExe2 = itemsJob2Exe2.size() * 2 + itemsJob1Exe2.size() + itemsJob3Exe2.size();

		assertEquals("total load of exe2 not equal", 4, totalLoadOfExe2);

		Main executor3 = startOneNewExecutorList();// 启动第3台executor
		Thread.sleep(1000);
		runAtOnce(jobName1);
		runAtOnce(jobName2);
		runAtOnce(jobName3);
		Thread.sleep(1000);

		List<Integer> itemsJob1Exe3 = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName1, ShardingNode.getShardingNode(executor3.getExecutorName()))));
		List<Integer> itemsJob2Exe3 = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName2, ShardingNode.getShardingNode(executor3.getExecutorName()))));
		List<Integer> itemsJob3Exe3 = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName3, ShardingNode.getShardingNode(executor3.getExecutorName()))));
		System.out.println("itemsJob1Exe3 : " + itemsJob1Exe3);
		System.out.println("itemsJob2Exe3 : " + itemsJob2Exe3);
		System.out.println("itemsJob3Exe3 : " + itemsJob3Exe3);
		// 节点3应该有分片
		Assert.assertFalse("The exe3 has no sharding. ",
				itemsJob1Exe3.isEmpty() && itemsJob2Exe3.isEmpty() && itemsJob3Exe3.isEmpty());

		// 当job3设置优先节点的情况下
		if (null != preferListJob3) {
			// 节点3不应该有job3的分片
			Assert.assertTrue("The exe3 should have no sharding of job3. ", itemsJob3Exe3.isEmpty());
		}

		// 停节点1前先获取节点1上所有作业分片
		itemsJob1Exe1 = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName1, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		itemsJob2Exe1 = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName2, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		itemsJob3Exe1 = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName3, ShardingNode.getShardingNode(executor1.getExecutorName()))));

		stopExecutor(0); // 停第1个executor
		Thread.sleep(1000);
		runAtOnce(jobName1);
		runAtOnce(jobName2);
		runAtOnce(jobName3);
		Thread.sleep(1000);
		waitForFinish(new AbstractSaturnIT.FinishCheck() {

			@Override
			public boolean docheck() {
				if (isNeedSharding(jobConfiguration1) || isNeedSharding(jobConfiguration2)
						|| isNeedSharding(jobConfiguration3)) {
					return false;
				}
				return true;
			}

		}, 10);

		if (!itemsJob1Exe1.isEmpty()) {
			itemsJob1Exe2 = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName1, ShardingNode.getShardingNode(executor2.getExecutorName()))));
			itemsJob1Exe3 = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName1, ShardingNode.getShardingNode(executor3.getExecutorName()))));
			for (Integer i : itemsJob1Exe1) {
				Assert.assertTrue("the sharding of exe1 should be shift to exe2 or exe3",
						itemsJob1Exe2.contains(i) || itemsJob1Exe3.contains(i));
			}
		}

		if (!itemsJob2Exe1.isEmpty()) {
			itemsJob2Exe2 = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName2, ShardingNode.getShardingNode(executor2.getExecutorName()))));
			itemsJob2Exe3 = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName2, ShardingNode.getShardingNode(executor3.getExecutorName()))));
			for (Integer i : itemsJob2Exe1) {
				Assert.assertTrue("the sharding of exe1 should be shift to exe2 or exe3",
						itemsJob2Exe2.contains(i) || itemsJob2Exe3.contains(i));
			}
		}

		if (!itemsJob3Exe1.isEmpty()) {
			itemsJob3Exe2 = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName3, ShardingNode.getShardingNode(executor2.getExecutorName()))));
			itemsJob3Exe3 = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName3, ShardingNode.getShardingNode(executor3.getExecutorName()))));
			for (Integer i : itemsJob3Exe1) {
				Assert.assertTrue("the sharding of exe1 should be shift to exe2 or exe3",
						itemsJob3Exe2.contains(i) || itemsJob3Exe3.contains(i));
			}
		}

		disableJob(jobName1);
		disableJob(jobName2);
		disableJob(jobName3);
		Thread.sleep(1000);
		removeJob(jobName1);
		removeJob(jobName2);
		removeJob(jobName3);

		Thread.sleep(1000);
		stopExecutorList();
		Thread.sleep(2000);
		forceRemoveJob(jobName1);
		forceRemoveJob(jobName2);
		forceRemoveJob(jobName3);
	}
}
