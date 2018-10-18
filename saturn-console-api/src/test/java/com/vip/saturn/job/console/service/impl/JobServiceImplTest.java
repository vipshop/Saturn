package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.domain.ExecutionInfo.ExecutionStatus;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.JobConfig4DB;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.SaturnConstants;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import org.apache.zookeeper.data.Stat;
import org.assertj.core.util.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.vip.saturn.job.console.service.impl.JobServiceImpl.CONFIG_ITEM_ENABLED;
import static com.vip.saturn.job.console.service.impl.JobServiceImpl.CONFIG_ITEM_PREFER_LIST;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JobServiceImplTest {

	@Mock
	private CuratorFrameworkOp curatorFrameworkOp;

	@Mock
	private CurrentJobConfigService currentJobConfigService;

	@Mock
	private RegistryCenterService registryCenterService;

	@Mock
	private SystemConfigService systemConfigService;

	@InjectMocks
	private JobServiceImpl jobService;

	private String namespace = "saturn-job-test.vip.com";

	private String jobName = "testJob";

	private String userName = "weicong01.li";


	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testGetGroup() {
		when(currentJobConfigService.findConfigsByNamespace(namespace))
				.thenReturn(Lists.newArrayList(new JobConfig4DB()));
		assertEquals(jobService.getGroups(namespace).size(), 1);
	}

	@Test
	public void testGetDependingJobs() throws SaturnJobConsoleException {
		String dependedJob = "dependedJob";
		String dependingJob = "dependingJob";
		JobConfig4DB dependedJobConfig = new JobConfig4DB();
		dependedJobConfig.setJobName(dependedJob);
		dependedJobConfig.setDependencies(dependingJob);
		JobConfig4DB dependingJobConfig = new JobConfig4DB();
		dependingJobConfig.setJobName(dependingJob);
		dependingJobConfig.setEnabled(Boolean.TRUE);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, dependedJob))
				.thenReturn(dependedJobConfig);
		when(currentJobConfigService.findConfigsByNamespace(namespace))
				.thenReturn(Lists.newArrayList(dependedJobConfig, dependingJobConfig));
		List<DependencyJob> dependingJobs = jobService.getDependingJobs(namespace, dependedJob);
		assertEquals(dependingJobs.size(), 1);
		assertEquals(dependingJobs.get(0).getJobName(), dependingJob);

		// test not exist job
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, dependedJob)).thenReturn(null);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("不能获取该作业（%s）依赖的所有作业，因为该作业不存在", dependedJob));
		jobService.getDependingJobs(namespace, dependedJob);
	}

	@Test
	public void testGetDependedJobs() throws SaturnJobConsoleException {
		String dependedJob = "dependedJob";
		String dependingJob = "dependingJob";
		JobConfig4DB dependedJobConfig = new JobConfig4DB();
		dependedJobConfig.setJobName(dependedJob);
		dependedJobConfig.setEnabled(Boolean.TRUE);
		dependedJobConfig.setDependencies(dependingJob);
		JobConfig4DB dependingJobConfig = new JobConfig4DB();
		dependingJobConfig.setJobName(dependingJob);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, dependingJob))
				.thenReturn(dependingJobConfig);
		when(currentJobConfigService.findConfigsByNamespace(namespace))
				.thenReturn(Lists.newArrayList(dependedJobConfig, dependingJobConfig));
		List<DependencyJob> dependingJobs = jobService.getDependedJobs(namespace, dependingJob);
		assertEquals(dependingJobs.size(), 1);
		assertEquals(dependingJobs.get(0).getJobName(), dependedJob);

		// test not exist job
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, dependingJob)).thenReturn(null);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("不能获取依赖该作业（%s）的所有作业，因为该作业不存在", dependingJob));
		jobService.getDependedJobs(namespace, dependingJob);
	}

	@Test
	public void testEnableJobFailByJobNotExist() throws SaturnJobConsoleException {
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(null);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("不能启用该作业（%s），因为该作业不存在", jobName));
		jobService.enableJob(namespace, jobName, userName);
	}

	@Test
	public void testEnableJobFailByJobHasEnabled() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(Boolean.TRUE);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("该作业（%s）已经处于启用状态", jobName));
		jobService.enableJob(namespace, jobName, userName);
	}

	@Test
	public void testEnabledJobFailByJobHasFinished() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(Boolean.FALSE);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName)))
				.thenReturn(Lists.newArrayList("1"));
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getExecutionNodePath(jobName, "1", "completed"))))
				.thenReturn(false);
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getExecutionNodePath(jobName, "1", "running"))))
				.thenReturn(true);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("不能启用该作业（%s），因为该作业不处于STOPPED状态", jobName));
		jobService.enableJob(namespace, jobName, userName);
	}

	@Test
	public void testEnabledJobSuccess() throws Exception {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(Boolean.FALSE);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName)))
				.thenReturn(Lists.newArrayList("1"));
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getExecutionNodePath(jobName, "1", "completed"))))
				.thenReturn(true);
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getExecutionNodePath(jobName, "1", "running"))))
				.thenReturn(false);
		jobService.enableJob(namespace, jobName, userName);
		verify(currentJobConfigService).updateByPrimaryKey(jobConfig4DB);
		verify(curatorFrameworkOp).update(eq(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_ENABLED)), eq(true));
	}

	@Test
	public void testDisableJobFailByJobNotExist() throws SaturnJobConsoleException {
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(null);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("不能禁用该作业（%s），因为该作业不存在", jobName));
		jobService.disableJob(namespace, jobName, userName);
	}

	@Test
	public void testDisableJobFailByJobHasDisabled() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(Boolean.FALSE);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("该作业（%s）已经处于禁用状态", jobName));
		jobService.disableJob(namespace, jobName, userName);
	}

	@Test
	public void testDisableJobFailByUpdateError() throws Exception {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(Boolean.TRUE);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(currentJobConfigService.updateByPrimaryKey(jobConfig4DB))
				.thenThrow(new SaturnJobConsoleException("update error"));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("update error");
		jobService.disableJob(namespace, jobName, userName);
	}

	@Test
	public void testDisableJobSuccess() throws Exception {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(Boolean.TRUE);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		jobService.disableJob(namespace, jobName, userName);
		verify(currentJobConfigService).updateByPrimaryKey(jobConfig4DB);
		verify(curatorFrameworkOp).update(eq(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_ENABLED)), eq(false));
	}

	@Test
	public void testRemoveJobFailByNotExist() throws SaturnJobConsoleException {
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(null);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("不能删除该作业（%s），因为该作业不存在", jobName));
		jobService.removeJob(namespace, jobName);
	}

	@Test
	public void testRemoveJobFailByNotStopped() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(Boolean.TRUE);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName)))
				.thenReturn(Lists.newArrayList("1"));
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getExecutionNodePath(jobName, "1", "completed"))))
				.thenReturn(true);
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getExecutionNodePath(jobName, "1", "running"))))
				.thenReturn(false);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("不能删除该作业(%s)，因为该作业不处于STOPPED状态", jobName));
		jobService.removeJob(namespace, jobName);
	}

	@Test
	public void testRemoveJobFailByLimitTime() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(Boolean.FALSE);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName)))
				.thenReturn(Lists.newArrayList("1"));
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getExecutionNodePath(jobName, "1", "completed"))))
				.thenReturn(true);
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getExecutionNodePath(jobName, "1", "running"))))
				.thenReturn(false);
		Stat stat = new Stat();
		stat.setCtime(System.currentTimeMillis());
		when(curatorFrameworkOp.getStat(eq(JobNodePath.getJobNodePath(jobName)))).thenReturn(stat);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("不能删除该作业(%s)，因为该作业创建时间距离现在不超过%d分钟", jobName,
				SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT / 60000));
		jobService.removeJob(namespace, jobName);
	}

	@Test
	public void testRemoveJobFailByDeleteDBError() throws Exception {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setId(1L);
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(Boolean.FALSE);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName)))
				.thenReturn(Lists.newArrayList("1"));
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getExecutionNodePath(jobName, "1", "completed"))))
				.thenReturn(true);
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getExecutionNodePath(jobName, "1", "running"))))
				.thenReturn(false);
		Stat stat = new Stat();
		stat.setCtime(System.currentTimeMillis() - (3 * 60 * 1000));
		when(curatorFrameworkOp.getStat(eq(JobNodePath.getJobNodePath(jobName)))).thenReturn(stat);
		when(currentJobConfigService.deleteByPrimaryKey(jobConfig4DB.getId()))
				.thenThrow(new SaturnJobConsoleException("delete error"));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("delete error");
		jobService.removeJob(namespace, jobName);
	}

	@Test
	public void testRemoveJobSuccess() throws Exception {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setId(1L);
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(Boolean.FALSE);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName)))
				.thenReturn(Lists.newArrayList("1"));
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getExecutionNodePath(jobName, "1", "completed"))))
				.thenReturn(true);
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getExecutionNodePath(jobName, "1", "running"))))
				.thenReturn(false);
		Stat stat = new Stat();
		stat.setCtime(System.currentTimeMillis() - (3 * 60 * 1000));
		when(curatorFrameworkOp.getStat(eq(JobNodePath.getJobNodePath(jobName)))).thenReturn(stat);
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getConfigNodePath(jobName, "toDelete")))).thenReturn(true);
		jobService.removeJob(namespace, jobName);
		verify(currentJobConfigService).deleteByPrimaryKey(jobConfig4DB.getId());
		verify(curatorFrameworkOp).deleteRecursive(eq(JobNodePath.getConfigNodePath(jobName, "toDelete")));
		verify(curatorFrameworkOp).create(eq(JobNodePath.getConfigNodePath(jobName, "toDelete")));
	}

	@Test
	public void testGetCandidateExecutorsFailByNotExist() throws SaturnJobConsoleException {
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(null);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("不能获取该作业（%s）可选择的优先Executor，因为该作业不存在", jobName));
		jobService.getCandidateExecutors(namespace, jobName);
	}

	@Test
	public void testGetCandidaExecutorsByExecutorPathNotExist() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.checkExists(eq(SaturnExecutorsNode.getExecutorsNodePath()))).thenReturn(false);
		assertTrue(jobService.getCandidateExecutors(namespace, jobName).isEmpty());
	}

	@Test
	public void testGetCandidateExecutors() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.checkExists(eq(SaturnExecutorsNode.getExecutorsNodePath()))).thenReturn(true);
		String executor = "executor";
		when(curatorFrameworkOp.getChildren(eq(SaturnExecutorsNode.getExecutorsNodePath())))
				.thenReturn(Lists.newArrayList(executor));
		assertEquals(jobService.getCandidateExecutors(namespace, jobName).size(), 1);

		when(curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PREFER_LIST)))
				.thenReturn(true);
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PREFER_LIST)))
				.thenReturn("preferExecutor2,@preferExecutor3");
		assertEquals(jobService.getCandidateExecutors(namespace, jobName).size(), 3);
	}

	@Test
	public void testSetPreferListFailByNotExist() throws SaturnJobConsoleException {
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(null);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("设置该作业（%s）优先Executor失败，因为该作业不存在", jobName));
		jobService.setPreferList(namespace, jobName, "preferList", userName);
	}

	@Test
	public void testSetPreferListFailByLocalModeNotStop() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(true);
		jobConfig4DB.setLocalMode(true);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("启用状态的本地模式作业(%s)，不能设置优先Executor，请先禁用它", jobName));
		jobService.setPreferList(namespace, jobName, "preferList", userName);
	}

	@Test
	public void testSetPreferListSuccess() throws Exception {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(false);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		String preferList = "preferList";
		jobService.setPreferList(namespace, jobName, preferList, userName);
		verify(currentJobConfigService)
				.updateNewAndSaveOld2History(any(JobConfig4DB.class), eq(jobConfig4DB), eq(userName));
		verify(curatorFrameworkOp)
				.update(eq(SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName)), eq(preferList));
		verify(curatorFrameworkOp).delete(eq(SaturnExecutorsNode.getJobConfigForceShardNodePath(jobName)));
		verify(curatorFrameworkOp).create(eq(SaturnExecutorsNode.getJobConfigForceShardNodePath(jobName)));
	}

	@Test
	public void testAddJobFailByWithoutJobName() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("作业名必填");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByJobNameInvalid() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName("!@#aa");
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("作业名只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByDependingJobNameInvalid() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setDependencies("12!@@");
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("依赖的作业只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_、英文逗号,");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByWithoutJobType() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("作业类型必填");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByJobTypeInvalid() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType("unknown");
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("作业类型未知");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByJavaJobWithoutClass() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.JAVA_JOB.name());
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("对于JAVA或消息作业，作业实现类必填");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByShellJobWithoutCorn() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.SHELL_JOB.name());
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("对于JAVA/SHELL作业，cron表达式必填");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByShellJobCornInvalid() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.SHELL_JOB.name());
		jobConfig.setCron("xxxxx");
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("cron表达式语法有误");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByLocalModeJobWithoutShardingItem() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.MSG_JOB.name());
		jobConfig.setJobClass("testCLass");
		jobConfig.setLocalMode(true);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("对于本地模式作业，分片参数必填。");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByNoLocalModeJobWithoutShardingItem() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.MSG_JOB.name());
		jobConfig.setJobClass("testCLass");
		jobConfig.setLocalMode(false);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("分片数不能为空，并且不能小于1");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByNoLocalModeJoShardingItemInvalid() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.MSG_JOB.name());
		jobConfig.setJobClass("testCLass");
		jobConfig.setLocalMode(false);
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("001");
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("分片参数'%s'格式有误", "001"));
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByNoLocalModeJoShardingItemInvalidNumber() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.MSG_JOB.name());
		jobConfig.setJobClass("testCLass");
		jobConfig.setLocalMode(false);
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("x=x");
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("分片参数'%s'格式有误", "x=x"));
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByAddSystemJob() throws SaturnJobConsoleException {
		JobConfig jobConfig = createValidJob();
		jobConfig.setJobMode(JobMode.SYSTEM_PREFIX);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("作业模式有误，不能添加系统作业");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByJobIsExist() throws SaturnJobConsoleException {
		JobConfig jobConfig = createValidJob();
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(eq(namespace), eq(jobName)))
				.thenReturn(jobConfig4DB);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("该作业(%s)已经存在", jobName));
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByLimitNum() throws SaturnJobConsoleException {
		JobConfig jobConfig = createValidJob();
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		when(systemConfigService.getIntegerValue(eq(SystemConfigProperties.MAX_JOB_NUM), eq(100))).thenReturn(1);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("总作业数超过最大限制(%d)，作业名%s创建失败", 1, jobName));
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobSuccess() throws Exception {
		JobConfig jobConfig = createValidJob();
		when(registryCenterService.getCuratorFrameworkOp(eq(namespace))).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getJobNodePath(jobConfig.getJobName())))).thenReturn(true);
		jobService.addJob(namespace, jobConfig, userName);
		verify(curatorFrameworkOp).deleteRecursive(eq(JobNodePath.getJobNodePath(jobConfig.getJobName())));
		verify(currentJobConfigService).create(any(JobConfig4DB.class));
	}

	@Test
	public void testCopyJobSuccess() throws Exception {
		JobConfig jobConfig = createValidJob();
		when(registryCenterService.getCuratorFrameworkOp(eq(namespace))).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getJobNodePath(jobConfig.getJobName())))).thenReturn(true);
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		when(currentJobConfigService.findConfigByNamespaceAndJobName(eq(namespace), eq("copyJob")))
				.thenReturn(jobConfig4DB);
		jobService.copyJob(namespace, jobConfig, "copyJob", userName);
		verify(curatorFrameworkOp).deleteRecursive(eq(JobNodePath.getJobNodePath(jobConfig.getJobName())));
		verify(currentJobConfigService).create(any(JobConfig4DB.class));
	}

	private JobConfig createValidJob() {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.MSG_JOB.name());
		jobConfig.setJobClass("testCLass");
		jobConfig.setLocalMode(false);
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=1");
		return jobConfig;
	}

	@Test
	public void testAddJobFailByNoLocalModeJoShardingItemLess() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.MSG_JOB.name());
		jobConfig.setJobClass("testCLass");
		jobConfig.setLocalMode(false);
		jobConfig.setShardingTotalCount(2);
		jobConfig.setShardingItemParameters("0=1");
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("分片参数不能小于分片总数");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByLocalModeJobShardingItemInvalid() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.MSG_JOB.name());
		jobConfig.setJobClass("testCLass");
		jobConfig.setLocalMode(true);
		jobConfig.setShardingItemParameters("test");
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("对于本地模式作业，分片参数必须包含如*=xx。");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testGetUnSystemJobsWithCondition() throws SaturnJobConsoleException {
		String namespace = "ns1";
		String jobName = "testJob";
		int count = 4;
		Map<String, Object> condition = buildCondition(null);
		when(currentJobConfigService
				.findConfigsByNamespaceWithCondition(eq(namespace), eq(condition), Matchers.<Pageable>anyObject()))
				.thenReturn(buildJobConfig4DBList(namespace, jobName, count));
		assertTrue(jobService.getUnSystemJobsWithCondition(namespace, condition, 1, 25).size() == count);
	}

	@Test
	public void testGetUnSystemJobWithConditionAndStatus() throws SaturnJobConsoleException {
		String namespace = "ns1";
		String jobName = "testJob";
		int count = 4;
		List<JobConfig4DB> jobConfig4DBList = buildJobConfig4DBList(namespace, jobName, count);
		Map<String, Object> condition = buildCondition(JobStatus.READY);
		when(currentJobConfigService
				.findConfigsByNamespaceWithCondition(eq(namespace), eq(condition), Matchers.<Pageable>anyObject()))
				.thenReturn(jobConfig4DBList);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		for (int i = 0; i < count; i++) {
			JobConfig4DB jobConfig4DB = jobConfig4DBList.get(i);
			// 设置 index 为单数的job enabled 为 true
			jobConfig4DB.setEnabled(i % 2 == 1);
			when(currentJobConfigService.findConfigByNamespaceAndJobName(eq(namespace), eq(jobConfig4DB.getJobName())))
					.thenReturn(jobConfig4DB);
			when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobConfig4DB.getJobName())))
					.thenReturn(null);
		}
		assertTrue(jobService.getUnSystemJobsWithCondition(namespace, condition, 1, 25).size() == (count / 2));
	}


	@Test
	public void testIsJobShardingAllocatedExecutor() throws SaturnJobConsoleException {
		String namespace = "ns1";
		String jobName = "testJob";
		String executor = "executor1";
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getChildren(JobNodePath.getServerNodePath(jobName)))
				.thenReturn(Lists.newArrayList(executor));
		when(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executor, "sharding")))
				.thenReturn("true");
		assertTrue(jobService.isJobShardingAllocatedExecutor(namespace, jobName));
	}

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
		when(curatorFrameworkOp.getData(JobNodePath.getServerSharding(jobName, executorName))).thenReturn("0,1,2,3,4");

		when(curatorFrameworkOp.checkExists(JobNodePath.getEnabledReportNodePath(jobName))).thenReturn(true);
		when(curatorFrameworkOp.getData(JobNodePath.getEnabledReportNodePath(jobName))).thenReturn("true");

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
		when(curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, nodeName))).thenReturn(data);
	}

	private void mockJobMessage(String jobName, String item, String msg) {
		when(curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "jobMsg"))).thenReturn(msg);
	}

	private void mockTimezone(String jobName, String timezone) {
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeZone"))).thenReturn(timezone);
	}

	private void mockExecutionStatusNode(boolean isRunning, boolean isCompleted, boolean isFailover, boolean isFailed,
			boolean isTimeout, String executorName, String jobName, String jobItem) {
		if (isRunning) {
			when(curatorFrameworkOp.getData(JobNodePath.getRunningNodePath(jobName, jobItem))).thenReturn(executorName);
		}

		if (isCompleted) {
			when(curatorFrameworkOp.getData(JobNodePath.getCompletedNodePath(jobName, jobItem)))
					.thenReturn(executorName);
		}

		if (isFailover) {
			when(curatorFrameworkOp.getData(JobNodePath.getFailoverNodePath(jobName, jobItem)))
					.thenReturn(executorName);
			when(curatorFrameworkOp.getMtime(JobNodePath.getFailoverNodePath(jobName, jobItem))).thenReturn(1L);
		}

		if (isFailed) {
			when(curatorFrameworkOp.checkExists(JobNodePath.getFailedNodePath(jobName, jobItem))).thenReturn(true);
		}

		if (isTimeout) {
			when(curatorFrameworkOp.checkExists(JobNodePath.getTimeoutNodePath(jobName, jobItem))).thenReturn(true);
		}
	}

	private JobConfig4DB buildJobConfig4DB(String namespace, String jobName) {
		JobConfig4DB config = new JobConfig4DB();
		config.setNamespace(namespace);
		config.setJobName(jobName);
		config.setEnabled(true);
		config.setEnabledReport(true);
		config.setJobType(JobType.JAVA_JOB.toString());
		return config;
	}

	private List<JobConfig4DB> buildJobConfig4DBList(String namespace, String jobName, int count) {
		List<JobConfig4DB> config4DBList = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			JobConfig4DB config = new JobConfig4DB();
			config.setNamespace(namespace);
			config.setJobName(jobName + i);
			config.setEnabled(true);
			config.setEnabledReport(true);
			config.setJobType(JobType.JAVA_JOB.toString());
			config4DBList.add(config);
		}
		return config4DBList;
	}

	private Map<String, Object> buildCondition(JobStatus jobStatus) {
		Map<String, Object> condition = new HashMap<>();
		condition.put("jobStatus", jobStatus);
		return condition;
	}
}