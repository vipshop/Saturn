package com.vip.saturn.it.impl;

import com.vip.saturn.it.base.AbstractSaturnIT;
import com.vip.saturn.it.base.FinishCheck;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.it.job.UpdateCronJob;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.executor.Main;
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
		stopExecutorListGracefully();
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

		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(SimpleJavaJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(shardCount);
		jobConfig.setTimeoutSeconds(0);
		jobConfig.setShardingItemParameters("0=0,1=1,2=2");
		addJob(jobConfig);
		Thread.sleep(2000);
		enableJob(jobName);

		jobConfig = new JobConfig();
		jobConfig.setJobName("updateCronITJob");
		jobConfig.setCron("9 9 9 9 9 ? 2099");
		jobConfig.setJobType(JobType.JAVA_JOB.toString());
		jobConfig.setJobClass(UpdateCronJob.class.getCanonicalName());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setProcessCountIntervalSeconds(1);
		jobConfig.setShardingItemParameters("0=toBeupdatedITJob");
		addJob(jobConfig);
		Thread.sleep(2000);

		for (int i = 0; i < shardCount; i++) {
			String key = jobName + "_" + i;
			assertThat(SimpleJavaJob.statusMap.get(key)).isEqualTo(0);
		}

		enableJob(jobConfig.getJobName());
		Thread.sleep(2000);
		runAtOnce(jobConfig.getJobName());
		Thread.sleep(2000);
		final JobConfig t = jobConfig;
		waitForFinish(new FinishCheck() {

			@Override
			public boolean isOk() {
				for (Main executor : saturnExecutorList) {
					String count = zkGetJobNode(t.getJobName(),
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

		disableJob(jobConfig.getJobName());
		Thread.sleep(2 * 1000);
		removeJob(jobConfig.getJobName());

		waitForFinish(new FinishCheck() {

			@Override
			public boolean isOk() {
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
