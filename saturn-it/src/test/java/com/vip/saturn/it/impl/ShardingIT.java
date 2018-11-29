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
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.curator.framework.CuratorFramework;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ShardingIT extends AbstractSaturnIT {

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorListGracefully();
		stopSaturnConsoleList();
	}

	@Test
	public void test_A_JAVA() throws Exception {
		int shardCount = 3;
		final String jobName = "test_A_JAVA";

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setShardingItemParameters("0=0,1=1,2=2");

		addJob(jobConfig);
		Thread.sleep(1000);
		enableJob(jobName);
		Thread.sleep(1000);

		Main executor1 = startOneNewExecutorList();// 启动第1台executor
		runAtOnce(jobName);
		Thread.sleep(1000);

		assertThat(regCenter.getDirectly(SaturnExecutorsNode.SHARDING_COUNT_PATH)).isEqualTo("4");

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
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(items).contains(0, 1, 2);

		Main executor2 = startOneNewExecutorList();// 启动第2台executor
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
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(items).isNotEmpty();
		System.out.println(items);

		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
		System.out.println(items);
		assertThat(items).isNotEmpty();

		Main executor3 = startOneNewExecutorList();// 启动第3台executor
		Thread.sleep(1000);
		runAtOnce(jobName);
		Thread.sleep(1000);

		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor3.getExecutorName()))));
		System.out.println(items);
		assertThat(items).hasSize(1);

		stopExecutorGracefully(0); // 停第1个executor

		Thread.sleep(1000);
		assertThat(regCenter.getDirectly(SaturnExecutorsNode.SHARDING_COUNT_PATH)).isEqualTo("10");

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
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		System.out.println(items);
		assertThat(items).isEmpty();

		stopExecutorGracefully(1); // 停第2个executor
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
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
		System.out.println(items);
		assertThat(items).isEmpty();

		// 分片全部落到第3个executor
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor3.getExecutorName()))));
		assertThat(items).contains(0, 1, 2);

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		stopExecutorListGracefully();
	}

	@Test
	public void test_D_PreferList() throws Exception {
		Main executor1 = startOneNewExecutorList();// 启动第1台executor

		int shardCount = 3;
		final String jobName = "test_D_PreferList";

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setShardingItemParameters("0=0,1=1,2=2");
		jobConfig.setPreferList(executor1.getExecutorName());
		addJob(jobConfig);
		Thread.sleep(1000);
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
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(items).contains(0, 1, 2);

		Main executor2 = startOneNewExecutorList();// 启动第2台executor
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
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(items).contains(0, 1, 2);

		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
		System.out.println(items);
		assertThat(items).isEmpty();

		stopExecutorGracefully(0); // 停第1个executor
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
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		System.out.println(items);
		assertThat(items).isEmpty();

		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
		System.out.println(items);
		assertThat(items).contains(0, 1, 2);

		// 再次启动第一个executor
		startExecutor(0);
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

		// 分片会重新回到executor1上
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		log.info("sharding at executor1 {}: ", items);
		assertThat(items).contains(0, 1, 2);

		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
		log.info("sharding at executor2 {}: ", items);
		assertThat(items).isEmpty();

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		stopExecutorListGracefully();
	}

	@Test
	public void test_E_PreferListOnly() throws Exception {
		Main executor1 = startOneNewExecutorList();// 启动第1台executor

		int shardCount = 3;
		final String jobName = "test_E_PreferListOnly";

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setShardingItemParameters("0=0,1=1,2=2");
		jobConfig.setPreferList(executor1.getExecutorName());
		jobConfig.setUseDispreferList(false);

		addJob(jobConfig);
		Thread.sleep(1000);
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
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(items).contains(0, 1, 2);

		Main executor2 = startOneNewExecutorList();// 启动第2台executor
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
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(items).contains(0, 1, 2);

		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
		System.out.println(items);
		assertThat(items).isEmpty();

		stopExecutorGracefully(0); // 停第1个executor
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
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		System.out.println(items);
		assertThat(items).isEmpty();

		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
		System.out.println(items);
		assertThat(items).isEmpty();

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		stopExecutorListGracefully();
	}

	/**
	 * 本地模式作业，配置了preferList，并且useDispreferList为false，则只有preferList能得到该作业分片
	 */
	@Test
	public void test_F_LocalModeWithPreferList() throws Exception {
		Main executor1 = startOneNewExecutorList();
		Main executor2 = startOneNewExecutorList();

		int shardCount = 2;
		final String jobName = "test_F_LocalModeWithPreferList";

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setShardingItemParameters("*=0");
		jobConfig.setLocalMode(true);
		jobConfig.setPreferList(executor2.getExecutorName()); // 设置preferList为executor2
		jobConfig.setUseDispreferList(false); // 设置useDispreferList为false

		addJob(jobConfig);
		Thread.sleep(1000);
		enableJob(jobName);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return isNeedSharding(jobName);
			}
		}, 10);
		runAtOnce(jobName);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return !isNeedSharding(jobName);
			}
		}, 10);
		List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
		assertThat(items).contains(0);
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(items).isEmpty();
		// wait running completed
		Thread.sleep(1000);
		// executor2下线
		stopExecutorGracefully(1);
		Thread.sleep(1000);
		// 等待sharding分片完成
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return isNeedSharding(jobName);
			}
		}, 10);
		runAtOnce(jobName);
		// 等待拿走分片
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return !isNeedSharding(jobName);
			}
		}, 10);
		// executor1仍然获取不到分片
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(items).isEmpty();

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		stopExecutorListGracefully();
	}

	/**
	 * 本地模式作业，配置了preferList，即使配置useDispreferList为true，但是仍然只有preferList能得到该作业分片。即useDispreferList对本地模式作业不起作用。
	 */
	@Test
	public void test_F_LocalModeWithPreferListAndUseDispreferList() throws Exception {
		Main executor1 = startOneNewExecutorList();
		Main executor2 = startOneNewExecutorList();

		int shardCount = 2;
		final String jobName = "test_F_LocalModeWithPreferListAndUseDispreferList";

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setShardingItemParameters("*=0");
		jobConfig.setLocalMode(true);
		jobConfig.setPreferList(executor2.getExecutorName()); // 设置preferList为executor2
		jobConfig.setUseDispreferList(true); // 设置useDispreferList为true

		addJob(jobConfig);
		Thread.sleep(1000);
		enableJob(jobName);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return isNeedSharding(jobName);
			}
		}, 10);
		runAtOnce(jobName);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return !isNeedSharding(jobName);
			}
		}, 10);
		// executor2获取到0分片，executor1获取不到分片
		List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
		assertThat(items).contains(0);
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(items).isEmpty();
		// wait running completed
		Thread.sleep(1000);
		// executor2下线
		stopExecutorGracefully(1);
		Thread.sleep(1000L);
		// 等待sharding分片完成
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return isNeedSharding(jobName);
			}
		}, 10);
		runAtOnce(jobName);
		// 等待拿走分片
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return !isNeedSharding(jobName);
			}
		}, 10);
		// executor1仍然获取不到分片
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(items).isEmpty();

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		stopExecutorListGracefully();
	}

	/**
	 * preferList配置了容器资源，并且useDispreferList为true。当该容器有executor在线，则得到分片；当该容器全部executor下线，则其他executor得到分片
	 */
	@Test
	public void test_G_ContainerWithUseDispreferList() throws Exception {
		Main executor1 = startOneNewExecutorList(); // 启动一个非容器executor

		boolean cleanOld = SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN;
		String taskOld = SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID;
		try {
			String taskId = "test1";
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = true;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = taskId;
			Main executor2 = startOneNewExecutorList(); // 启动一个容器executor

			final int shardCount = 2;
			final String jobName = "test_G_ContainerWithUseDispreferList";

			for (int i = 0; i < shardCount; i++) {
				String key = jobName + "_" + i;
				SimpleJavaJob.statusMap.put(key, 0);
			}

			final JobConfig jobConfig = new JobConfig();
			jobConfig.setJobName(jobName);
			jobConfig.setCron("9 9 9 9 9 ? 2099");
			jobConfig.setJobType(JobType.JAVA_JOB.toString());
			jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
			jobConfig.setShardingTotalCount(shardCount);
			jobConfig.setShardingItemParameters("0=0,1=1");
			jobConfig.setLocalMode(false);
			jobConfig.setPreferList("@" + taskId); // 设置preferList为@taskId
			jobConfig.setUseDispreferList(true); // 设置useDispreferList为true

			addJob(jobConfig);
			Thread.sleep(1000);
			enableJob(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return isNeedSharding(jobName);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return !isNeedSharding(jobName);
				}
			}, 10);

			// executor2获取到0、1分片，executor1获取不到分片
			List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
			assertThat(items).contains(0, 1);
			items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
			assertThat(items).isEmpty();

			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return hasCompletedZnodeForAllShards(jobName, shardCount);
				}
			}, 10);

			// executor2下线
			stopExecutorGracefully(1);
			Thread.sleep(1000L);
			// 等待sharding分片完成
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return isNeedSharding(jobName);
				}
			}, 10);

			runAtOnce(jobName);
			// 等待拿走分片
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return !isNeedSharding(jobName);
				}
			}, 10);
			// executor1仍然获取0、1分片
			items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
			assertThat(items).contains(0, 1);

			disableJob(jobName);
			Thread.sleep(1000);
			removeJob(jobName);
			stopExecutorListGracefully();
		} finally {
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = cleanOld;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = taskOld;
		}
	}

	/**
	 * preferList配置了容器资源，并且useDispreferList为false。当该容器有executor在线，则得到分片；当该容器全部executor下线，则其他executor可以得不到分片
	 */
	@Test
	public void test_H_ContainerWithOnlyPreferList() throws Exception {
		Main executor1 = startOneNewExecutorList(); // 启动一个非容器executor

		boolean cleanOld = SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN;
		String taskOld = SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID;
		try {
			String taskId = "test1";
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = true;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = taskId;
			Main executor2 = startOneNewExecutorList(); // 启动一个容器executor

			int shardCount = 2;
			final String jobName = "test_H_ContainerWithOnlyPreferList";

			for (int i = 0; i < shardCount; i++) {
				String key = jobName + "_" + i;
				SimpleJavaJob.statusMap.put(key, 0);
			}

			JobConfig jobConfig = new JobConfig();
			jobConfig.setJobName(jobName);
			jobConfig.setCron("9 9 9 9 9 ? 2099");
			jobConfig.setJobType(JobType.JAVA_JOB.toString());
			jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
			jobConfig.setShardingTotalCount(shardCount);
			jobConfig.setShardingItemParameters("0=0,1=1");
			jobConfig.setLocalMode(false);
			jobConfig.setPreferList("@" + taskId); // 设置preferList为@taskId
			jobConfig.setUseDispreferList(false); // 设置useDispreferList为false

			addJob(jobConfig);
			Thread.sleep(1000);
			enableJob(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return isNeedSharding(jobName);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return !isNeedSharding(jobName);
				}
			}, 10);

			// executor2获取到0、1分片，executor1获取不到分片
			List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
			assertThat(items).contains(0, 1);
			items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
			assertThat(items).isEmpty();
			// executor2下线
			stopExecutorGracefully(1);
			Thread.sleep(1000L);
			// 等待sharding分片完成
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return isNeedSharding(jobName);
				}
			}, 10);
			runAtOnce(jobName);
			// 等待拿走分片
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return !isNeedSharding(jobName);
				}
			}, 10);
			// executor1仍然获取不到分片
			items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
			assertThat(items).isEmpty();

			disableJob(jobName);
			Thread.sleep(1000);
			removeJob(jobName);
			stopExecutorListGracefully();
		} finally {
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = cleanOld;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = taskOld;
		}
	}

	/**
	 * preferList配置了容器资源，并且是本地模式，useDispreferList为true。当该容器有executor在线，则得到分片，并且分片数为1；当该容器全部executor下线，则其他executor也得不到分片，因为useDispreferList对本地模式无效
	 */
	@Test
	public void test_I_ContainerWithLocalModeAndUseDispreferList() throws Exception {
		Main executor1 = startOneNewExecutorList(); // 启动一个非容器executor

		boolean cleanOld = SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN;
		String taskOld = SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID;
		try {
			String taskId = "test1";
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = true;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = taskId;
			Main executor2 = startOneNewExecutorList(); // 启动一个容器executor

			int shardCount = 2;
			final String jobName = "test_I_ContainerWithLocalModeAndUseDispreferList";

			for (int i = 0; i < shardCount; i++) {
				String key = jobName + "_" + i;
				SimpleJavaJob.statusMap.put(key, 0);
			}

			JobConfig jobConfig = new JobConfig();
			jobConfig.setJobName(jobName);
			jobConfig.setCron("9 9 9 9 9 ? 2099");
			jobConfig.setJobType(JobType.JAVA_JOB.toString());
			jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
			jobConfig.setShardingTotalCount(shardCount);
			jobConfig.setShardingItemParameters("*=a");
			jobConfig.setLocalMode(true); // 设置localMode为true
			jobConfig.setPreferList("@" + taskId); // 设置preferList为@taskId
			jobConfig.setUseDispreferList(true); // 设置useDispreferList为true

			addJob(jobConfig);
			Thread.sleep(1000);
			enableJob(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return isNeedSharding(jobName);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return !isNeedSharding(jobName);
				}
			}, 10);

			// executor2获取到0分片，executor1获取不到分片
			List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
			assertThat(items).hasSize(1).contains(0);
			items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
			assertThat(items).isEmpty();
			// executor2下线
			stopExecutorGracefully(1);
			Thread.sleep(1000L);
			// 等待sharding分片完成
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return isNeedSharding(jobName);
				}
			}, 10);
			runAtOnce(jobName);
			// 等待拿走分片
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return !isNeedSharding(jobName);
				}
			}, 10);
			// executor1仍然拿不到分片
			items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
			assertThat(items).isEmpty();

			disableJob(jobName);
			Thread.sleep(1000);
			removeJob(jobName);
			stopExecutorListGracefully();
		} finally {
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = cleanOld;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = taskOld;
		}
	}

	/**
	 * preferList配置了容器资源，并且是本地模式，useDispreferList为false。当该容器有executor在线，则得到分片，并且分片数为1；当该容器全部executor下线，则其他executor也得不到分片
	 */
	@Test
	public void test_J_ContainerWithLocalModeAndOnlyPreferList() throws Exception {
		Main executor1 = startOneNewExecutorList(); // 启动一个非容器executor

		boolean cleanOld = SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN;
		String taskOld = SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID;
		try {
			String taskId = "test1";
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = true;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = taskId;
			Main executor2 = startOneNewExecutorList(); // 启动一个容器executor

			int shardCount = 2;
			final String jobName = "test_J_ContainerWithLocalModeAndOnlyPreferList";

			for (int i = 0; i < shardCount; i++) {
				String key = jobName + "_" + i;
				SimpleJavaJob.statusMap.put(key, 0);
			}

			JobConfig jobConfig = new JobConfig();
			jobConfig.setJobName(jobName);
			jobConfig.setCron("9 9 9 9 9 ? 2099");
			jobConfig.setJobType(JobType.JAVA_JOB.toString());
			jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
			jobConfig.setShardingTotalCount(shardCount);
			jobConfig.setShardingItemParameters("*=a");
			jobConfig.setLocalMode(true); // 设置localMode为true
			jobConfig.setPreferList("@" + taskId); // 设置preferList为@taskId
			jobConfig.setUseDispreferList(false); // 设置useDispreferList为false

			addJob(jobConfig);
			Thread.sleep(1000);
			enableJob(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return isNeedSharding(jobName);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return !isNeedSharding(jobName);
				}
			}, 10);

			// executor2获取到0分片，executor1获取不到分片
			List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
			assertThat(items).hasSize(1).contains(0);
			items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
			assertThat(items).isEmpty();
			// executor2下线
			stopExecutorGracefully(1);
			Thread.sleep(1000L);
			// 等待sharding分片完成
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return isNeedSharding(jobName);
				}
			}, 10);
			runAtOnce(jobName);
			// 等待拿走分片
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return !isNeedSharding(jobName);
				}
			}, 10);
			// executor1仍然获取不到分片
			items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
			assertThat(items).isEmpty();

			disableJob(jobName);
			Thread.sleep(1000);
			removeJob(jobName);
			stopExecutorListGracefully();
		} finally {
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = cleanOld;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = taskOld;
		}
	}

	/**
	 * preferList配置了无效容器资源，并且useDispreferList为true。则非容器资源会得到分片。
	 */
	@Test
	public void test_K_ContainerWithUseDispreferList_ButInvalidTaskId() throws Exception {
		Main logicExecutor = startOneNewExecutorList(); // 启动一个非容器executor

		boolean cleanOld = SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN;
		String taskOld = SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID;
		try {
			String taskId = "test1";
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = true;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = taskId;
			Main vdosExecutor = startOneNewExecutorList(); // 启动一个容器executor

			int shardCount = 2;
			final String jobName = "test_K_ContainerWithUseDispreferList_ButInvalidTaskId";

			for (int i = 0; i < shardCount; i++) {
				String key = jobName + "_" + i;
				SimpleJavaJob.statusMap.put(key, 0);
			}

			JobConfig jobConfig = new JobConfig();
			jobConfig.setJobName(jobName);
			jobConfig.setCron("9 9 9 9 9 ? 2099");
			jobConfig.setJobType(JobType.JAVA_JOB.toString());
			jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
			jobConfig.setShardingTotalCount(shardCount);
			jobConfig.setShardingItemParameters("0=0,1=1");
			jobConfig.setLocalMode(false);
			jobConfig.setPreferList("@haha" + taskId); // 设置preferList为@hahataskId
			jobConfig.setUseDispreferList(true); // 设置useDispreferList为true

			addJob(jobConfig);
			Thread.sleep(1000);
			enableJob(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return isNeedSharding(jobName);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return !isNeedSharding(jobName);
				}
			}, 10);

			// vdosExecutor获取不到分片，logicExecutor获取到0、1分片
			List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath
					.getNodeFullPath(jobName, ShardingNode.getShardingNode(vdosExecutor.getExecutorName()))));
			assertThat(items).isEmpty();
			items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath
					.getNodeFullPath(jobName, ShardingNode.getShardingNode(logicExecutor.getExecutorName()))));
			assertThat(items).contains(0, 1);
			// vdosExecutor下线
			stopExecutorGracefully(1);
			Thread.sleep(1000);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return isNeedSharding(jobName);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return !isNeedSharding(jobName);
				}
			}, 10);
			// logicExecutor仍然获取0、1分片
			items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath
					.getNodeFullPath(jobName, ShardingNode.getShardingNode(logicExecutor.getExecutorName()))));
			assertThat(items).contains(0, 1);

			disableJob(jobName);
			Thread.sleep(1000);
			removeJob(jobName);
			stopExecutorListGracefully();
		} finally {
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = cleanOld;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = taskOld;
		}
	}

	/**
	 * preferList配置了无效容器资源，并且useDispreferList为true。先添加作业，启用作业，再启动容器，再启动物理机。则非容器资源会得到分片。
	 */
	@Test
	public void test_L_ContainerWithUseDispreferList_ButInvalidTaskId_ContainerFirst() throws Exception {
		boolean cleanOld = SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN;
		String taskOld = SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID;
		try {
			int shardCount = 2;
			final String jobName = "test_L_ContainerWithUseDispreferList_ButInvalidTaskId_ContainerFirst";

			for (int i = 0; i < shardCount; i++) {
				String key = jobName + "_" + i;
				SimpleJavaJob.statusMap.put(key, 0);
			}

			JobConfig jobConfig = new JobConfig();
			jobConfig.setJobName(jobName);
			jobConfig.setCron("9 9 9 9 9 ? 2099");
			jobConfig.setJobType(JobType.JAVA_JOB.toString());
			jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
			jobConfig.setShardingTotalCount(shardCount);
			jobConfig.setShardingItemParameters("0=0,1=1");
			jobConfig.setLocalMode(false);
			jobConfig.setPreferList("@haha"); // 设置preferList为@haha
			jobConfig.setUseDispreferList(true); // 设置useDispreferList为true

			addJob(jobConfig);
			Thread.sleep(1000);
			enableJob(jobName);

			// 启动一个容器executor
			String taskId = "test1";
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = true;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = taskId;
			Main vdosExecutor = startOneNewExecutorList();

			// 启动一个非容器executor
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = false;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = null;
			Main logicExecutor = startOneNewExecutorList();

			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return isNeedSharding(jobName);
				}
			}, 10);

			runAtOnce(jobName);

			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return !isNeedSharding(jobName);
				}
			}, 10);

			// vdosExecutor获取不到分片，logicExecutor获取到0、1分片
			List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath
					.getNodeFullPath(jobName, ShardingNode.getShardingNode(vdosExecutor.getExecutorName()))));
			assertThat(items).isEmpty();
			items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath
					.getNodeFullPath(jobName, ShardingNode.getShardingNode(logicExecutor.getExecutorName()))));
			assertThat(items).contains(0, 1);
			// vdosExecutor下线
			stopExecutorGracefully(0);
			Thread.sleep(1000L);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return isNeedSharding(jobName);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return !isNeedSharding(jobName);
				}
			}, 10);
			// logicExecutor仍然获取0、1分片
			items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath
					.getNodeFullPath(jobName, ShardingNode.getShardingNode(logicExecutor.getExecutorName()))));
			assertThat(items).contains(0, 1);

			disableJob(jobName);
			Thread.sleep(1000);
			removeJob(jobName);
			stopExecutorListGracefully();
		} finally {
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = cleanOld;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = taskOld;
		}
	}

	/**
	 * preferList配置了无效物理资源，并且useDispreferList为true。先添加作业，启用作业，再启动容器，再启动物理机。则物理资源会得到分片。
	 */
	@Test
	public void test_M_UseDispreferList_ButInvalidLogicPreferList() throws Exception {
		boolean cleanOld = SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN;
		String taskOld = SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID;
		try {
			int shardCount = 2;
			final String jobName = "test_M_UseDispreferList_ButInvalidLogicPreferList";

			for (int i = 0; i < shardCount; i++) {
				String key = jobName + "_" + i;
				SimpleJavaJob.statusMap.put(key, 0);
			}

			JobConfig jobConfig = new JobConfig();
			jobConfig.setJobName(jobName);
			jobConfig.setCron("9 9 9 9 9 ? 2099");
			jobConfig.setJobType(JobType.JAVA_JOB.toString());
			jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
			jobConfig.setShardingTotalCount(shardCount);
			jobConfig.setShardingItemParameters("0=0,1=1");
			jobConfig.setLocalMode(false);
			jobConfig.setPreferList("haha"); // 设置preferList为@haha
			jobConfig.setUseDispreferList(true); // 设置useDispreferList为true

			addJob(jobConfig);
			Thread.sleep(1000);
			enableJob(jobName);

			// 启动一个容器executor
			String taskId = "test1";
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = true;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = taskId;
			Main vdosExecutor = startOneNewExecutorList();

			// 启动一个非容器executor
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = false;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = null;
			Main logicExecutor = startOneNewExecutorList();

			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return isNeedSharding(jobName);
				}
			}, 10);

			runAtOnce(jobName);

			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return !isNeedSharding(jobName);
				}
			}, 10);

			// vdosExecutor获取不到分片，logicExecutor获取到0、1分片
			List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath
					.getNodeFullPath(jobName, ShardingNode.getShardingNode(vdosExecutor.getExecutorName()))));
			assertThat(items).isEmpty();
			items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath
					.getNodeFullPath(jobName, ShardingNode.getShardingNode(logicExecutor.getExecutorName()))));
			assertThat(items).contains(0, 1);
			// vdosExecutor下线
			stopExecutorGracefully(0);
			Thread.sleep(1000L);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return isNeedSharding(jobName);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean isOk() {
					return !isNeedSharding(jobName);
				}
			}, 10);
			// logicExecutor仍然获取0、1分片
			items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath
					.getNodeFullPath(jobName, ShardingNode.getShardingNode(logicExecutor.getExecutorName()))));
			assertThat(items).contains(0, 1);

			disableJob(jobName);
			Thread.sleep(1000);
			removeJob(jobName);
			stopExecutorListGracefully();
		} finally {
			SystemEnvProperties.VIP_SATURN_EXECUTOR_CLEAN = cleanOld;
			SystemEnvProperties.VIP_SATURN_CONTAINER_DEPLOYMENT_ID = taskOld;
		}
	}

	/**
	 * sharding仅仅通知分片信息改变的作业
	 */
	@Test
	public void test_N_NotifyNecessaryJobs() throws Exception {
		// 启动1个executor
		Main executor1 = startOneNewExecutorList();
		Thread.sleep(1000);

		// 启动第一个作业
		Thread.sleep(1000);
		final String jobName1 = "test_N_NotifyNecessaryJobs1";
		JobConfig jobConfig1 = new JobConfig();
		jobConfig1.setJobName(jobName1);
		jobConfig1.setCron("9 9 9 9 9 ? 2099");
		jobConfig1.setJobType(JobType.JAVA_JOB.toString());
		jobConfig1.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig1.setShardingTotalCount(1);
		jobConfig1.setShardingItemParameters("0=0");
		addJob(jobConfig1);
		Thread.sleep(1000);
		enableJob(jobName1);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return isNeedSharding(jobName1);
			}
		}, 10);
		runAtOnce(jobName1);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return !isNeedSharding(jobName1);
			}
		}, 10);

		// 启动第二个作业
		Thread.sleep(1000);
		final String jobName2 = "test_N_NotifyNecessaryJobs2";
		JobConfig jobConfig2 = new JobConfig();
		jobConfig2.setJobName(jobName2);
		jobConfig2.setCron("9 9 9 9 9 ? 2099");
		jobConfig2.setJobType(JobType.JAVA_JOB.toString());
		jobConfig2.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig2.setShardingTotalCount(1);
		jobConfig2.setShardingItemParameters("0=0");

		addJob(jobConfig2);
		// job1和job2均无需re-sharding
		Thread.sleep(1000);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return !isNeedSharding(jobName1) && !isNeedSharding(jobName2);
			}
		}, 10);

		enableJob(jobName2);
		// job1无需re-sharding
		Thread.sleep(1000);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return !isNeedSharding(jobName1);
			}
		}, 10);

		disableJob(jobName1);
		removeJob(jobName1);
		disableJob(jobName2);
		removeJob(jobName2);
		stopExecutorListGracefully();
	}

	/**
	 * sharding仅仅通知分片信息改变的作业 test the fix:
	 * https://github.com/vipshop/Saturn/commit/9b64dfe50c21c1b4f3e3f781d5281be06a0a8d08
	 */
	@Test
	public void test_O_NotifyNecessaryJobsPrior() throws Exception {
		// 启动1个executor
		Main executor1 = startOneNewExecutorList();
		Thread.sleep(1000);

		// 启动第一个作业
		Thread.sleep(1000);
		final String jobName = "test_O_NotifyNecessaryJobsPrior";
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		addJob(jobConfig);
		Thread.sleep(1000);
		enableJob(jobName);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return isNeedSharding(jobName);
			}
		}, 10);
		runAtOnce(jobName);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return !isNeedSharding(jobName);
			}
		}, 10);

		// 禁用作业
		Thread.sleep(1000);
		disableJob(jobName);

		// 设置preferList为一个无效的executor，并且设置useDispreferList为false
		zkUpdateJobNode(jobName, "config/preferList", "abc");
		zkUpdateJobNode(jobName, "config/useDispreferList", "false");

		// 启用作业
		Thread.sleep(500);
		enableJob(jobName);

		// job1需re-sharding
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return isNeedSharding(jobName);
			}
		}, 10);

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		stopExecutorListGracefully();
	}

	/**
	 * NamespaceShardingService is not necessary to persist the sharding result content that is not changed<br/>
	 * https://github.com/vipshop/Saturn/issues/88
	 */
	@Test
	public void test_P_PersistShardingContentIfNecessary() throws Exception {
		// 启动1个executor
		Main executor1 = startOneNewExecutorList();
		Thread.sleep(1000);

		// 启动第一个作业
		Thread.sleep(1000);
		final String jobName = "test_P_PersistShardingContentIfNecessary";
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		jobConfig.setPreferList("abc");
		jobConfig.setUseDispreferList(false);
		addJob(jobConfig);
		Thread.sleep(1000);
		enableJob(jobName);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return isNeedSharding(jobName);
			}
		}, 10);
		runAtOnce(jobName);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return !isNeedSharding(jobName);
			}
		}, 10);
		long mtime = ((CuratorFramework) regCenter.getRawClient()).checkExists()
				.forPath(SaturnExecutorsNode.getShardingContentElementNodePath("0")).getMtime();

		// 禁用作业
		Thread.sleep(1000);
		disableJob(jobName);

		Thread.sleep(1000);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return !isNeedSharding(jobName);
			}
		}, 10);

		long mtime2 = ((CuratorFramework) regCenter.getRawClient()).checkExists()
				.forPath(SaturnExecutorsNode.getShardingContentElementNodePath("0")).getMtime();

		assertThat(mtime).isEqualTo(mtime2);

		removeJob(jobName);
		stopExecutorListGracefully();
	}

	/**
	 * https://github.com/vipshop/Saturn/issues/119
	 */
	@Test
	public void test_Q_PersistNecessaryTheRightData() throws Exception {
		// 启动1个executor
		Main executor1 = startOneNewExecutorList();
		Thread.sleep(1000);

		// 启动第一个作业
		Thread.sleep(1000);
		final String jobName = "test_Q_PersistNecessaryTheRightData";
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		jobConfig.setUseDispreferList(false);
		addJob(jobConfig);
		Thread.sleep(1000);
		enableJob(jobName);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return isNeedSharding(jobName);
			}
		}, 10);

		String jobLeaderShardingNecessaryNodePath = SaturnExecutorsNode.getJobLeaderShardingNecessaryNodePath(jobName);
		String data1 = regCenter.getDirectly(jobLeaderShardingNecessaryNodePath);
		System.out.println("data1:" + data1);

		runAtOnce(jobName);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return !isNeedSharding(jobName);
			}
		}, 10);

		// 启动第2个executor
		Main executor2 = startOneNewExecutorList();
		Thread.sleep(1000);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return isNeedSharding(jobName);
			}
		}, 10);

		String data2 = regCenter.getDirectly(jobLeaderShardingNecessaryNodePath);
		System.out.println("data2:" + data2);
		assertThat(data2.contains(executor2.getExecutorName())).isTrue();

		runAtOnce(jobName);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return !isNeedSharding(jobName);
			}
		}, 10);

		// offline executor2
		stopExecutorGracefully(1);
		Thread.sleep(1000);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return isNeedSharding(jobName);
			}
		}, 10);

		String data3 = regCenter.getDirectly(jobLeaderShardingNecessaryNodePath);
		System.out.println("data3:" + data3);

		assertThat(data3.contains(executor2.getExecutorName())).isFalse();

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		stopExecutorListGracefully();
	}

}
