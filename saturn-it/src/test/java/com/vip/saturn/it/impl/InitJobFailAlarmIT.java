package com.vip.saturn.it.impl;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.InitMsgJobFail.InitFailOfDefaultConstructorJob;
import com.vip.saturn.it.job.InitMsgJobFail.InitFailOfErrorJob;
import com.vip.saturn.it.job.InitMsgJobFail.InitFailOfGetObjectJob;
import com.vip.saturn.it.job.InitMsgJobFail.InitFailOfRuntimeExceptionJob;
import com.vip.saturn.job.basic.SaturnExecutorContext;
import com.vip.saturn.job.internal.config.JobConfiguration;
import org.junit.*;
import org.junit.runners.MethodSorters;

import static org.assertj.core.api.Assertions.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class InitJobFailAlarmIT extends AbstractSaturnIT {

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopSaturnConsoleList();
		stopExecutorList();
	}

	@After
	public void after() throws Exception {
		stopExecutorList();
	}

	@Test
	public void testA_InitFailOfGetObjectJob() throws Exception {
		startOneNewExecutorList();
		Thread.sleep(1000);
		JobConfiguration jobConfiguration = new JobConfiguration("testA_InitFailOfGetObjectJob");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(InitFailOfGetObjectJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(SaturnExecutorContext.containsJobInitExceptionMessage(jobConfiguration.getJobName(),
				"java.lang.ArithmeticException: / by zero")).isTrue();
		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

	@Test
	public void testB_InitFailOfDefaultConstructorJob() throws Exception {
		startOneNewExecutorList();
		Thread.sleep(1000);
		JobConfiguration jobConfiguration = new JobConfiguration("testB_InitFailOfDefaultConstructorJob");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(InitFailOfDefaultConstructorJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(SaturnExecutorContext.containsJobInitExceptionMessage(jobConfiguration.getJobName(),
				"java.lang.ArithmeticException: / by zero")).isTrue();
		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

	@Test
	public void testC_InitFailOfRuntimeExceptionJob() throws Exception {
		startOneNewExecutorList();
		Thread.sleep(1000);
		JobConfiguration jobConfiguration = new JobConfiguration("testC_InitFailOfRuntimeExceptionJob");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(InitFailOfRuntimeExceptionJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(SaturnExecutorContext.containsJobInitExceptionMessage(jobConfiguration.getJobName(),
				"java.lang.RuntimeException: RuntimeException!!!")).isTrue();
		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

	@Test
	public void testD_InitFailOfErrorJob() throws Exception {
		startOneNewExecutorList();
		Thread.sleep(1000);
		JobConfiguration jobConfiguration = new JobConfiguration("testD_InitFailOfErrorJob");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(InitFailOfErrorJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(SaturnExecutorContext
				.containsJobInitExceptionMessage(jobConfiguration.getJobName(), "java.lang.Error: Error!!!")).isTrue();
		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

}
