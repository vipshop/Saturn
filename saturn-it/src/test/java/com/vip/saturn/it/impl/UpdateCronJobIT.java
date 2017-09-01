package com.vip.saturn.it.impl;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.it.job.UpdateCronJob;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.internal.config.JobConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.assertj.core.api.Assertions.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UpdateCronJobIT extends AbstractSaturnIT {

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
		startExecutorList(1);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorList();
		stopSaturnConsoleList();

	}

	@Test
	public void updateCron() throws Exception {
		final int shardCount = 3;
		final String jobName = "toBeupdatedITJob";
		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			SimpleJavaJob.statusMap.put(key, 0);
		}

		JobConfiguration jobConfiguration = new JobConfiguration(jobName);
		jobConfiguration.setCron("59 59 1 * * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(shardCount);
		jobConfiguration.setTimeoutSeconds(0);
		jobConfiguration.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfiguration);
		Thread.sleep(2000);
		enableJob(jobName);

		jobConfiguration = new JobConfiguration("updateCronITJob");
		jobConfiguration.setCron("0 1 1 1 * ?");
		jobConfiguration.setJobType(JobType.JAVA_JOB.toString());
		jobConfiguration.setJobClass(UpdateCronJob.class.getCanonicalName());
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setProcessCountIntervalSeconds(1);
		jobConfiguration.setShardingItemParameters("0=toBeupdatedITJob");
		addJob(jobConfiguration);
		Thread.sleep(2000);

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			assertThat(SimpleJavaJob.statusMap.get(key)).isEqualTo(0);
		}

		enableJob(jobConfiguration.getJobName());
		Thread.sleep(2000);
		runAtOnce(jobConfiguration.getJobName());
		Thread.sleep(2000);
		final JobConfiguration t = jobConfiguration;
		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				for (Main executor : saturnExecutorList) {
					String count = getJobNode(t, "servers/" + executor.getExecutorName() + "/processSuccessCount");
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
		Thread.sleep(2 * 1000);
		removeJob(jobConfiguration.getJobName());

		waitForFinish(new FinishCheck() {

			@Override
			public boolean docheck() {
				for (int i = 0; i < shardCount; i++) {
					String key = jobName + "_" + i;
					if (SimpleJavaJob.statusMap.get(key) < 1) {
						return false;
					}
				}
				return true;
			}

		}, 30);

		disableJob(jobName);
		Thread.sleep(1000);
		removeJob(jobName);
		Thread.sleep(2000);
		SimpleJavaJob.statusMap.clear();
	}
}
