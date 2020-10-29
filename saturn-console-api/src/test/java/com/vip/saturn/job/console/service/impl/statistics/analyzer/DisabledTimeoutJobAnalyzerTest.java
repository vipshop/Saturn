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

import com.vip.saturn.job.console.domain.DisabledTimeoutAlarmJob;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.impl.JobServiceImpl;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.integrate.service.ReportAlarmService;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DisabledTimeoutJobAnalyzerTest {

	@Mock
	private CuratorRepository.CuratorFrameworkOp curatorFrameworkOp;

	@Mock
	private ReportAlarmService reportAlarmService;

	@InjectMocks
	private DisabledTimeoutJobAnalyzer disabledTimeoutJobAnalyzer;

	private String namespace = "saturn-job-test.vip.com";

	private String jobName = "testJob";

	/**
	 * 测试isDisabledTimeout方法
	 */
	@Test
	public void testIsDisabledTimeout() throws Exception{
		//反射参数
		Class[] parameterTyps = {DisabledTimeoutAlarmJob.class, List.class, CuratorRepository.CuratorFrameworkOp.class};
		DisabledTimeoutAlarmJob disabledTimeoutAlarmJob = new DisabledTimeoutAlarmJob(jobName, namespace, null, null);
		Object[] invokeParams = {disabledTimeoutAlarmJob, null, curatorFrameworkOp};
		String methodName = "isDisabledTimeout";

		// 1.当没禁用超时告警时间为0时返回false
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName,
				JobServiceImpl.CONFIG_DISABLE_TIMEOUT_SECONDS))).thenReturn("0");
		Object result = MethodUtils.invokeMethod(disabledTimeoutJobAnalyzer, true, methodName, invokeParams, parameterTyps);
		Assert.assertEquals(false, result);

		// 2.当作业处于启用状态时返回false
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName,
				JobServiceImpl.CONFIG_DISABLE_TIMEOUT_SECONDS))).thenReturn("600");
		when(curatorFrameworkOp.getData(SaturnExecutorsNode.getJobConfigEnableNodePath(jobName))).thenReturn("true");
		result = MethodUtils.invokeMethod(disabledTimeoutJobAnalyzer, true, methodName, invokeParams, parameterTyps);
		Assert.assertEquals(false, result);

		// 3.当还未达到超时时间时返回false
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName,
				JobServiceImpl.CONFIG_DISABLE_TIMEOUT_SECONDS))).thenReturn("600");
		when(curatorFrameworkOp.getData(SaturnExecutorsNode.getJobConfigEnableNodePath(jobName))).thenReturn("false");
		when(curatorFrameworkOp.getMtime(SaturnExecutorsNode.getJobConfigEnableNodePath(jobName))).thenReturn(System.currentTimeMillis());
		result = MethodUtils.invokeMethod(disabledTimeoutJobAnalyzer, true, methodName, invokeParams, parameterTyps);
		Assert.assertEquals(false, result);

		// 4.达到超时时间时返回true
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName,
				JobServiceImpl.CONFIG_DISABLE_TIMEOUT_SECONDS))).thenReturn("600");
		when(curatorFrameworkOp.getData(SaturnExecutorsNode.getJobConfigEnableNodePath(jobName))).thenReturn("false");
		when(curatorFrameworkOp.getMtime(SaturnExecutorsNode.getJobConfigEnableNodePath(jobName))).thenReturn(System.currentTimeMillis() - 601 * 1000);
		result = MethodUtils.invokeMethod(disabledTimeoutJobAnalyzer, true, methodName, invokeParams, parameterTyps);
		Assert.assertEquals(true, result);
		verify(reportAlarmService, times(1)).dashboardLongTimeDisabledJob(eq(namespace), eq(jobName), anyByte(), anyByte());

	}

}
