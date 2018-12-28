package com.vip.saturn.it.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.vip.saturn.it.base.AbstractSaturnIT;
import com.vip.saturn.it.utils.LogbackListAppender;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.executor.InitNewJobService;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.junit.*;
import org.junit.runners.MethodSorters;

import static org.assertj.core.api.Assertions.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class InitJobByGroupsIT extends AbstractSaturnIT {

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

	@Test
	public void testA_ExecutorNotConfigGroups() throws Exception {
		System.clearProperty(SystemEnvProperties.NAME_VIP_SATURN_INIT_JOB_BY_GROUPS);
		SystemEnvProperties.loadProperties();

		LogbackListAppender logbackListAppender = new LogbackListAppender();
		logbackListAppender.addToLogger(InitNewJobService.class);
		logbackListAppender.start();
		try {
			startOneNewExecutorList();
			Thread.sleep(1000);
			JobConfig jobConfig = new JobConfig();
			jobConfig.setJobName("testA_ExecutorNotConfigGroups");
			jobConfig.setCron("*/2 * * * * ?");
			jobConfig.setJobType(JobType.SHELL_JOB.toString());
			jobConfig.setShardingTotalCount(1);
			jobConfig.setShardingItemParameters("0=0");
			jobConfig.setGroups("");
			addJob(jobConfig);
			Thread.sleep(1000);

			assertThat(logbackListAppender.getLastMessage()).isEqualTo(
					"[testA_ExecutorNotConfigGroups] msg=the job testA_ExecutorNotConfigGroups initialize successfully");

			removeJob(jobConfig.getJobName());
		} finally {
			logbackListAppender.clearLogs();
			logbackListAppender.stop();
		}
	}

	@Test
	public void testB_ExecutorConfigGroupsAndInitJobSuccessfully() throws Exception {
		try {
			System.setProperty(SystemEnvProperties.NAME_VIP_SATURN_INIT_JOB_BY_GROUPS, "group1, group2");
			SystemEnvProperties.loadProperties();

			LogbackListAppender logbackListAppender = new LogbackListAppender();
			logbackListAppender.addToLogger(InitNewJobService.class);
			logbackListAppender.start();
			try {
				startOneNewExecutorList();
				Thread.sleep(1000);
				JobConfig jobConfig = new JobConfig();
				jobConfig.setJobName("testB_ExecutorConfigGroupsAndInitJobSuccessfully");
				jobConfig.setCron("*/2 * * * * ?");
				jobConfig.setJobType(JobType.SHELL_JOB.toString());
				jobConfig.setShardingTotalCount(1);
				jobConfig.setShardingItemParameters("0=0");
				jobConfig.setGroups("group2");
				addJob(jobConfig);
				Thread.sleep(1000);

				assertThat(logbackListAppender.getLastMessage()).isEqualTo(
						"[testB_ExecutorConfigGroupsAndInitJobSuccessfully] msg=the job testB_ExecutorConfigGroupsAndInitJobSuccessfully initialize successfully");

				removeJob(jobConfig.getJobName());
			} finally {
				logbackListAppender.clearLogs();
				logbackListAppender.stop();
			}
		} finally {
			System.clearProperty(SystemEnvProperties.NAME_VIP_SATURN_INIT_JOB_BY_GROUPS);
			SystemEnvProperties.loadProperties();
		}
	}

	@Test
	public void testC_ExecutorConfigGroupsAndInitJobFailed() throws Exception {
		try {
			System.setProperty(SystemEnvProperties.NAME_VIP_SATURN_INIT_JOB_BY_GROUPS, "group1, group2");
			SystemEnvProperties.loadProperties();

			LogbackListAppender logbackListAppender = new LogbackListAppender();
			logbackListAppender.addToLogger(InitNewJobService.class);
			logbackListAppender.start();
			try {
				startOneNewExecutorList();
				Thread.sleep(1000);
				JobConfig jobConfig = new JobConfig();
				jobConfig.setJobName("testC_ExecutorConfigGroupsAndInitJobFailed");
				jobConfig.setCron("*/2 * * * * ?");
				jobConfig.setJobType(JobType.SHELL_JOB.toString());
				jobConfig.setShardingTotalCount(1);
				jobConfig.setShardingItemParameters("0=0");
				jobConfig.setGroups("");
				addJob(jobConfig);
				Thread.sleep(1000);

				ILoggingEvent lastLog = logbackListAppender.getLastLog();
				assertThat(lastLog.getLevel()).isEqualTo(Level.INFO);
				// VIP_SATURN_INIT_JOB_BY_GROUPS is Set, not ArrayList
				assertThat(lastLog.getFormattedMessage())
						.isIn("[testC_ExecutorConfigGroupsAndInitJobFailed] msg=the job testC_ExecutorConfigGroupsAndInitJobFailed wont be initialized, because it's not in the groups [group1, group2]",
								"[testC_ExecutorConfigGroupsAndInitJobFailed] msg=the job testC_ExecutorConfigGroupsAndInitJobFailed wont be initialized, because it's not in the groups [group2, group1]");

				removeJob(jobConfig.getJobName());
			} finally {
				logbackListAppender.clearLogs();
				logbackListAppender.stop();
			}
		} finally {
			System.clearProperty(SystemEnvProperties.NAME_VIP_SATURN_INIT_JOB_BY_GROUPS);
			SystemEnvProperties.loadProperties();
		}
	}

}
