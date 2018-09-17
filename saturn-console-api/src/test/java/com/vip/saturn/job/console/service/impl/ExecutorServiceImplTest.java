package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.console.domain.ServerRunningInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.JobNodePath;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static com.vip.saturn.job.console.utils.JobNodePath.$JOBS_NODE_NAME;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExecutorServiceImplTest {

	@Mock
	private CuratorRepository.CuratorFrameworkOp curatorFrameworkOp;

	@Mock
	private RegistryCenterService registryCenterService;

	@Mock
	private JobService jobService;

	@InjectMocks
	private ExecutorServiceImpl executorService;

	@Test
	public void getExecutorRunningInfo() throws SaturnJobConsoleException {
		String namespace = "www.abc.com";
		String executorName = "exec01";

		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);

		JobConfig jobConfigA = new JobConfig();
		jobConfigA.setJobName("jobA");
		jobConfigA.setJobType(JobType.JAVA_JOB.name());
		jobConfigA.setEnabledReport(Boolean.TRUE);
		jobConfigA.setFailover(Boolean.TRUE);
		jobConfigA.setLocalMode(Boolean.FALSE);

		JobConfig jobConfigB = new JobConfig();
		jobConfigB.setJobName("jobB");
		jobConfigB.setJobType(JobType.MSG_JOB.name());
		jobConfigB.setEnabledReport(Boolean.FALSE);
		jobConfigB.setFailover(Boolean.FALSE);
		jobConfigB.setLocalMode(Boolean.FALSE);

		JobConfig jobConfigC = new JobConfig();
		jobConfigC.setJobName("jobC");
		jobConfigC.setJobType(JobType.JAVA_JOB.name());
		jobConfigC.setEnabledReport(Boolean.TRUE);
		jobConfigC.setFailover(Boolean.FALSE);
		jobConfigC.setLocalMode(Boolean.FALSE);

		when(jobService.getUnSystemJobs(namespace)).thenReturn(Lists.newArrayList(jobConfigA, jobConfigB, jobConfigC));

		when(curatorFrameworkOp.checkExists(String.format("/%s/%s/servers", $JOBS_NODE_NAME, jobConfigA.getJobName())))
				.thenReturn(true);
		when(curatorFrameworkOp.checkExists(String.format("/%s/%s/servers", $JOBS_NODE_NAME, jobConfigB.getJobName())))
				.thenReturn(true);
		when(curatorFrameworkOp.checkExists(String.format("/%s/%s/servers", $JOBS_NODE_NAME, jobConfigC.getJobName())))
				.thenReturn(true);

		when(curatorFrameworkOp.getData(
				String.format("%s/%s/%s", JobNodePath.getServerNodePath(jobConfigA.getJobName()), executorName,
						"sharding"))).thenReturn("1,2");
		when(curatorFrameworkOp.getData(
				String.format("%s/%s/%s", JobNodePath.getServerNodePath(jobConfigB.getJobName()), executorName,
						"sharding"))).thenReturn("2,3");
		when(curatorFrameworkOp.getData(
				String.format("%s/%s/%s", JobNodePath.getServerNodePath(jobConfigC.getJobName()), executorName,
						"sharding"))).thenReturn("0,1");


		when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath("jobA")))
				.thenReturn(Lists.newArrayList("0", "1", "2", "3"));
		when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath("jobB")))
				.thenReturn(Lists.newArrayList("0", "1", "2", "3"));
		when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath("jobC")))
				.thenReturn(Lists.newArrayList("0", "1", "2", "3"));

		when(curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath("jobA", "0", "running"))).thenReturn(true);
		when(curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath("jobA", "0", "failover")))
				.thenReturn(executorName);

		when(curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath("jobA", "1", "running"))).thenReturn(true);
		when(curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath("jobA", "2", "running"))).thenReturn(true);
		when(curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath("jobA", "3", "running"))).thenReturn(true);
		when(curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath("jobA", "3", "failover")))
				.thenReturn("other-exec");

		when(curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath("jobC", "0", "running")))
				.thenReturn(false);
		when(curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath("jobC", "1", "running"))).thenReturn(true);
		when(curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath("jobC", "2", "running"))).thenReturn(true);
		when(curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath("jobC", "3", "running"))).thenReturn(true);

		ServerRunningInfo serverRunningInfo = executorService.getExecutorRunningInfo(namespace, executorName);

		assertEquals(2, serverRunningInfo.getRunningJobItems().size());
		assertEquals("0,1,2", serverRunningInfo.getRunningJobItems().get("jobA"));
		assertEquals("1", serverRunningInfo.getRunningJobItems().get("jobC"));

		assertEquals(1, serverRunningInfo.getPotentialRunningJobItems().size());
		assertEquals("2,3", serverRunningInfo.getPotentialRunningJobItems().get("jobB"));
	}
}