/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

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
