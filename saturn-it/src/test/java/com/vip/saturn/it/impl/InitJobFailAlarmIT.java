package com.vip.saturn.it.impl;

import com.vip.saturn.it.base.AbstractSaturnIT;
import com.vip.saturn.it.job.InitMsgJobFail.*;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.executor.InitNewJobService;
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
		stopExecutorListGracefully();
		stopSaturnConsoleList();
	}

	@After
	public void after() throws Exception {
		stopExecutorListGracefully();
	}

	@Test
	public void testA_InitFailOfGetObjectJob() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName("testA_InitFailOfGetObjectJob");
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(InitFailOfGetObjectJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		addJob(jobConfig);
		Thread.sleep(2000);
		assertThat(InitNewJobService.containsJobInitFailedRecord(executorName, jobConfig.getJobName(),
				"java.lang.ArithmeticException: / by zero")).isTrue();
		removeJob(jobConfig.getJobName());
	}

	@Test
	public void testB_InitFailOfDefaultConstructorJob() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName("testB_InitFailOfDefaultConstructorJob");
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(InitFailOfDefaultConstructorJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		addJob(jobConfig);
		Thread.sleep(2000);
		assertThat(InitNewJobService.containsJobInitFailedRecord(executorName, jobConfig.getJobName(),
				"java.lang.ArithmeticException: / by zero")).isTrue();
		removeJob(jobConfig.getJobName());
	}

	@Test
	public void testC_InitFailOfRuntimeExceptionJob() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName("testC_InitFailOfRuntimeExceptionJob");
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(InitFailOfRuntimeExceptionJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		addJob(jobConfig);
		Thread.sleep(2000);
		assertThat(InitNewJobService.containsJobInitFailedRecord(executorName, jobConfig.getJobName(),
				"java.lang.RuntimeException: RuntimeException!!!")).isTrue();
		removeJob(jobConfig.getJobName());
	}

	@Test
	public void testD_InitFailOfErrorJob() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName("testD_InitFailOfErrorJob");
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(InitFailOfErrorJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		addJob(jobConfig);
		Thread.sleep(2000);
		assertThat(InitNewJobService
				.containsJobInitFailedRecord(executorName, jobConfig.getJobName(), "java.lang.Error: Error!!!"))
				.isTrue();
		removeJob(jobConfig.getJobName());
	}

	@Test
	public void testE_ClassNotFoundException() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName("testE_ClassNotFoundException");
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass("WhoAmI");
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		addJob(jobConfig);
		Thread.sleep(2000);
		assertThat(InitNewJobService.containsJobInitFailedRecord(executorName, jobConfig.getJobName(),
				"java.lang.ClassNotFoundException: WhoAmI")).isTrue();
		removeJob(jobConfig.getJobName());
	}

	@Test
	public void testF_jobClassIsNotSet() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName("testF_jobClassIsNotSet");
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass("");
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		// just add to zk, because add fail if add job by console api
		zkAddJob(jobConfig);
		Thread.sleep(2000);
		assertThat(InitNewJobService
				.containsJobInitFailedRecord(executorName, jobConfig.getJobName(), "jobClass is not set")).isTrue();
		zkRemoveJob(jobConfig.getJobName());
	}

	@Test
	public void testG_multiRecord() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName("testG_multiRecord");
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass("");
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		// just add to zk, because add fail if add job by console api
		zkAddJob(jobConfig);
		Thread.sleep(2000);
		assertThat(InitNewJobService
				.containsJobInitFailedRecord(executorName, jobConfig.getJobName(), "jobClass is not set")).isTrue();

		zkRemoveJob(jobConfig.getJobName());

		jobConfig.setJobClass("WhoAmI");
		addJob(jobConfig);
		Thread.sleep(2000);
		assertThat(InitNewJobService
				.containsJobInitFailedRecord(executorName, jobConfig.getJobName(), "jobClass is not set")).isTrue();
		assertThat(InitNewJobService.containsJobInitFailedRecord(executorName, jobConfig.getJobName(),
				"java.lang.ClassNotFoundException: WhoAmI")).isTrue();

		removeJob(jobConfig.getJobName());
	}

	@Test
	public void testH_clearRecord() throws Exception {
		String executorName = startOneNewExecutorList().getExecutorName();
		Thread.sleep(1000);
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName("testH_clearRecord");
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass("");
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		// just add to zk, because add fail if add job by console api
		zkAddJob(jobConfig);
		Thread.sleep(2000);
		assertThat(InitNewJobService
				.containsJobInitFailedRecord(executorName, jobConfig.getJobName(), "jobClass is not set")).isTrue();

		zkRemoveJob(jobConfig.getJobName());

		jobConfig.setJobClass(InitSuccessfullyJob.class.getCanonicalName());
		addJob(jobConfig);
		Thread.sleep(2000);
		assertThat(InitNewJobService
				.containsJobInitFailedRecord(executorName, jobConfig.getJobName(), "jobClass is not set")).isFalse();

		removeJob(jobConfig.getJobName());
	}

}
