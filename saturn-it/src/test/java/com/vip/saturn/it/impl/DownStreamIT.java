package com.vip.saturn.it.impl;

import com.vip.saturn.it.base.AbstractSaturnIT;
import com.vip.saturn.it.base.FinishCheck;
import com.vip.saturn.it.job.downStream.JobA;
import com.vip.saturn.it.job.downStream.JobB;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.console.vo.UpdateJobConfigVo;
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

		// add downStream firstly
		JobConfig jobB = new JobConfig();
		jobB.setJobName("downStreamITJobB");
		jobB.setJobType(JobType.PASSIVE_JAVA_JOB.toString());
		jobB.setJobClass(JobB.class.getCanonicalName());
		jobB.setShardingTotalCount(1);
		jobB.setShardingItemParameters("0=0");
		addJob(jobB);
		Thread.sleep(1000);

		JobConfig jobA = new JobConfig();
		jobA.setJobName("downStreamITJobA");
		jobA.setCron("9 9 9 9 9 ? 2099");
		jobA.setJobType(JobType.JAVA_JOB.toString());
		jobA.setJobClass(JobA.class.getCanonicalName());
		jobA.setShardingTotalCount(1);
		jobA.setShardingItemParameters("0=0");
		jobA.setDownStream(jobB.getJobName());
		addJob(jobA);
		Thread.sleep(1000);

		enableJob(jobA.getJobName());
		enableJob(jobB.getJobName());
		Thread.sleep(1000);

		runAtOnce(jobA.getJobName());
		Thread.sleep(1000);

		waitForFinish(new FinishCheck() {
			@Override
			public boolean isOk() {
				return JobB.count == 1;
			}
		}, 10);

		disableJob(jobA.getJobName());
		disableJob(jobB.getJobName());
		Thread.sleep(1000);

		UpdateJobConfigVo updateJobConfigVo = new UpdateJobConfigVo();
		updateJobConfigVo.setJobName(jobA.getJobName());
		updateJobConfigVo.setDownStream("");
		updateJob(updateJobConfigVo);
		Thread.sleep(1000);

		removeJob(jobA.getJobName());
		removeJob(jobB.getJobName());
	}
}
