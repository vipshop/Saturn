package com.vip.saturn.it.impl;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.internal.config.JobConfiguration;
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
		stopExecutorList();
		stopSaturnConsoleList();
	}

	@Test
	public void test_A_JAVA() throws Exception {
		int shardCount = 3;
		String jobName = "javaITJob";

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("* * 1 * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters("0=0,1=1,2=2");

		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1 * 1000);

		Main executor1 = startOneNewExecutorList();// 启动第1台executor
		runAtOnce(jobName);
		Thread.sleep(1000);

		assertThat(regCenter.getDirectly(SaturnExecutorsNode.SHARDING_COUNT_PATH)).isEqualTo("4");

		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				if (isNeedSharding(jobConfiguration)) {
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
			public boolean docheck() {
				if (isNeedSharding(jobConfiguration)) {
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

		stopExecutor(0); // 停第1个executor

		Thread.sleep(1000);
		assertThat(regCenter.getDirectly(SaturnExecutorsNode.SHARDING_COUNT_PATH)).isEqualTo("10");

		Thread.sleep(1000);
		runAtOnce(jobName);
		Thread.sleep(1000);
		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				if (isNeedSharding(jobConfiguration)) {
					return false;
				}
				return true;
			}

		}, 10);
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		System.out.println(items);
		assertThat(items).isEmpty();

		stopExecutor(1); // 停第2个executor
		Thread.sleep(1000);
		runAtOnce(jobName);
		Thread.sleep(1000);
		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				if (isNeedSharding(jobConfiguration)) {
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

		stopExecutorList();
		Thread.sleep(2000);
		forceRemoveJob(jobName);
	}

	@Test
	public void test_D_PreferList() throws Exception {
		Main executor1 = startOneNewExecutorList();// 启动第1台executor

		int shardCount = 3;
		String jobName = "javaITJob";

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("* * 1 * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters("0=0,1=1,2=2");
		jobConfiguration.setPreferList(executor1.getExecutorName());
		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1 * 1000);

		runAtOnce(jobName);
		Thread.sleep(1000);
		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				if (isNeedSharding(jobConfiguration)) {
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
			public boolean docheck() {
				if (isNeedSharding(jobConfiguration)) {
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

		stopExecutor(0); // 停第1个executor
		Thread.sleep(1000);
		runAtOnce(jobName);
		Thread.sleep(1000);
		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				if (isNeedSharding(jobConfiguration)) {
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
			public boolean docheck() {
				if (isNeedSharding(jobConfiguration)) {
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
		removeJob(jobConfiguration.getJobName());

		Thread.sleep(1000);

		stopExecutorList();
		forceRemoveJob(jobName);
	}

	@Test
	public void test_E_PreferListOnly() throws Exception {
		Main executor1 = startOneNewExecutorList();// 启动第1台executor

		int shardCount = 3;
		String jobName = "javaITJob";

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("* * 1 * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters("0=0,1=1,2=2");
		jobConfiguration.setPreferList(executor1.getExecutorName());
		jobConfiguration.setUseDispreferList(false);

		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1 * 1000);

		runAtOnce(jobName);
		Thread.sleep(1000);
		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				if (isNeedSharding(jobConfiguration)) {
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
			public boolean docheck() {
				if (isNeedSharding(jobConfiguration)) {
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

		stopExecutor(0); // 停第1个executor
		Thread.sleep(1000);
		runAtOnce(jobName);
		Thread.sleep(1000);
		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				if (isNeedSharding(jobConfiguration)) {
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

		Thread.sleep(1000);

		stopExecutorList();
		forceRemoveJob(jobName);
	}

	/**
	 * 本地模式作业，配置了preferList，并且useDispreferList为false，则只有preferList能得到该作业分片
	 */
	@Test
	public void test_F_LocalModeWithPreferList() throws Exception {
		Main executor1 = startOneNewExecutorList();
		Main executor2 = startOneNewExecutorList();

		int shardCount = 2;
		String jobName = "test_F_LocalModeWithPreferList_job";

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0 0 1 * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters("*=0");
		jobConfiguration.setLocalMode(true);
		jobConfiguration.setPreferList(executor2.getExecutorName()); // 设置preferList为executor2
		jobConfiguration.setUseDispreferList(false); // 设置useDispreferList为false

		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return isNeedSharding(jobConfiguration);
			}
		}, 10);
		runAtOnce(jobName);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return !isNeedSharding(jobConfiguration);
			}
		}, 10);
		List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
		assertThat(items).contains(0);
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(items).isEmpty();
		// executor2下线
		final String executor2Name = saturnExecutorList.get(1).getExecutorName();
		stopExecutor(1);
		Thread.sleep(1000L);
		// 等待sharding分片完成
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return isNeedSharding(jobConfiguration);
			}
		}, 10);
		// executor1仍然获取不到分片
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(items).isEmpty();

		stopExecutorList();
		forceRemoveJob(jobName);
	}

	/**
	 * 本地模式作业，配置了preferList，即使配置useDispreferList为true，但是仍然只有preferList能得到该作业分片。即useDispreferList对本地模式作业不起作用。
	 */
	@Test
	public void test_F_LocalModeWithPreferListAndUseDispreferList() throws Exception {
		Main executor1 = startOneNewExecutorList();
		Main executor2 = startOneNewExecutorList();

		int shardCount = 2;
		String jobName = "test_F_LocalModeWithPreferListAndUseDispreferList_job";

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("* * 1 * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setShardingItemParameters("*=0");
		jobConfiguration.setLocalMode(true);
		jobConfiguration.setPreferList(executor2.getExecutorName()); // 设置preferList为executor2
		jobConfiguration.setUseDispreferList(true); // 设置useDispreferList为true

		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return isNeedSharding(jobConfiguration);
			}
		}, 10);
		runAtOnce(jobName);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return !isNeedSharding(jobConfiguration);
			}
		}, 10);
		// executor2获取到0分片，executor1获取不到分片
		List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor2.getExecutorName()))));
		assertThat(items).contains(0);
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(items).isEmpty();
		// executor2下线
		final String executor2Name = saturnExecutorList.get(1).getExecutorName();
		stopExecutor(1);
		Thread.sleep(1000L);
		// 等待sharding分片完成
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return isNeedSharding(jobConfiguration);
			}
		}, 10);
		runAtOnce(jobName);
		// 等待拿走分片
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return !isNeedSharding(jobConfiguration);
			}
		}, 10);
		// executor1仍然获取不到分片
		items = ItemUtils.toItemList(regCenter.getDirectly(
				JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
		assertThat(items).isEmpty();

		stopExecutorList();
		forceRemoveJob(jobName);
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
			final String jobName = "test_G_Container";

			for (int i = 0; i < shardCount; i++) {
				String key = jobName + "_" + i;
				SimpleJavaJob.statusMap.put(key, 0);
			}

			final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
			jobConfiguration.setCron("* * 1 * * ?");
			jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
			jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
			jobConfiguration.setShardingTotalCount(shardCount);
			jobConfiguration.setShardingItemParameters("0=0,1=1");
			jobConfiguration.setLocalMode(false);
			jobConfiguration.setPreferList("@" + taskId); // 设置preferList为@taskId
			jobConfiguration.setUseDispreferList(true); // 设置useDispreferList为true

			addJob(jobConfiguration);
			Thread.sleep(1000);
			enableJob(jobConfiguration.getJobName());
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return isNeedSharding(jobConfiguration);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isNeedSharding(jobConfiguration);
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
			final String executor2Name = saturnExecutorList.get(1).getExecutorName();
			stopExecutor(1);
			Thread.sleep(1000L);
			// 等待sharding分片完成
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return isNeedSharding(jobConfiguration);
				}
			}, 10);

			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return hasCompletedZnodeForAllShards(jobName, shardCount);
				}
			}, 10);

			runAtOnce(jobName);
			// 等待拿走分片
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isNeedSharding(jobConfiguration);
				}
			}, 10);
			// executor1仍然获取0、1分片
			items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
			assertThat(items).contains(0, 1);

			stopExecutorList();
			forceRemoveJob(jobName);
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
			String jobName = "test_G_Container";

			for (int i = 0; i < shardCount; i++) {
				String key = jobName + "_" + i;
				SimpleJavaJob.statusMap.put(key, 0);
			}

			final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
			jobConfiguration.setCron("* * 1 * * ?");
			jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
			jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
			jobConfiguration.setShardingTotalCount(shardCount);
			jobConfiguration.setShardingItemParameters("0=0,1=1");
			jobConfiguration.setLocalMode(false);
			jobConfiguration.setPreferList("@" + taskId); // 设置preferList为@taskId
			jobConfiguration.setUseDispreferList(false); // 设置useDispreferList为false

			addJob(jobConfiguration);
			Thread.sleep(1000);
			enableJob(jobConfiguration.getJobName());
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return isNeedSharding(jobConfiguration);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isNeedSharding(jobConfiguration);
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
			final String executor2Name = saturnExecutorList.get(1).getExecutorName();
			stopExecutor(1);
			Thread.sleep(1000L);
			// 等待sharding分片完成
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return isNeedSharding(jobConfiguration);
				}
			}, 10);
			runAtOnce(jobName);
			// 等待拿走分片
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isNeedSharding(jobConfiguration);
				}
			}, 10);
			// executor1仍然获取不到分片
			items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
			assertThat(items).isEmpty();

			stopExecutorList();
			forceRemoveJob(jobName);
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
			String jobName = "test_I_ContainerWithLocalModeAndUseDispreferList";

			for (int i = 0; i < shardCount; i++) {
				String key = jobName + "_" + i;
				SimpleJavaJob.statusMap.put(key, 0);
			}

			final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
			jobConfiguration.setCron("* * 1 * * ?");
			jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
			jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
			jobConfiguration.setShardingTotalCount(shardCount);
			jobConfiguration.setShardingItemParameters("*=a");
			jobConfiguration.setLocalMode(true); // 设置localMode为true
			jobConfiguration.setPreferList("@" + taskId); // 设置preferList为@taskId
			jobConfiguration.setUseDispreferList(true); // 设置useDispreferList为true

			addJob(jobConfiguration);
			Thread.sleep(1000);
			enableJob(jobConfiguration.getJobName());
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return isNeedSharding(jobConfiguration);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isNeedSharding(jobConfiguration);
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
			final String executor2Name = saturnExecutorList.get(1).getExecutorName();
			stopExecutor(1);
			Thread.sleep(1000L);
			// 等待sharding分片完成
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return isNeedSharding(jobConfiguration);
				}
			}, 10);
			runAtOnce(jobName);
			// 等待拿走分片
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isNeedSharding(jobConfiguration);
				}
			}, 10);
			// executor1仍然拿不到分片
			items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
			assertThat(items).isEmpty();

			stopExecutorList();
			forceRemoveJob(jobName);
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
			String jobName = "test_J_ContainerWithLocalModeAndOnlyPreferList";

			for (int i = 0; i < shardCount; i++) {
				String key = jobName + "_" + i;
				SimpleJavaJob.statusMap.put(key, 0);
			}

			final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
			jobConfiguration.setCron("* * 1 * * ?");
			jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
			jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
			jobConfiguration.setShardingTotalCount(shardCount);
			jobConfiguration.setShardingItemParameters("*=a");
			jobConfiguration.setLocalMode(true); // 设置localMode为true
			jobConfiguration.setPreferList("@" + taskId); // 设置preferList为@taskId
			jobConfiguration.setUseDispreferList(false); // 设置useDispreferList为false

			addJob(jobConfiguration);
			Thread.sleep(1000);
			enableJob(jobConfiguration.getJobName());
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return isNeedSharding(jobConfiguration);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isNeedSharding(jobConfiguration);
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
			final String executor2Name = saturnExecutorList.get(1).getExecutorName();
			stopExecutor(1);
			Thread.sleep(1000L);
			// 等待sharding分片完成
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return isNeedSharding(jobConfiguration);
				}
			}, 10);
			runAtOnce(jobName);
			// 等待拿走分片
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isNeedSharding(jobConfiguration);
				}
			}, 10);
			// executor1仍然获取不到分片
			items = ItemUtils.toItemList(regCenter.getDirectly(
					JobNodePath.getNodeFullPath(jobName, ShardingNode.getShardingNode(executor1.getExecutorName()))));
			assertThat(items).isEmpty();

			stopExecutorList();
			forceRemoveJob(jobName);
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
			String jobName = "test_K_ContainerWithUseDispreferList_ButInvalidTaskId";

			for (int i = 0; i < shardCount; i++) {
				String key = jobName + "_" + i;
				SimpleJavaJob.statusMap.put(key, 0);
			}

			final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
			jobConfiguration.setCron("* * 1 * * ?");
			jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
			jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
			jobConfiguration.setShardingTotalCount(shardCount);
			jobConfiguration.setShardingItemParameters("0=0,1=1");
			jobConfiguration.setLocalMode(false);
			jobConfiguration.setPreferList("@haha" + taskId); // 设置preferList为@hahataskId
			jobConfiguration.setUseDispreferList(true); // 设置useDispreferList为true

			addJob(jobConfiguration);
			Thread.sleep(1000);
			enableJob(jobConfiguration.getJobName());
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return isNeedSharding(jobConfiguration);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isNeedSharding(jobConfiguration);
				}
			}, 10);

			// vdosExecutor获取不到分片，logicExecutor获取到0、1分片
			List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName,
					ShardingNode.getShardingNode(vdosExecutor.getExecutorName()))));
			assertThat(items).isEmpty();
			items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName,
					ShardingNode.getShardingNode(logicExecutor.getExecutorName()))));
			assertThat(items).contains(0, 1);
			// vdosExecutor下线
			stopExecutor(1);
			Thread.sleep(1000);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return isNeedSharding(jobConfiguration);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isNeedSharding(jobConfiguration);
				}
			}, 10);
			// logicExecutor仍然获取0、1分片
			items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName,
					ShardingNode.getShardingNode(logicExecutor.getExecutorName()))));
			assertThat(items).contains(0, 1);

			stopExecutorList();
			forceRemoveJob(jobName);
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
			String jobName = "test_L_ContainerWithUseDispreferList_ButInvalidTaskId_ContainerFirst";

			for (int i = 0; i < shardCount; i++) {
				String key = jobName + "_" + i;
				SimpleJavaJob.statusMap.put(key, 0);
			}

			final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
			jobConfiguration.setCron("* * 1 * * ?");
			jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
			jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
			jobConfiguration.setShardingTotalCount(shardCount);
			jobConfiguration.setShardingItemParameters("0=0,1=1");
			jobConfiguration.setLocalMode(false);
			jobConfiguration.setPreferList("@haha"); // 设置preferList为@haha
			jobConfiguration.setUseDispreferList(true); // 设置useDispreferList为true

			addJob(jobConfiguration);
			Thread.sleep(1000);
			enableJob(jobConfiguration.getJobName());

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
				public boolean docheck() {
					return isNeedSharding(jobConfiguration);
				}
			}, 10);

			runAtOnce(jobName);

			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isNeedSharding(jobConfiguration);
				}
			}, 10);

			// vdosExecutor获取不到分片，logicExecutor获取到0、1分片
			List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName,
					ShardingNode.getShardingNode(vdosExecutor.getExecutorName()))));
			assertThat(items).isEmpty();
			items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName,
					ShardingNode.getShardingNode(logicExecutor.getExecutorName()))));
			assertThat(items).contains(0, 1);
			// vdosExecutor下线
			stopExecutor(0);
			Thread.sleep(1000L);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return isNeedSharding(jobConfiguration);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isNeedSharding(jobConfiguration);
				}
			}, 10);
			// logicExecutor仍然获取0、1分片
			items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName,
					ShardingNode.getShardingNode(logicExecutor.getExecutorName()))));
			assertThat(items).contains(0, 1);

			stopExecutorList();
			forceRemoveJob(jobName);
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
			String jobName = "test_L_ContainerWithUseDispreferList_ButInvalidTaskId_ContainerFirst";

			for (int i = 0; i < shardCount; i++) {
				String key = jobName + "_" + i;
				SimpleJavaJob.statusMap.put(key, 0);
			}

			final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
			jobConfiguration.setCron("* * 1 * * ?");
			jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
			jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
			jobConfiguration.setShardingTotalCount(shardCount);
			jobConfiguration.setShardingItemParameters("0=0,1=1");
			jobConfiguration.setLocalMode(false);
			jobConfiguration.setPreferList("haha"); // 设置preferList为@haha
			jobConfiguration.setUseDispreferList(true); // 设置useDispreferList为true

			addJob(jobConfiguration);
			Thread.sleep(1000);
			enableJob(jobConfiguration.getJobName());

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
				public boolean docheck() {
					return isNeedSharding(jobConfiguration);
				}
			}, 10);

			runAtOnce(jobName);

			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isNeedSharding(jobConfiguration);
				}
			}, 10);

			// vdosExecutor获取不到分片，logicExecutor获取到0、1分片
			List<Integer> items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName,
					ShardingNode.getShardingNode(vdosExecutor.getExecutorName()))));
			assertThat(items).isEmpty();
			items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName,
					ShardingNode.getShardingNode(logicExecutor.getExecutorName()))));
			assertThat(items).contains(0, 1);
			// vdosExecutor下线
			stopExecutor(0);
			Thread.sleep(1000L);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return isNeedSharding(jobConfiguration);
				}
			}, 10);
			runAtOnce(jobName);
			waitForFinish(new FinishCheck() {
				@Override
				public boolean docheck() {
					return !isNeedSharding(jobConfiguration);
				}
			}, 10);
			// logicExecutor仍然获取0、1分片
			items = ItemUtils.toItemList(regCenter.getDirectly(JobNodePath.getNodeFullPath(jobName,
					ShardingNode.getShardingNode(logicExecutor.getExecutorName()))));
			assertThat(items).contains(0, 1);

			stopExecutorList();
			forceRemoveJob(jobName);
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
		String jobName1 = "test_M_NotifyNecessaryJobs_job1";
		final JobConfiguration jobConfiguration1 = new JobConfiguration(jobName1);
		jobConfiguration1.setCron("* * 1 * * ?");
		jobConfiguration1.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration1.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration1.setShardingTotalCount(1);
		jobConfiguration1.setShardingItemParameters("0=0");
		addJob(jobConfiguration1);
		Thread.sleep(1000);
		enableJob(jobName1);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return isNeedSharding(jobConfiguration1);
			}
		}, 10);
		runAtOnce(jobName1);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return !isNeedSharding(jobConfiguration1);
			}
		}, 10);

		// 启动第二个作业
		Thread.sleep(1000);
		String jobName2 = "test_M_NotifyNecessaryJobs_job2";
		final JobConfiguration jobConfiguration2 = new JobConfiguration(jobName2);
		jobConfiguration2.setCron("* * 1 * * ?");
		jobConfiguration2.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration2.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration2.setShardingTotalCount(1);
		jobConfiguration2.setShardingItemParameters("0=0");

		addJob(jobConfiguration2);
		// job1和job2均无需re-sharding
		Thread.sleep(1000);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return !isNeedSharding(jobConfiguration1) && !isNeedSharding(jobConfiguration2);
			}
		}, 10);

		enableJob(jobName2);
		// job1无需re-sharding
		Thread.sleep(1000);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return !isNeedSharding(jobConfiguration1);
			}
		}, 10);

		stopExecutorList();
		forceRemoveJob(jobName1);
		forceRemoveJob(jobName2);
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
		String jobName1 = "test_M_NotifyNecessaryJobs_job1";
		final JobConfiguration jobConfiguration1 = new JobConfiguration(jobName1);
		jobConfiguration1.setCron("* * 1 * * ?");
		jobConfiguration1.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration1.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration1.setShardingTotalCount(1);
		jobConfiguration1.setShardingItemParameters("0=0");
		addJob(jobConfiguration1);
		Thread.sleep(1000);
		enableJob(jobName1);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return isNeedSharding(jobConfiguration1);
			}
		}, 10);
		runAtOnce(jobName1);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return !isNeedSharding(jobConfiguration1);
			}
		}, 10);

		// 禁用作业
		Thread.sleep(1000);
		disableJob(jobName1);

		// 设置preferList为一个无效的executor，并且设置useDispreferList为false
		configJob(jobName1, "config/preferList", "abc");
		configJob(jobName1, "config/useDispreferList", "false");

		// 启用作业
		Thread.sleep(500);
		enableJob(jobName1);

		// job1需re-sharding
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return isNeedSharding(jobConfiguration1);
			}
		}, 10);

		stopExecutorList();
		forceRemoveJob(jobName1);
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
		String jobName1 = "test_P_PersistShardingContentIfNecessary";
		final JobConfiguration jobConfiguration1 = new JobConfiguration(jobName1);
		jobConfiguration1.setCron("* * 1 * * ?");
		jobConfiguration1.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration1.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration1.setShardingTotalCount(1);
		jobConfiguration1.setShardingItemParameters("0=0");
		jobConfiguration1.setPreferList("abc");
		jobConfiguration1.setUseDispreferList(false);
		addJob(jobConfiguration1);
		Thread.sleep(1000);
		enableJob(jobName1);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return isNeedSharding(jobConfiguration1);
			}
		}, 10);
		runAtOnce(jobName1);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return !isNeedSharding(jobConfiguration1);
			}
		}, 10);
		long mtime = ((CuratorFramework) regCenter.getRawClient()).checkExists()
				.forPath(SaturnExecutorsNode.getShardingContentElementNodePath("0")).getMtime();

		// 禁用作业
		Thread.sleep(1000);
		disableJob(jobName1);

		Thread.sleep(1000);
		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return !isNeedSharding(jobConfiguration1);
			}
		}, 10);

		long mtime2 = ((CuratorFramework) regCenter.getRawClient()).checkExists()
				.forPath(SaturnExecutorsNode.getShardingContentElementNodePath("0")).getMtime();

		assertThat(mtime).isEqualTo(mtime2);

		stopExecutorList();
		forceRemoveJob(jobName1);
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
		String jobName1 = "test_Q_PersistNecessaryTheRightData";
		final JobConfiguration jobConfiguration1 = new JobConfiguration(jobName1);
		jobConfiguration1.setCron("* * 1 * * ?");
		jobConfiguration1.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration1.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration1.setShardingTotalCount(1);
		jobConfiguration1.setShardingItemParameters("0=0");
		jobConfiguration1.setUseDispreferList(false);
		addJob(jobConfiguration1);
		Thread.sleep(1000);
		enableJob(jobName1);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return isNeedSharding(jobConfiguration1);
			}
		}, 10);

		String jobLeaderShardingNecessaryNodePath = SaturnExecutorsNode.getJobLeaderShardingNecessaryNodePath(jobName1);
		String data1 = regCenter.getDirectly(jobLeaderShardingNecessaryNodePath);
		System.out.println("data1:" + data1);

		runAtOnce(jobName1);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return !isNeedSharding(jobConfiguration1);
			}
		}, 10);

		// 启动第2个executor
		Main executor2 = startOneNewExecutorList();
		Thread.sleep(1000);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return isNeedSharding(jobConfiguration1);
			}
		}, 10);

		String data2 = regCenter.getDirectly(jobLeaderShardingNecessaryNodePath);
		System.out.println("data2:" + data2);
		assertThat(data2.contains(executor2.getExecutorName())).isTrue();

		runAtOnce(jobName1);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return !isNeedSharding(jobConfiguration1);
			}
		}, 10);

		// offline executor2
		stopExecutor(1);
		Thread.sleep(1000);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return isNeedSharding(jobConfiguration1);
			}
		}, 10);

		String data3 = regCenter.getDirectly(jobLeaderShardingNecessaryNodePath);
		System.out.println("data3:" + data3);

		assertThat(data3.contains(executor2.getExecutorName())).isFalse();

		stopExecutorList();
		forceRemoveJob(jobName1);
	}

}
