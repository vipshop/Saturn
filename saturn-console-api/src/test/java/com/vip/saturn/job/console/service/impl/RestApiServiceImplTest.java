package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.JobNodePath;
import org.apache.curator.framework.CuratorFramework;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by kfchu on 31/05/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class RestApiServiceImplTest {

	private final static String TEST_NAME_SPACE_NAME = "testDomain";

	@Mock
	private RegistryCenterService registryCenterService;

	@Mock
	private CuratorRepository curatorRepository;

	@Mock
	private JobService jobService;
	
	@Mock
	private CurrentJobConfigService currentJobConfigService;

	@Mock
	private CuratorRepository.CuratorFrameworkOp curatorFrameworkOp;

	@Mock
	private CuratorFramework curatorFramework;

	@InjectMocks
	private RestApiServiceImpl restApiService;

	@Before
	public void setUp() throws Exception {
		when(registryCenterService.findConfigByNamespace(anyString())).thenReturn(new RegistryCenterConfiguration());

		RegistryCenterClient registryCenterClient = new RegistryCenterClient();
		registryCenterClient.setCuratorClient(curatorFramework);
		registryCenterClient.setConnected(true);

		when(registryCenterService.connectByNamespace(anyString())).thenReturn(registryCenterClient);
		when(curatorRepository.newCuratorFrameworkOp(any(CuratorFramework.class))).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.checkExists(JobNodePath.getJobNodePath(anyString()))).thenReturn(true);
	}

	@Test
	public void testRunAtOnceSuccessfully() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobService.getJobStatus(TEST_NAME_SPACE_NAME, jobName)).thenReturn(JobStatus.READY);

		List<JobServer> servers = Lists.newArrayList();
		JobServer jobServer = createJobServer("job1");
		servers.add(jobServer);
		when(jobService.getJobServers(TEST_NAME_SPACE_NAME, jobName)).thenReturn(servers);

		List<JobServerStatus> jobServerStatusList = getJobServerStatus(servers);
		when(jobService.getJobServersStatus(TEST_NAME_SPACE_NAME, jobName)).thenReturn(jobServerStatusList);
		// run
		restApiService.runJobAtOnce(TEST_NAME_SPACE_NAME, jobName);
	}

	private List<JobServerStatus> getJobServerStatus(List<JobServer> servers) {
		List<JobServerStatus> result = Lists.newArrayList();
		for (JobServer server : servers) {
			JobServerStatus jobServerStatus = new JobServerStatus();
			jobServerStatus.setJobName(server.getJobName());
			jobServerStatus.setExecutorName(server.getExecutorName());
			jobServerStatus.setServerStatus(ServerStatus.ONLINE);

			result.add(jobServerStatus);
		}
		return result;
	}

	@Test
	public void testRunAtOnceFailAsJobStatusIsNotReady() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobService.getJobStatus(TEST_NAME_SPACE_NAME, jobName)).thenReturn(JobStatus.RUNNING);

		// run
		try {
			restApiService.runJobAtOnce(TEST_NAME_SPACE_NAME, jobName);
		} catch (SaturnJobConsoleHttpException e) {
			assertEquals("status code is not 400", 400, e.getStatusCode());
			assertEquals("error message is not equals", "job's status is not {READY}", e.getMessage());
		}
	}

	@Test
	public void testRunAtOnceFailAsNoExecutorFound() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobService.getJobStatus(TEST_NAME_SPACE_NAME, jobName)).thenReturn(JobStatus.READY);

		List<JobServer> servers = Lists.newArrayList();
		when(jobService.getJobServers(TEST_NAME_SPACE_NAME, jobName)).thenReturn(servers);

		// run
		try {
			restApiService.runJobAtOnce(TEST_NAME_SPACE_NAME, jobName);
		} catch (SaturnJobConsoleHttpException e) {
			assertEquals("status code is not 400", 400, e.getStatusCode());
			assertEquals("error message is not equals", "no executor found for this job", e.getMessage());
		}
	}

	@Test
	public void testStopAtOnceSuccessfullyWhenJobIsAlreadyStopped() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobService.getJobStatus(TEST_NAME_SPACE_NAME, jobName)).thenReturn(JobStatus.STOPPED);

		// run
		restApiService.stopJobAtOnce(TEST_NAME_SPACE_NAME, jobName);
	}

	@Test
	public void testStopAtOnceSuccessfullyWhenJobStatusIsStoppingAndJobTypeIsMsg() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobService.getJobStatus(TEST_NAME_SPACE_NAME, jobName)).thenReturn(JobStatus.STOPPING);

		List<JobServer> servers = Lists.newArrayList();
		JobServer jobServer = createJobServer("job1");
		servers.add(jobServer);
		when(jobService.getJobServers(TEST_NAME_SPACE_NAME, jobName)).thenReturn(servers);
		List<String> serverNameList = getJobServerNameList(servers);
		when(jobService.getJobServerList(TEST_NAME_SPACE_NAME, jobName)).thenReturn(serverNameList);
		// run
		restApiService.stopJobAtOnce(TEST_NAME_SPACE_NAME, jobName);
	}

	@Test
	public void testStopAtOnceSuccessfullyWhenJobStatusIsStoppingAndJobTypeIsJava() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobService.getJobStatus(TEST_NAME_SPACE_NAME, jobName)).thenReturn(JobStatus.STOPPING);

		List<JobServer> servers = Lists.newArrayList();
		JobServer jobServer = createJobServer("job1");
		servers.add(jobServer);
		when(jobService.getJobServers(TEST_NAME_SPACE_NAME, jobName)).thenReturn(servers);
		List<String> serverNameList = getJobServerNameList(servers);
		when(jobService.getJobServerList(TEST_NAME_SPACE_NAME, jobName)).thenReturn(serverNameList);

		// run
		restApiService.stopJobAtOnce(TEST_NAME_SPACE_NAME, jobName);
	}

	@Test
	public void testStopAtOnceFailForMsgJobAsJobIsEnable() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobService.getJobStatus(TEST_NAME_SPACE_NAME, jobName)).thenReturn(JobStatus.STOPPING);

		List<JobServer> servers = Lists.newArrayList();
		JobServer jobServer = createJobServer("job1");
		servers.add(jobServer);
		when(jobService.getJobServers(TEST_NAME_SPACE_NAME, jobName)).thenReturn(servers);

		List<String> serverNameList = getJobServerNameList(servers);
		when(jobService.getJobServerList(TEST_NAME_SPACE_NAME, jobName)).thenReturn(serverNameList);

		// run
		try {
			restApiService.stopJobAtOnce(TEST_NAME_SPACE_NAME, jobName);
		} catch (SaturnJobConsoleHttpException e) {
			assertEquals("status code is not 400", 400, e.getStatusCode());
			assertEquals("error message is not equals", "job cannot be stopped while it is enable", e.getMessage());
		}
	}

	private List<String> getJobServerNameList(List<JobServer> servers) {
		List<String> result = Lists.newArrayList();
		for (JobServer jobServer : servers) {
			result.add(jobServer.getExecutorName());
		}

		return result;
	}

	@Test
	public void testStopAtOnceFailForJavaJobAsNoExecutor() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobService.getJobStatus(TEST_NAME_SPACE_NAME, jobName)).thenReturn(JobStatus.STOPPING);

		List<JobServer> servers = Lists.newArrayList();
		when(jobService.getJobServers(TEST_NAME_SPACE_NAME, jobName)).thenReturn(servers);

		// run
		try {
			restApiService.stopJobAtOnce(TEST_NAME_SPACE_NAME, jobName);
		} catch (SaturnJobConsoleHttpException e) {
			assertEquals("status code is not 400", 400, e.getStatusCode());
			assertEquals("error message is not equals", "no executor found for this job", e.getMessage());
		}
	}

	@Test
	public void testStopAtOnceFailForJavaJobAsStatusIsNotStopping() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobService.getJobStatus(TEST_NAME_SPACE_NAME, jobName)).thenReturn(JobStatus.READY);

		List<JobServer> servers = Lists.newArrayList();
		when(jobService.getJobServers(TEST_NAME_SPACE_NAME, jobName)).thenReturn(servers);

		// run
		try {
			restApiService.stopJobAtOnce(TEST_NAME_SPACE_NAME, jobName);
		} catch (SaturnJobConsoleHttpException e) {
			assertEquals("status code is not 400", 400, e.getStatusCode());
			assertEquals("error message is not equals", "job cannot be stopped while its status is READY or RUNNING",
					e.getMessage());
		}
	}

	@Test
	public void testDeleteJobSuccessfully() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobService.getJobStatus(TEST_NAME_SPACE_NAME, jobName)).thenReturn(JobStatus.STOPPED);

		// run
		restApiService.deleteJob(TEST_NAME_SPACE_NAME, jobName);

		// verify
		verify(jobService).removeJob(TEST_NAME_SPACE_NAME, jobName);
	}

	@Test
	public void testDeleteJobFailAsStatusIsNotStopped() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobService.getJobStatus(TEST_NAME_SPACE_NAME, jobName)).thenReturn(JobStatus.STOPPING);

		// run
		try {
			restApiService.deleteJob(TEST_NAME_SPACE_NAME, jobName);
		} catch (SaturnJobConsoleHttpException e) {
			assertEquals("status code is not 400", 400, e.getStatusCode());
			assertEquals("error message is not equals", "job's status is not {STOPPED}", e.getMessage());
		}

		// verify
		verify(jobService, times(0)).removeJob(TEST_NAME_SPACE_NAME, jobName);
	}

	@Test
	public void testUpdateJobSuccessfully() throws SaturnJobConsoleException {
		String jobName = "testJob";
		JobConfig jobConfig = buildUpdateJobConfig(jobName);
		when(jobService.getJobStatus(TEST_NAME_SPACE_NAME, jobName)).thenReturn(JobStatus.STOPPED);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(TEST_NAME_SPACE_NAME, jobName))
				.thenReturn(buildJobConfig4DB(TEST_NAME_SPACE_NAME, jobName));

		// run
		restApiService.updateJob(TEST_NAME_SPACE_NAME, jobName, jobConfig);
	}

	@Test
	public void testUpdateJobFailAsSaturnsIsNotStopped() throws SaturnJobConsoleException {
		String jobName = "testJob";
		when(jobService.getJobStatus(TEST_NAME_SPACE_NAME, jobName)).thenReturn(JobStatus.STOPPING);

		// run
		try {
			restApiService.updateJob(TEST_NAME_SPACE_NAME, jobName, buildUpdateJobConfig(jobName));
		} catch (SaturnJobConsoleHttpException e) {
			assertEquals("status code is not 400", 400, e.getStatusCode());
			assertEquals("error message is not equals", "job's status is not {STOPPED}", e.getMessage());
		}
	}

	private JobServer createJobServer(String name) {
		JobServer jobServer = new JobServer();
		jobServer.setJobName(name);
		jobServer.setExecutorName("exec-" + name);
		jobServer.setStatus(ServerStatus.ONLINE);

		return jobServer;
	}

	private JobConfig buildUpdateJobConfig(String name) {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName("name");
		jobConfig.setShardingItemParameters("0=0");
		jobConfig.setShardingTotalCount(2);
		return jobConfig;
	}

	private JobConfig4DB buildJobConfig4DB(String namespace, String jobName) {
		JobConfig4DB config = new JobConfig4DB();
		config.setNamespace(namespace);
		config.setJobName(jobName);
		config.setEnabled(false);
		config.setEnabledReport(true);
		config.setJobType(JobType.JAVA_JOB.toString());
		return config;
	}
}