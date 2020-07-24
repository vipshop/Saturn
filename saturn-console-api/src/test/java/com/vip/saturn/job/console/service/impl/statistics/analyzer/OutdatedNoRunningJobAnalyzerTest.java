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

package com.vip.saturn.job.console.service.impl.statistics.analyzer;

import com.vip.saturn.job.console.domain.AbnormalJob;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.helper.DashboardConstants;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OutdatedNoRunningJobAnalyzerTest {

	@Mock
	private CuratorRepository.CuratorFrameworkOp curatorFrameworkOp;

	@InjectMocks
	private OutdatedNoRunningJobAnalyzer outdatedNoRunningJobAnalyzer;

	private String namespace = "saturn-job-test.vip.com";

	private String jobName = "testJob";

	/**
	 * 测试mayBlockWaitingRunningItemEnd方法
	 */
	@Test
	public void testMayBlockWaitingRunningItemEnd() throws Exception {
		long currentTime = System.currentTimeMillis();
		long nextFireTime = currentTime - 1000L * 60L * 60L * 24L * 5L;
		long failoverMTime = currentTime - DashboardConstants.NOT_RUNNING_WARN_DELAY_MS_WHEN_JOB_RUNNING;

		//反射参数
		Class[] parameterTyps = {CuratorRepository.CuratorFrameworkOp.class, AbnormalJob.class, long.class, String.class};
		AbnormalJob abnormalJob = new AbnormalJob(jobName, null, null, null);
		Object[] invokeParams = {curatorFrameworkOp, abnormalJob, nextFireTime, "2"};
		String methodName = "mayBlockWaitingRunningItemEnd";

		//1.没有running的item，返回false
		when(curatorFrameworkOp.getChildren(eq("/$Jobs/testJob/execution"))).thenReturn(Arrays.asList("0", "1", "2"));
		when(curatorFrameworkOp.checkExists(anyString())).thenReturn(false);
		Object result = MethodUtils
				.invokeMethod(outdatedNoRunningJobAnalyzer, true, methodName, invokeParams, parameterTyps);
		Assert.assertEquals(false, result);

		//2.加上NOT_RUNNING_WARN_DELAY_MS_WHEN_JOB_RUNNING也已过期，返回false
		when(curatorFrameworkOp.checkExists("/$Jobs/testJob/execution/0/running")).thenReturn(true);
		when(curatorFrameworkOp.getMtime("/$Jobs/testJob/execution/2/failover")).thenReturn(failoverMTime);
		result = MethodUtils
				.invokeMethod(outdatedNoRunningJobAnalyzer, true, methodName, invokeParams, parameterTyps);
		Assert.assertEquals(false, result);

		//3.加上NOT_RUNNING_WARN_DELAY_MS_WHEN_JOB_RUNNING后未过期，返回true
		when(curatorFrameworkOp.getMtime("/$Jobs/testJob/execution/2/failover")).thenReturn(failoverMTime + 1000);
		result = MethodUtils
				.invokeMethod(outdatedNoRunningJobAnalyzer, true, methodName, invokeParams, parameterTyps);
		Assert.assertEquals(true, result);
	}

}
