package com.vip.saturn.it.impl;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.job.downStream.JobA;
import com.vip.saturn.it.job.downStream.JobB;
import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.internal.config.JobConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * @author hebelala
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DownStreamIT extends AbstractSaturnIT {

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
	public void test() throws Exception {
		startOneNewExecutorList();

		JobB.count = 0;

		JobConfiguration jobA = new JobConfiguration("downStreamITJobA");
		jobA.setCron("* * * * * ? 2099");
		jobA.setJobType(JobType.JAVA_JOB.toString());
		jobA.setJobClass(JobA.class.getCanonicalName());
		jobA.setShardingTotalCount(1);
		jobA.setShardingItemParameters("0=0");
		jobA.setDownStream("downStreamJobITJobB");
		addJob(jobA);
		Thread.sleep(1000);

		JobConfiguration jobB = new JobConfiguration("downStreamITJobB");
		jobB.setJobType(JobType.PASSIVE_JAVA_JOB.toString());
		jobB.setJobClass(JobB.class.getCanonicalName());
		jobB.setShardingTotalCount(1);
		jobB.setShardingItemParameters("0=0");
		addJob(jobB);
		Thread.sleep(1000);

		enableJob(jobA.getJobName());
		enableJob(jobB.getJobName());
		Thread.sleep(1000);

		runAtOnce(jobA.getJobName());
		Thread.sleep(1000);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean docheck() {
				return JobB.count == 1;
			}
		}, 10);

		disableJob(jobA.getJobName());
		disableJob(jobB.getJobName());
		Thread.sleep(1000);
		removeJob(jobA.getJobName());
		removeJob(jobB.getJobName());
		Thread.sleep(1000);
		forceRemoveJob(jobA.getJobName());
		forceRemoveJob(jobB.getJobName());
	}
}
