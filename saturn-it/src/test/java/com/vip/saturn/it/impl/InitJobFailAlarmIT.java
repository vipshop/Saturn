package com.vip.saturn.it.impl;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.InitMsgJobFail.*;
import com.vip.saturn.job.executor.InitNewJobService;
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
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfiguration jobConfiguration = new JobConfiguration("testA_InitFailOfGetObjectJob");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(InitFailOfGetObjectJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(InitNewJobService.containsJobInitFailedRecord(executorName, jobConfiguration.getJobName(),
				"java.lang.ArithmeticException: / by zero")).isTrue();
		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

	@Test
	public void testB_InitFailOfDefaultConstructorJob() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfiguration jobConfiguration = new JobConfiguration("testB_InitFailOfDefaultConstructorJob");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(InitFailOfDefaultConstructorJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(InitNewJobService.containsJobInitFailedRecord(executorName, jobConfiguration.getJobName(),
				"java.lang.ArithmeticException: / by zero")).isTrue();
		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

	@Test
	public void testC_InitFailOfRuntimeExceptionJob() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfiguration jobConfiguration = new JobConfiguration("testC_InitFailOfRuntimeExceptionJob");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(InitFailOfRuntimeExceptionJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(InitNewJobService.containsJobInitFailedRecord(executorName, jobConfiguration.getJobName(),
				"java.lang.RuntimeException: RuntimeException!!!")).isTrue();
		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

	@Test
	public void testD_InitFailOfErrorJob() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfiguration jobConfiguration = new JobConfiguration("testD_InitFailOfErrorJob");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(InitFailOfErrorJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(InitNewJobService
				.containsJobInitFailedRecord(executorName, jobConfiguration.getJobName(), "java.lang.Error: Error!!!"))
				.isTrue();
		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

	@Test
	public void testE_ClassNotFoundException() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfiguration jobConfiguration = new JobConfiguration("testE_ClassNotFoundException");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass("WhoAmI");
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(InitNewJobService.containsJobInitFailedRecord(executorName, jobConfiguration.getJobName(),
				"java.lang.ClassNotFoundException: WhoAmI")).isTrue();
		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

	@Test
	public void testF_jobClassIsNotSet() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfiguration jobConfiguration = new JobConfiguration("testF_jobClassIsNotSet");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass("");
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(InitNewJobService
				.containsJobInitFailedRecord(executorName, jobConfiguration.getJobName(), "jobClass is not set"))
				.isTrue();
		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

	@Test
	public void testG_multiRecord() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfiguration jobConfiguration = new JobConfiguration("testG_multiRecord");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass("");
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(InitNewJobService
				.containsJobInitFailedRecord(executorName, jobConfiguration.getJobName(), "jobClass is not set"))
				.isTrue();

		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());

		jobConfiguration.setJobClass("WhoAmI");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(InitNewJobService
				.containsJobInitFailedRecord(executorName, jobConfiguration.getJobName(), "jobClass is not set"))
				.isTrue();
		assertThat(InitNewJobService.containsJobInitFailedRecord(executorName, jobConfiguration.getJobName(),
				"java.lang.ClassNotFoundException: WhoAmI")).isTrue();

		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

	@Test
	public void testH_clearRecord() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfiguration jobConfiguration = new JobConfiguration("testH_clearRecord");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass("");
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(InitNewJobService
				.containsJobInitFailedRecord(executorName, jobConfiguration.getJobName(), "jobClass is not set"))
				.isTrue();

		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());

		jobConfiguration.setJobClass(InitSuccessfullyJob.class.getCanonicalName());
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(InitNewJobService
				.containsJobInitFailedRecord(executorName, jobConfiguration.getJobName(), "jobClass is not set"))
				.isFalse();

		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

}
