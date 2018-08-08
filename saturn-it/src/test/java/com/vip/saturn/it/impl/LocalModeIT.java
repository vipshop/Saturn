package com.vip.saturn.it.impl;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.it.utils.LogbackListAppender;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.internal.config.JobConfiguration;
import com.vip.saturn.job.sharding.task.AbstractAsyncShardingTask;
import org.apache.commons.exec.OS;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LocalModeIT extends AbstractSaturnIT {
	public static String NORMAL_SH_PATH;

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
		File file1 = new File("src/test/resources/script/normal/normal_0.sh");
		NORMAL_SH_PATH = file1.getAbsolutePath();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorList();
		stopSaturnConsoleList();
	}

	@After
	public void after() throws Exception {
		stopExecutorList();
		SimpleJavaJob.statusMap.clear();
	}

	@Test
	public void test_A() throws Exception {
		if (!OS.isFamilyUnix()) {
			return;
		}
		startExecutorList(2);

		final JobConfiguration jobConfiguration = new JobConfiguration("shLocalModeJob");
		jobConfiguration.setCron("*/2 * * * * ?");
		jobConfiguration.setJobType(JobType.SHELL_JOB.toString());
		jobConfiguration.setProcessCountIntervalSeconds(1);
		jobConfiguration.setShardingItemParameters("*=sh " + NORMAL_SH_PATH);
		jobConfiguration.setLocalMode(true);

		addJob(jobConfiguration);
		Thread.sleep(1000);
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		startOneNewExecutorList();
		Thread.sleep(1000);
		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				for (Main executor : saturnExecutorList) {
					String count = getJobNode(jobConfiguration,
							"servers/" + executor.getExecutorName() + "/processSuccessCount");
					System.out.println("count:" + count + ";executor:" + executor.getExecutorName());
					if (count == null)
						return false;
					int times = Integer.parseInt(count);
					if (times <= 0)
						return false;
				}

				return true;
			}

		}, 10);

		disableJob(jobConfiguration.getJobName());
		Thread.sleep(1000L);
		removeJob(jobConfiguration.getJobName());

		Thread.sleep(1000);

		forceRemoveJob(jobConfiguration.getJobName());
	}

	@Test
	public void test_B() throws Exception {
		startExecutorList(2);

		int shardCount = 3;
		String jobName = "javaLocalModeJob";

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0/1 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setProcessCountIntervalSeconds(1);
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingItemParameters("*=0");
		jobConfiguration.setLocalMode(true);

		addJob(jobConfiguration);
		Thread.sleep(1000);

		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1 * 1000);

		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				for (Main executor : saturnExecutorList) {
					String count = getJobNode(jobConfiguration,
							"servers/" + executor.getExecutorName() + "/processSuccessCount");
					System.out.println("count:" + count + ";executor:" + executor.getExecutorName());
					if (count == null)
						return false;
					int times = Integer.parseInt(count);
					if (times <= 0)
						return false;
				}

				return true;
			}

		}, 60);

		disableJob(jobName);
		Thread.sleep(1000L);
		removeJob(jobConfiguration.getJobName());

		Thread.sleep(1000);

		forceRemoveJob(jobName);
	}

	@Test
	public void test_C_withPreferList() throws Exception {
		startExecutorList(2);

		int shardCount = 3;
		String jobName = "javaLocalModeWithPreferListJob";

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		final Main preferExecutor = saturnExecutorList.get(0);

		final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0/1 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setProcessCountIntervalSeconds(1);
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingItemParameters("*=0");
		jobConfiguration.setLocalMode(true);
		jobConfiguration.setPreferList(preferExecutor.getExecutorName());

		addJob(jobConfiguration);
		Thread.sleep(1000);

		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1 * 1000);

		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				String count0 = getJobNode(jobConfiguration,
						"servers/" + preferExecutor.getExecutorName() + "/processSuccessCount");
				System.out.println("count:" + count0 + ";executor:" + preferExecutor.getExecutorName());
				if (count0 == null)
					return false;
				int times0 = Integer.parseInt(count0);
				if (times0 <= 0)
					return false;

				for (int i = 1; i < saturnExecutorList.size(); i++) {
					Main executor = saturnExecutorList.get(i);
					String count = getJobNode(jobConfiguration,
							"servers/" + executor.getExecutorName() + "/processSuccessCount");
					System.out.println("count:" + count + ";executor:" + executor.getExecutorName());
					if (count != null) {
						int times = Integer.parseInt(count);
						if (times != 0)
							return false;
					}
				}

				return true;
			}

		}, 30);

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobConfiguration.getJobName());

		Thread.sleep(1000);

		forceRemoveJob(jobName);
	}

	@Test
	public void test_D_fixIssue441() throws Exception {
		LogbackListAppender logbackListAppender = new LogbackListAppender();
		logbackListAppender.addToLogger(AbstractAsyncShardingTask.class);
		logbackListAppender.start();

		final int items = 4;
		final String jobName = "test_D_fixIssue441";

		final JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("0/1 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setProcessCountIntervalSeconds(1);
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingItemParameters("*=0");
		jobConfiguration.setLocalMode(true);

		addJob(jobConfiguration);
		Thread.sleep(1000);

		enableJob(jobName);
		Thread.sleep(1000);

		startExecutorList(items);

		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				for (int i = 0; i < items; i++) {
					if (SimpleJavaJob.statusMap.get(jobName + "_" + i) <= 0) {
						return false;
					}
				}
				return true;
			}

		}, 30);

		// 下线0,1,2
		stopExecutor(0);
		stopExecutor(1);
		stopExecutor(2);

		// 上线一个executor
		startOneNewExecutorList();

		List<ILoggingEvent> allLogs = logbackListAppender.getAllLogs();
		Iterator<ILoggingEvent> iterator = allLogs.iterator();
		while (iterator.hasNext()) {
			ILoggingEvent event = iterator.next();
			if (event != null) {
				IThrowableProxy throwableProxy = event.getThrowableProxy();
				if (throwableProxy != null) {
					assertThat(throwableProxy.getClassName()).isNotEqualTo("java.lang.ArrayIndexOutOfBoundsException");
				}
			}
		}

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);

		Thread.sleep(1000);

		forceRemoveJob(jobName);
	}
}
