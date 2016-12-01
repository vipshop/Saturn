package com.vip.saturn.it.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.OS;
import org.junit.*;
import org.junit.runners.MethodSorters;

import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.JobType;
import com.vip.saturn.it.SaturnAutoBasic.FinishCheck;
import com.vip.saturn.it.job.SimpleJavaJob;
import com.vip.saturn.job.internal.config.JobConfiguration;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScriptJobIT extends AbstractSaturnIT {
	public static String NORMAL_SH_PATH;

	@BeforeClass
	public static void setUp() throws Exception {
		startNamespaceShardingManagerList(1);
		startExecutorList(1);

		File file1 = new File("src/test/resources/script/normal/normal_0.sh");
		NORMAL_SH_PATH = file1.getAbsolutePath();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		// clean msg in the queueToTest
		// clearQueues(queueToTest);
		stopExecutorList();
		stopNamespaceShardingManagerList();
	}

	@Test
	public void A_Normalsh() throws InterruptedException {
		if (!OS.isFamilyUnix()) {
			return;
		}
		final JobConfiguration jobConfiguration = new JobConfiguration("scriptJob_1");
		jobConfiguration.setCron("*/4 * * * * ?");
		jobConfiguration.setJobType(JobType.SHELL_JOB.toString());
		jobConfiguration.setJobClass("com.vip.saturn.job.shell.SaturnScriptJob");
		jobConfiguration.setShardingTotalCount(1);
		jobConfiguration.setProcessCountIntervalSeconds(1);
		jobConfiguration.setShardingItemParameters("0=sh " + NORMAL_SH_PATH);
		addJob(jobConfiguration);
		Thread.sleep(1000);
		log.info("enabled job...");
		enableJob(jobConfiguration.getJobName());
		Thread.sleep(1000);

		try {
			waitForFinish(new FinishCheck() {

				@Override
				public boolean docheck() {
					String count = getJobNode(jobConfiguration,
							"servers/" + saturnExecutorList.get(0).getExecutorName() + "/processSuccessCount");
					log.info("success count: {}", count);
					int cc = Integer.parseInt(count);
					if( cc > 0){
						return true;
					}
					return false;
				}
			}, 15);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		disableJob(jobConfiguration.getJobName());
	}

}
