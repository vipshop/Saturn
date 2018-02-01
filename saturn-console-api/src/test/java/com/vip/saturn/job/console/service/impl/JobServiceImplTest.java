package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.domain.ExecutionInfo;
import com.vip.saturn.job.console.domain.ExecutionInfo.ExecutionStatus;
import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.JobNodePath;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JobServiceImplTest {

	@Mock
	private CuratorFrameworkOp curatorFrameworkOp;

	@Mock
	private CurrentJobConfigService currentJobConfigService;

	@Mock
	private RegistryCenterService registryCenterService;

	@InjectMocks
	private JobServiceImpl jobService;

	@Test
	public void testGetExecutionStatusSuccessfully() throws Exception {
		String namespace = "ns1";
		String jobName = "jobA";
		String executorName = "exec1";
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName))
				.thenReturn(buildJobConfig4DB(namespace, jobName));

		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);

		List<String> shardItems = Lists.newArrayList("0", "1", "2", "3", "4");
		when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName))).thenReturn(shardItems);

		when(curatorFrameworkOp.getChildren(JobNodePath.getServerNodePath(jobName)))
				.thenReturn(Lists.newArrayList(executorName));
		// 3个分片
		when(curatorFrameworkOp.getData(JobNodePath.getServerSharding(jobName, executorName)))
				.thenReturn("0,1,2,3,4");

		when(curatorFrameworkOp.checkExists(JobNodePath.getEnabledReportNodePath(jobName)))
				.thenReturn(true);
		when(curatorFrameworkOp.getData(JobNodePath.getEnabledReportNodePath(jobName)))
				.thenReturn("true");
		// 0号分片running
		mockExecutionStatusNode(true, false, false, false, false, executorName, jobName, "0");
		// 1号分片completed
		mockExecutionStatusNode(false, true, false, false, false, executorName, jobName, "1");
		// 2号分片fail
		mockExecutionStatusNode(false, true, false, true, false, executorName, jobName, "2");
		// 3号分片failover
		mockExecutionStatusNode(true, false, true, false, false, executorName, jobName, "3");
		// 4号分片timeout
		mockExecutionStatusNode(false, true, false, false, true, executorName, jobName, "4");

		mockJobMessage(jobName, "0", "this is message");
		mockJobMessage(jobName, "1", "this is message");
		mockJobMessage(jobName, "2", "this is message");
		mockJobMessage(jobName, "3", "this is message");
		mockJobMessage(jobName, "4", "this is message");

		mockTimezone(jobName, "Asia/Shanghai");

		mockExecutionNodeData(jobName, "0", "lastBeginTime", "0");
		mockExecutionNodeData(jobName, "0", "nextFireTime", "2000");
		mockExecutionNodeData(jobName, "0", "lastCompleteTime", "1000");

		mockExecutionNodeData(jobName, "1", "lastBeginTime", "0");
		mockExecutionNodeData(jobName, "1", "nextFireTime", "2000");
		mockExecutionNodeData(jobName, "1", "lastCompleteTime", "1000");

		mockExecutionNodeData(jobName, "2", "lastBeginTime", "0");
		mockExecutionNodeData(jobName, "2", "nextFireTime", "2000");
		mockExecutionNodeData(jobName, "2", "lastCompleteTime", "1000");

		mockExecutionNodeData(jobName, "3", "lastBeginTime", "0");
		mockExecutionNodeData(jobName, "3", "nextFireTime", "2000");
		mockExecutionNodeData(jobName, "3", "lastCompleteTime", "1000");

		mockExecutionNodeData(jobName, "4", "nextFireTime", "2000");
		mockExecutionNodeData(jobName, "4", "lastCompleteTime", "1000");

		List<ExecutionInfo> result = jobService.getExecutionStatus(namespace, jobName);

		assertEquals("size should be 5", 5, result.size());
		// verify 0号分片
		ExecutionInfo executionInfo = result.get(0);

		assertEquals("executorName not equal", executorName, executionInfo.getExecutorName());
		assertEquals("jobName not equal", jobName, executionInfo.getJobName());
		assertEquals("status not equal", ExecutionStatus.RUNNING, executionInfo.getStatus());
		assertEquals("jobMsg not equal", "this is message", executionInfo.getJobMsg());
		assertFalse("failover should be false", executionInfo.getFailover());
		assertEquals("lastbeginTime not equal", "1970-01-01 08:00:00", executionInfo.getLastBeginTime());
		assertEquals("nextFireTime not equal", "1970-01-01 08:00:02", executionInfo.getNextFireTime());
		assertEquals("lastCompleteTime not equal", "1970-01-01 08:00:01", executionInfo.getLastCompleteTime());
		// verify 1号分片
		executionInfo = result.get(1);

		assertEquals("executorName not equal", executorName, executionInfo.getExecutorName());
		assertEquals("jobName not equal", jobName, executionInfo.getJobName());
		assertEquals("status not equal", ExecutionStatus.COMPLETED, executionInfo.getStatus());
		assertEquals("jobMsg not equal", "this is message", executionInfo.getJobMsg());
		assertFalse("failover should be false", executionInfo.getFailover());
		assertEquals("lastbeginTime not equal", "1970-01-01 08:00:00", executionInfo.getLastBeginTime());
		assertEquals("nextFireTime not equal", "1970-01-01 08:00:02", executionInfo.getNextFireTime());
		assertEquals("lastCompleteTime not equal", "1970-01-01 08:00:01", executionInfo.getLastCompleteTime());

		// verify 2号分片
		executionInfo = result.get(2);

		assertEquals("executorName not equal", executorName, executionInfo.getExecutorName());
		assertEquals("jobName not equal", jobName, executionInfo.getJobName());
		assertEquals("status not equal", ExecutionStatus.FAILED, executionInfo.getStatus());
		assertEquals("jobMsg not equal", "this is message", executionInfo.getJobMsg());
		assertFalse("failover should be false", executionInfo.getFailover());
		assertEquals("lastbeginTime not equal", "1970-01-01 08:00:00", executionInfo.getLastBeginTime());
		assertEquals("nextFireTime not equal", "1970-01-01 08:00:02", executionInfo.getNextFireTime());
		assertEquals("lastCompleteTime not equal", "1970-01-01 08:00:01", executionInfo.getLastCompleteTime());

		// verify 3号分片
		executionInfo = result.get(3);

		assertEquals("executorName not equal", executorName, executionInfo.getExecutorName());
		assertEquals("jobName not equal", jobName, executionInfo.getJobName());
		assertEquals("status not equal", ExecutionStatus.RUNNING, executionInfo.getStatus());
		assertEquals("jobMsg not equal", "this is message", executionInfo.getJobMsg());
		assertTrue("failover should be false", executionInfo.getFailover());
		assertEquals("lastbeginTime not equal", "1970-01-01 08:00:00", executionInfo.getLastBeginTime());
		assertEquals("nextFireTime not equal", "1970-01-01 08:00:02", executionInfo.getNextFireTime());
		assertEquals("lastCompleteTime not equal", "1970-01-01 08:00:01", executionInfo.getLastCompleteTime());

		// verify 4号分片
		executionInfo = result.get(4);

		assertEquals("executorName not equal", executorName, executionInfo.getExecutorName());
		assertEquals("jobName not equal", jobName, executionInfo.getJobName());
		assertEquals("status not equal", ExecutionStatus.TIMEOUT, executionInfo.getStatus());
		assertEquals("jobMsg not equal", "this is message", executionInfo.getJobMsg());
		assertFalse("failover should be false", executionInfo.getFailover());
		assertEquals("nextFireTime not equal", "1970-01-01 08:00:02", executionInfo.getNextFireTime());
		assertEquals("lastCompleteTime not equal", "1970-01-01 08:00:01", executionInfo.getLastCompleteTime());

	}

	private void mockExecutionNodeData(String jobName, String item, String nodeName, String data) {
		when(curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, nodeName)))
				.thenReturn(data);
	}

	private void mockJobMessage(String jobName, String item, String msg) {
		when(curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "jobMsg")))
				.thenReturn(msg);
	}

	private void mockTimezone(String jobName, String timezone) {
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeZone")))
				.thenReturn(timezone);
	}

	private void mockExecutionStatusNode(boolean isRunning, boolean isCompleted, boolean isFailover, boolean isFailed,
			boolean isTimeout, String executorName, String jobName, String jobItem) {
		if (isRunning) {
			when(curatorFrameworkOp.getData(JobNodePath.getRunningNodePath(jobName, jobItem)))
					.thenReturn(executorName);
		}

		if (isCompleted) {
			when(curatorFrameworkOp.getData(JobNodePath.getCompletedNodePath(jobName, jobItem)))
					.thenReturn(executorName);
		}

		if (isFailover) {
			when(curatorFrameworkOp.getData(JobNodePath.getFailoverNodePath(jobName, jobItem)))
					.thenReturn(executorName);
			when(curatorFrameworkOp.getMtime(JobNodePath.getFailoverNodePath(jobName, jobItem)))
					.thenReturn(1L);
		}

		if (isFailed) {
			when(curatorFrameworkOp.checkExists(JobNodePath.getFailedNodePath(jobName, jobItem)))
					.thenReturn(true);
		}

		if (isTimeout) {
			when(curatorFrameworkOp.checkExists(JobNodePath.getTimeoutNodePath(jobName, jobItem)))
					.thenReturn(true);
		}
	}

	private JobConfig4DB buildJobConfig4DB(String namespace, String jobName) {
		JobConfig4DB config = new JobConfig4DB();
		config.setNamespace(namespace);
		config.setJobName(jobName);
		config.setEnabled(true);
		return config;
	}
}