package com.vip.saturn.it.impl;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.job.internal.config.JobConfiguration;
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
		stopSaturnConsoleList();
	}

	@After
	public void after() throws Exception {
		SimpleJavaJob.lock.set(false);
		stopExecutorList();
	}

	@Test
	public void testA() throws Exception {
		startOneNewExecutorList();
		Thread.sleep(1000);
		SimpleJavaJob.lock.set(false);
		JobConfiguration jobConfiguration = new JobConfiguration("testA");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isFalse();
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isTrue();
		disableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isFalse();
		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

	@Test
	public void testB_restartExecutorWithEnabledChanged() throws Exception {
		startOneNewExecutorList();
		Thread.sleep(1000);
		SimpleJavaJob.lock.set(false);
		JobConfiguration jobConfiguration = new JobConfiguration("testB_restartExecutor");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isFalse();

		stopExecutor(0);
		Thread.sleep(1000);

		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		startOneNewExecutorList();
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isTrue();

		disableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isFalse();

		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

	@Test
	public void testC_singleExecutor() throws Exception {
		startOneNewExecutorList();
		Thread.sleep(1000);
		SimpleJavaJob.lock.set(true);
		JobConfiguration jobConfiguration = new JobConfiguration("testC_singleExecutor");
		jobConfiguration.setCron("0/2 * * * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setShardingItemParameters("0=0");
		addJob(jobConfiguration);
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isFalse();

		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isTrue();

		disableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		assertThat(SimpleJavaJob.enabled.get()).isTrue(); // still true

		synchronized (SimpleJavaJob.lock) {
			SimpleJavaJob.lock.notifyAll();
		}
		Thread.sleep(200);
		assertThat(SimpleJavaJob.enabled.get()).isFalse(); // change to false

		removeJob(jobConfiguration.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobConfiguration.getJobName());
	}

}
