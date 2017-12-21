package com.vip.saturn.job.console.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.vip.saturn.job.integrate.service.ReportAlarmService;
import org.apache.curator.framework.CuratorFramework;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.vip.saturn.job.console.AbstractSaturnConsoleTest;
import com.vip.saturn.job.console.domain.JobBriefInfo;
import com.vip.saturn.job.console.domain.JobServer;
import com.vip.saturn.job.console.domain.JobStatus;
import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.ServerStatus;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.service.JobOperationService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.JobNodePath;

/**
 * Created by kfchu on 31/05/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class RestApiServiceImplTest extends AbstractSaturnConsoleTest {

	private final static String TEST_NAME_SPACE_NAME = "testDomain";

	@Mock
	private RegistryCenterService registryCenterService;

	@Mock
	private CuratorRepository curatorRepository;

	@Mock
	private JobDimensionService jobDimensionService;

	@Mock
	private JobOperationService jobOperationService;

	@Mock
	private ReportAlarmService reportAlarmService;

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
	public void testCreateJobSuccessfully() {

	}

	@Test
	public void testRunAtOnceSuccessfully() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobDimensionService.getJobStatus(jobName, curatorFrameworkOp)).thenReturn(JobStatus.READY);

		List<JobServer> servers = Lists.newArrayList();
		JobServer jobServer = createJobServer("job1");
		servers.add(jobServer);
		when(jobDimensionService.getServers(jobName, curatorFrameworkOp)).thenReturn(servers);

		// run
		restApiService.runJobAtOnce(TEST_NAME_SPACE_NAME, jobName);

		// verify
		verify(jobOperationService).runAtOnceByJobnameAndExecutorName(jobName, jobServer.getExecutorName(),
				curatorFrameworkOp);
	}

	@Test
	public void testRunAtOnceFailAsJobStatusIsNotReady() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobDimensionService.getJobStatus(jobName, curatorFrameworkOp)).thenReturn(JobStatus.RUNNING);

		// run
		try {
			restApiService.runJobAtOnce(TEST_NAME_SPACE_NAME, jobName);
		} catch (SaturnJobConsoleHttpException e) {
			assertEquals("status code is not 400", 400, e.getStatusCode());
			assertEquals("error message is not equals", "job' status is not {READY}", e.getMessage());
		}

		// verify
		verify(jobOperationService, times(0)).runAtOnceByJobnameAndExecutorName(anyString(), anyString(),
				any(CuratorRepository.CuratorFrameworkOp.class));
	}

	@Test
	public void testRunAtOnceFailAsNoExecutorFound() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobDimensionService.getJobStatus(jobName, curatorFrameworkOp)).thenReturn(JobStatus.READY);

		List<JobServer> servers = Lists.newArrayList();
		when(jobDimensionService.getServers(jobName, curatorFrameworkOp)).thenReturn(servers);

		// run
		try {
			restApiService.runJobAtOnce(TEST_NAME_SPACE_NAME, jobName);
		} catch (SaturnJobConsoleHttpException e) {
			assertEquals("status code is not 400", 400, e.getStatusCode());
			assertEquals("error message is not equals", "no executor found for this job", e.getMessage());
		}

		// verify
		verify(jobOperationService, times(0)).runAtOnceByJobnameAndExecutorName(anyString(), anyString(),
				any(CuratorRepository.CuratorFrameworkOp.class));
	}

	@Test
	public void testStopAtOnceSuccessfullyWhenJobIsAlreadyStopped() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobDimensionService.getJobStatus(jobName, curatorFrameworkOp)).thenReturn(JobStatus.STOPPED);

		// run
		restApiService.stopJobAtOnce(TEST_NAME_SPACE_NAME, jobName);

		// verify
		verify(jobOperationService, times(0)).stopAtOnceByJobnameAndExecutorName(anyString(), anyString(),
				any(CuratorRepository.CuratorFrameworkOp.class));
	}

	@Test
	public void testStopAtOnceSuccessfullyWhenJobStatusIsStoppingAndJobTypeIsMsg() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobDimensionService.getJobStatus(jobName, curatorFrameworkOp)).thenReturn(JobStatus.STOPPING);

		when(jobDimensionService.getJobType(jobName, curatorFrameworkOp))
				.thenReturn(JobBriefInfo.JobType.MSG_JOB.name());
		when(jobDimensionService.isJobEnabled(jobName, curatorFrameworkOp)).thenReturn(false);

		List<JobServer> servers = Lists.newArrayList();
		JobServer jobServer = createJobServer("job1");
		servers.add(jobServer);
		when(jobDimensionService.getServers(jobName, curatorFrameworkOp)).thenReturn(servers);

		// run
		restApiService.stopJobAtOnce(TEST_NAME_SPACE_NAME, jobName);

		// verify
		verify(jobOperationService).stopAtOnceByJobnameAndExecutorName(jobName, jobServer.getExecutorName(),
				curatorFrameworkOp);
	}

	@Test
	public void testStopAtOnceSuccessfullyWhenJobStatusIsStoppingAndJobTypeIsJava() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobDimensionService.getJobStatus(jobName, curatorFrameworkOp)).thenReturn(JobStatus.STOPPING);

		when(jobDimensionService.getJobType(jobName, curatorFrameworkOp))
				.thenReturn(JobBriefInfo.JobType.JAVA_JOB.name());

		List<JobServer> servers = Lists.newArrayList();
		JobServer jobServer = createJobServer("job1");
		servers.add(jobServer);
		when(jobDimensionService.getServers(jobName, curatorFrameworkOp)).thenReturn(servers);

		// run
		restApiService.stopJobAtOnce(TEST_NAME_SPACE_NAME, jobName);

		// verify
		verify(jobOperationService).stopAtOnceByJobnameAndExecutorName(jobName, jobServer.getExecutorName(),
				curatorFrameworkOp);
	}

	@Test
	public void testStopAtOnceFailForMsgJobAsJobIsEnable() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobDimensionService.getJobStatus(jobName, curatorFrameworkOp)).thenReturn(JobStatus.STOPPING);

		when(jobDimensionService.getJobType(jobName, curatorFrameworkOp))
				.thenReturn(JobBriefInfo.JobType.MSG_JOB.name());
		when(jobDimensionService.isJobEnabled(jobName, curatorFrameworkOp)).thenReturn(true);

		List<JobServer> servers = Lists.newArrayList();
		JobServer jobServer = createJobServer("job1");
		servers.add(jobServer);
		when(jobDimensionService.getServers(jobName, curatorFrameworkOp)).thenReturn(servers);

		// run
		try {
			restApiService.stopJobAtOnce(TEST_NAME_SPACE_NAME, jobName);
		} catch (SaturnJobConsoleHttpException e) {
			assertEquals("status code is not 400", 400, e.getStatusCode());
			assertEquals("error message is not equals", "job cannot be stopped while it is enable", e.getMessage());
		}

		// verify
		verify(jobOperationService, times(0)).stopAtOnceByJobnameAndExecutorName(jobName, jobServer.getExecutorName(),
				curatorFrameworkOp);
	}

	@Test
	public void testStopAtOnceFailForJavaJobAsNoExecutor() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobDimensionService.getJobStatus(jobName, curatorFrameworkOp)).thenReturn(JobStatus.STOPPING);

		when(jobDimensionService.getJobType(jobName, curatorFrameworkOp))
				.thenReturn(JobBriefInfo.JobType.JAVA_JOB.name());

		List<JobServer> servers = Lists.newArrayList();
		when(jobDimensionService.getServers(jobName, curatorFrameworkOp)).thenReturn(servers);

		// run
		try {
			restApiService.stopJobAtOnce(TEST_NAME_SPACE_NAME, jobName);
		} catch (SaturnJobConsoleHttpException e) {
			assertEquals("status code is not 400", 400, e.getStatusCode());
			assertEquals("error message is not equals", "no executor found for this job", e.getMessage());
		}

		// verify
		verify(jobOperationService, times(0)).stopAtOnceByJobnameAndExecutorName(anyString(), anyString(),
				any(CuratorRepository.CuratorFrameworkOp.class));
	}

	@Test
	public void testStopAtOnceFailForJavaJobAsStatusIsNotStopping() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobDimensionService.getJobStatus(jobName, curatorFrameworkOp)).thenReturn(JobStatus.READY);

		when(jobDimensionService.getJobType(jobName, curatorFrameworkOp))
				.thenReturn(JobBriefInfo.JobType.JAVA_JOB.name());

		List<JobServer> servers = Lists.newArrayList();
		when(jobDimensionService.getServers(jobName, curatorFrameworkOp)).thenReturn(servers);

		// run
		try {
			restApiService.stopJobAtOnce(TEST_NAME_SPACE_NAME, jobName);
		} catch (SaturnJobConsoleHttpException e) {
			assertEquals("status code is not 400", 400, e.getStatusCode());
			assertEquals("error message is not equals", "job cannot be stopped while its status is READY or RUNNING",
					e.getMessage());
		}

		// verify
		verify(jobOperationService, times(0)).stopAtOnceByJobnameAndExecutorName(anyString(), anyString(),
				any(CuratorRepository.CuratorFrameworkOp.class));
	}

	@Test
	public void testDeleteJobSuccessfully() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobDimensionService.getJobStatus(jobName, curatorFrameworkOp)).thenReturn(JobStatus.STOPPED);

		// run
		restApiService.deleteJob(TEST_NAME_SPACE_NAME, jobName);

		// verify
		verify(jobOperationService).deleteJob(jobName, curatorFrameworkOp);
	}

	@Test
	public void testDeleteJobFailAsStatusIsNotStopped() throws SaturnJobConsoleException {
		// prepare
		String jobName = "testJob";
		when(jobDimensionService.getJobStatus(jobName, curatorFrameworkOp)).thenReturn(JobStatus.STOPPING);

		// run
		try {
			restApiService.deleteJob(TEST_NAME_SPACE_NAME, jobName);
		} catch (SaturnJobConsoleHttpException e) {
			assertEquals("status code is not 400", 400, e.getStatusCode());
			assertEquals("error message is not equals", "job' status is not {STOPPED}", e.getMessage());
		}

		// verify
		verify(jobOperationService, times(0)).deleteJob(jobName, curatorFrameworkOp);
	}

	private JobServer createJobServer(String name) {
		JobServer jobServer = new JobServer();
		jobServer.setJobName(name);
		jobServer.setExecutorName("exec-" + name);
		jobServer.setStatus(ServerStatus.ONLINE);

		return jobServer;
	}

}