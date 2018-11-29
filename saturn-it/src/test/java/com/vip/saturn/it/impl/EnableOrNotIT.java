package com.vip.saturn.it.impl;

import com.vip.saturn.it.base.AbstractSaturnIT;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobType;
import org.junit.*;
import org.junit.runners.MethodSorters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author hebelala
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EnableOrNotIT extends AbstractSaturnIT {

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
		SimpleJavaJob.lock.set(false);
		stopExecutorListGracefully();
	}

	@Test
	public void testA() throws Exception {
		startOneNewExecutorList();
		Thread.sleep(1000);
		SimpleJavaJob.lock.set(false);
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName("testA");
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		addJob(jobConfig);
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isFalse();
		enableJob(jobConfig.getJobName());
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isTrue();
		disableJob(jobConfig.getJobName());
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isFalse();
		removeJob(jobConfig.getJobName());
	}

	@Test
	public void testB_restartExecutorWithEnabledChanged() throws Exception {
		startOneNewExecutorList();
		Thread.sleep(1000);
		SimpleJavaJob.lock.set(false);
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName("testB_restartExecutor");
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		addJob(jobConfig);
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isFalse();

		stopExecutorGracefully(0);
		Thread.sleep(1000);

		enableJob(jobConfig.getJobName());
		Thread.sleep(1000);
		startOneNewExecutorList();
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isTrue();

		disableJob(jobConfig.getJobName());
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isFalse();

		removeJob(jobConfig.getJobName());
	}

	@Test
	public void testC_singleExecutor() throws Exception {
		startOneNewExecutorList();
		Thread.sleep(1000);
		SimpleJavaJob.lock.set(true);
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName("testC_singleExecutor");
		jobConfig.setCron("*/2 * * * * ?");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		addJob(jobConfig);
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isFalse();

		enableJob(jobConfig.getJobName());
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isTrue();

		disableJob(jobConfig.getJobName());
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isTrue(); // still true

		synchronized (SimpleJavaJob.lock) {
			SimpleJavaJob.lock.notifyAll();
		}
		Thread.sleep(200);
		assertThat(SimpleJavaJob.enabled.get()).isFalse(); // change to false

		removeJob(jobConfig.getJobName());
	}

}
