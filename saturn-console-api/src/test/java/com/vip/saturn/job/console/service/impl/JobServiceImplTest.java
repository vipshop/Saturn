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
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.SaturnConstants;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Maps;
import org.hamcrest.core.StringContains;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static com.vip.saturn.job.console.service.impl.JobServiceImpl.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JobServiceImplTest {

	@Mock
	private CuratorFrameworkOp curatorFrameworkOp;

	@Mock
	private CuratorFrameworkOp.CuratorTransactionOp curatorTransactionOp;

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
	public void testGetGroup() throws SaturnJobConsoleException {
		when(currentJobConfigService.findConfigsByNamespace(namespace))
				.thenReturn(Lists.newArrayList(new JobConfig4DB()));
		assertEquals(jobService.getGroups(namespace).size(), 1);
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
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.<JobConfig4DB>emptyList());
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("不能删除该作业（%s），因为该作业不存在", jobName));
		jobService.removeJob(namespace, jobName);
	}

	@Test
	public void testRemoveJobFailByHaveUpStream() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setUpStream("upStreamJob");
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException
				.expectMessage(String.format("不能删除该作业（%s），因为该作业存在上游作业（%s），请先断开上下游关系再删除", jobName, "upStreamJob"));
		jobService.removeJob(namespace, jobName);
	}

	@Test
	public void testRemoveJobFailByHaveDownStream() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setDownStream("downStreamJob");
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException
				.expectMessage(String.format("不能删除该作业（%s），因为该作业存在下游作业（%s），请先断开上下游关系再删除", jobName, "downStreamJob"));
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
		expectedException.expectMessage("对于java作业，作业实现类必填");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByPassiveJavaJobWithoutClass() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.PASSIVE_JAVA_JOB.name());
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("对于java作业，作业实现类必填");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByVMSJobWithoutClass() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.JAVA_JOB.name());
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("对于java作业，作业实现类必填");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByShellJobWithoutCron() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.SHELL_JOB.name());
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("对于cron作业，cron表达式必填");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByShellJobCronInvalid() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.SHELL_JOB.name());
		jobConfig.setCron("xxxxx");
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("cron表达式语法有误");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByMsgJobWithoutQueue() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.MSG_JOB.name());
		jobConfig.setJobClass("testCLass");
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("对于消息作业，queue必填");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByLocalModeJobWithoutShardingItem() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.JAVA_JOB.name());
		jobConfig.setCron("0 */2 * * * ?");
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
		jobConfig.setJobType(JobType.JAVA_JOB.name());
		jobConfig.setCron("0 */2 * * * ?");
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
		jobConfig.setJobType(JobType.JAVA_JOB.name());
		jobConfig.setCron("0 */2 * * * ?");
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
		jobConfig.setJobType(JobType.JAVA_JOB.name());
		jobConfig.setCron("0 */2 * * * ?");
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
		jobConfig4DB.setJobName("abc");
		when(systemConfigService.getIntegerValue(eq(SystemConfigProperties.MAX_JOB_NUM), eq(100))).thenReturn(1);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("总作业数超过最大限制(%d)，作业名%s创建失败", 1, jobName));
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByDeleting() throws SaturnJobConsoleException {
		JobConfig jobConfig = createValidJob();
		String server = "e1";
		when(registryCenterService.getCuratorFrameworkOp(eq(namespace))).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getJobNodePath(jobConfig.getJobName())))).thenReturn(true);
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getServerNodePath(jobConfig.getJobName()))))
				.thenReturn(true);
		when(curatorFrameworkOp.getChildren(eq(JobNodePath.getServerNodePath(jobConfig.getJobName()))))
				.thenReturn(Lists.newArrayList(server));
		when(curatorFrameworkOp.checkExists(eq(ExecutorNodePath.getExecutorNodePath(server, "ip")))).thenReturn(true);
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getServerStatus(jobConfig.getJobName(), server))))
				.thenReturn(true);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("该作业(%s)正在删除中，请稍后再试", jobName));
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobSuccess() throws Exception {
		JobConfig jobConfig = createValidJob();
		when(registryCenterService.getCuratorFrameworkOp(eq(namespace))).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.inTransaction()).thenReturn(curatorTransactionOp);
		when(curatorTransactionOp.create(anyString(), anyString())).thenReturn(curatorTransactionOp);
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getJobNodePath(jobConfig.getJobName())))).thenReturn(true);
		jobService.addJob(namespace, jobConfig, userName);
		verify(curatorFrameworkOp).deleteRecursive(eq(JobNodePath.getJobNodePath(jobConfig.getJobName())));
		verify(currentJobConfigService).create(any(JobConfig4DB.class));
	}

	@Test
	public void testCopyJobSuccess() throws Exception {
		JobConfig jobConfig = createValidJob();
		when(registryCenterService.getCuratorFrameworkOp(eq(namespace))).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.inTransaction()).thenReturn(curatorTransactionOp);
		when(curatorFrameworkOp.checkExists(eq(JobNodePath.getJobNodePath(jobConfig.getJobName())))).thenReturn(true);
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		when(currentJobConfigService.findConfigByNamespaceAndJobName(eq(namespace), eq("copyJob")))
				.thenReturn(jobConfig4DB);
		when(curatorFrameworkOp.inTransaction()).thenReturn(curatorTransactionOp);
		when(curatorTransactionOp.replaceIfChanged(anyString(), anyString())).thenReturn(curatorTransactionOp);
		when(curatorTransactionOp.create(anyString(), anyString())).thenReturn(curatorTransactionOp);
		jobService.copyJob(namespace, jobConfig, "copyJob", userName);
		verify(curatorFrameworkOp).deleteRecursive(eq(JobNodePath.getJobNodePath(jobConfig.getJobName())));
		verify(currentJobConfigService).create(any(JobConfig4DB.class));
	}

	private JobConfig createValidJob() {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.MSG_JOB.name());
		jobConfig.setQueueName("queue");
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
		jobConfig.setJobType(JobType.JAVA_JOB.name());
		jobConfig.setCron("0 */2 * * * ?");
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
		jobConfig.setJobType(JobType.JAVA_JOB.name());
		jobConfig.setCron("0 */2 * * * ?");
		jobConfig.setJobClass("testCLass");
		jobConfig.setLocalMode(true);
		jobConfig.setShardingItemParameters("test");
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("对于本地模式作业，分片参数必须包含如*=xx。");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByLocalModeJobHasDownStream() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.JAVA_JOB.name());
		jobConfig.setCron("0 */2 * * * ?");
		jobConfig.setJobClass("testCLass");
		jobConfig.setLocalMode(true);
		jobConfig.setShardingItemParameters("*=xx");
		jobConfig.setDownStream("test");
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("非本地模式作业，才能配置下游作业");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByHasDownStreamButShardingTotalCountIsNotOne() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.JAVA_JOB.name());
		jobConfig.setCron("0 */2 * * * ?");
		jobConfig.setJobClass("testCLass");
		jobConfig.setShardingTotalCount(2);
		jobConfig.setShardingItemParameters("0=0,1=1");
		jobConfig.setDownStream("test");
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("分片数为1，才能配置下游作业");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByHasDownStreamButIsSelf() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.JAVA_JOB.name());
		jobConfig.setCron("0 */2 * * * ?");
		jobConfig.setJobClass("testCLass");
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		jobConfig.setDownStream(jobName);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("下游作业(" + jobName + ")不能是该作业本身");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByHasDownStreamButNotExisting() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.JAVA_JOB.name());
		jobConfig.setCron("0 */2 * * * ?");
		jobConfig.setJobClass("testCLass");
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		jobConfig.setDownStream("test");
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("下游作业(test)不存在");
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByHasDownStreamButIsAncestor() throws Exception {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.PASSIVE_SHELL_JOB.name());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		jobConfig.setUpStream("test1");
		jobConfig.setDownStream("test1");

		JobConfig4DB test1 = new JobConfig4DB();
		test1.setJobName("test1");
		test1.setJobType(JobType.PASSIVE_SHELL_JOB.name());
		test1.setShardingTotalCount(1);
		test1.setShardingItemParameters("0=0");

		when(currentJobConfigService.findConfigsByNamespace(eq(namespace))).thenReturn(Arrays.asList(test1));
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.inTransaction()).thenReturn(curatorTransactionOp);
		when(curatorTransactionOp.replaceIfChanged(anyString(), anyString())).thenReturn(curatorTransactionOp);
		when(curatorTransactionOp.create(anyString(), anyString())).thenReturn(curatorTransactionOp);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("该域(%s)作业编排有误，存在环: %s", namespace, "[testJob, test1, testJob]"));
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByHasDownStreamButIsAncestor2() throws Exception {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.PASSIVE_SHELL_JOB.name());
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		jobConfig.setUpStream("test2");
		jobConfig.setDownStream("test1");

		JobConfig4DB test1 = new JobConfig4DB();
		test1.setJobName("test1");
		test1.setJobType(JobType.PASSIVE_SHELL_JOB.name());
		test1.setShardingTotalCount(1);
		test1.setShardingItemParameters("0=0");
		test1.setDownStream("test2");

		JobConfig4DB test2 = new JobConfig4DB();
		test2.setJobName("test2");
		test2.setJobType(JobType.PASSIVE_SHELL_JOB.name());
		test2.setShardingTotalCount(1);
		test2.setShardingItemParameters("0=0");
		test2.setUpStream("test1");

		when(currentJobConfigService.findConfigsByNamespace(eq(namespace))).thenReturn(Arrays.asList(test1, test2));
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.inTransaction()).thenReturn(curatorTransactionOp);
		when(curatorTransactionOp.replaceIfChanged(anyString(), anyString())).thenReturn(curatorTransactionOp);
		when(curatorTransactionOp.create(anyString(), anyString())).thenReturn(curatorTransactionOp);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException
				.expectMessage(String.format("该域(%s)作业编排有误，存在环: %s", namespace, "[testJob, test2, test1, testJob]"));
		jobService.addJob(namespace, jobConfig, userName);
	}

	@Test
	public void testAddJobFailByHasDownStreamButIsNotPassive() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		jobConfig.setJobType(JobType.JAVA_JOB.name());
		jobConfig.setCron("0 */2 * * * ?");
		jobConfig.setJobClass("testCLass");
		jobConfig.setShardingTotalCount(1);
		jobConfig.setShardingItemParameters("0=0");
		jobConfig.setDownStream("test1");
		JobConfig4DB test1 = new JobConfig4DB();
		test1.setJobName("test1");
		when(currentJobConfigService.findConfigsByNamespace(eq(namespace))).thenReturn(Arrays.asList(test1));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("下游作业(test1)不是被动作业");
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
	public void testGetMaxJobNum() {
		assertEquals(jobService.getMaxJobNum(), 100);
	}

	@Test
	public void testGetUnSystemJob() throws SaturnJobConsoleException {
		when(currentJobConfigService.findConfigsByNamespace(namespace))
				.thenReturn(Lists.newArrayList(new JobConfig4DB()));
		assertEquals(jobService.getUnSystemJobs(namespace).size(), 1);
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
	public void testGetUnSystemJobNames() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		assertEquals(jobService.getUnSystemJobs(namespace).size(), 1);
	}

	@Test
	public void testGetJobName() throws SaturnJobConsoleException {
		when(currentJobConfigService.findConfigNamesByNamespace(namespace)).thenReturn(null);
		assertTrue(jobService.getJobNames(namespace).isEmpty());
		when(currentJobConfigService.findConfigNamesByNamespace(namespace)).thenReturn(Lists.newArrayList(jobName));
		assertEquals(jobService.getJobNames(namespace).size(), 1);
	}

	@Test
	public void testPersistJobFromDb() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		jobService.persistJobFromDB(namespace, jobConfig);
		jobService.persistJobFromDB(jobConfig, curatorFrameworkOp);
	}

	@Test
	public void testImportFailByWithoutJobName() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("作业名必填。"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByJobNameInvalid() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName("!@avb");
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("作业名只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_。"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByWithoutJobType() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("作业类型必填。"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByUnknownJobType() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn("xxx");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("作业类型未知。"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByJavaJobWithoutClass() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.JAVA_JOB.name());
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("对于java作业，作业实现类必填。"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByPassiveJavaJobWithoutClass() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.PASSIVE_JAVA_JOB.name());
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("对于java作业，作业实现类必填。"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByShellJobWithoutCron() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("对于cron作业，cron表达式必填。"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByShellJobCronInvalid() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON))).thenReturn("xxxx");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("cron表达式语法有误，"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByWithoutShardingCount() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("分片数必填"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByShardingCountInvalid() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("xxx");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("分片数有误"));
		jobService.importJobs(namespace, data, userName);
	}


	@Test
	public void testImportFailByShardingCountLess4One() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("0");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("分片数不能小于1"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByTimeoutSecondsInvalid() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIMEOUT_SECONDS)))
				.thenReturn("error");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("超时（Kill线程/进程）时间有误，"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByLocalJobWithoutShardingParam() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOCAL_MODE)))
				.thenReturn("true");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("对于本地模式作业，分片参数必填。"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByLocalJobShardingParamInvalid() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOCAL_MODE)))
				.thenReturn("true");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)))
				.thenReturn("0=1");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("对于本地模式作业，分片参数必须包含如*=xx。"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByShardingParamLess4Count() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("2");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)))
				.thenReturn("0=1");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("分片参数不能小于分片总数。"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByProcessCountIntervalSecondsInvalid() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)))
				.thenReturn("0=1");
		when(curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PROCESS_COUNT_INTERVAL_SECONDS)))
				.thenReturn("error");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("统计处理数据量的间隔秒数有误，"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByLoadLevelInvalid() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)))
				.thenReturn("0=1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOAD_LEVEL)))
				.thenReturn("error");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("负荷有误"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByJobDegreeInvalid() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)))
				.thenReturn("0=1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_DEGREE)))
				.thenReturn("error");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("作业重要等级有误，"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByJobModeInvalid() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)))
				.thenReturn("0=1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_MODE)))
				.thenReturn(JobMode.SYSTEM_PREFIX);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("作业模式有误，不能添加系统作业"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByDependenciesInvalid() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)))
				.thenReturn("0=1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_DEPENDENCIES)))
				.thenReturn("!@error");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("依赖的作业只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_、英文逗号,"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByTimeout4AlarmSecondsInvalid() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)))
				.thenReturn("0=1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIMEOUT_4_ALARM_SECONDS)))
				.thenReturn("error");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("超时（告警）时间有误，"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByTimeZoneInvalid() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)))
				.thenReturn("0=1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIME_ZONE)))
				.thenReturn("error");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("时区有误"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByLocalJobNotSupportFailover() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)))
				.thenReturn("*=1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOCAL_MODE)))
				.thenReturn("true");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_FAILOVER)))
				.thenReturn("true");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("本地模式不支持failover"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByVMSJobNotSupportFailover() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.MSG_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_QUEUE_NAME)))
				.thenReturn("queue");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)))
				.thenReturn("*=1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_LOCAL_MODE)))
				.thenReturn("false");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_FAILOVER)))
				.thenReturn("true");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("消息作业不支持failover"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailByVMSJobNotSupportRerun() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.MSG_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_QUEUE_NAME)))
				.thenReturn("queue");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)))
				.thenReturn("0=1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_RERUN))).thenReturn("true");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(StringContains.containsString("消息作业不支持rerun"));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportFailTotalCountLimit() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)))
				.thenReturn("0=1");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		when(systemConfigService.getIntegerValue(SystemConfigProperties.MAX_JOB_NUM, 100)).thenReturn(1);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("总作业数超过最大限制(%d)，导入失败", 1));
		jobService.importJobs(namespace, data, userName);
	}

	@Test
	public void testImportSuccess() throws SaturnJobConsoleException, IOException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_CRON)))
				.thenReturn("0 */2 * * * ?");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_CLASS)))
				.thenReturn("vip");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_ITEM_PARAMETERS)))
				.thenReturn("0=1");
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		when(systemConfigService.getIntegerValue(SystemConfigProperties.MAX_JOB_NUM, 100)).thenReturn(100);
		jobService.importJobs(namespace, data, userName);
	}


	@Test
	public void testExport() throws SaturnJobConsoleException, IOException, BiffException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigsByNamespace(namespace)).thenReturn(Lists.newArrayList(jobConfig4DB));
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		File file = jobService.exportJobs(namespace);
		MultipartFile data = new MockMultipartFile("test.xls", new FileInputStream(file));
		Workbook workbook = Workbook.getWorkbook(data.getInputStream());
		assertNotNull(workbook);
		Sheet[] sheets = workbook.getSheets();
		assertEquals(sheets.length, 1);
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

	@Test
	public void testGetJobConfigFromZK() throws SaturnJobConsoleException {
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_JOB_TYPE)))
				.thenReturn(JobType.SHELL_JOB.name());
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_SHARDING_TOTAL_COUNT)))
				.thenReturn("1");
		when(curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_PROCESS_COUNT_INTERVAL_SECONDS)))
				.thenReturn("100");
		when(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, CONFIG_ITEM_TIMEOUT_SECONDS)))
				.thenReturn("100");
		assertNotNull(jobService.getJobConfigFromZK(namespace, jobName));
	}

	@Test
	public void testGetJobConfigFailByJobNotExist() throws SaturnJobConsoleException {
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(null);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("该作业(%s)不存在", jobName));
		jobService.getJobConfig(namespace, jobName);
	}

	@Test
	public void testGetJobConfigSuccess() throws SaturnJobConsoleException {
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName))
				.thenReturn(new JobConfig4DB());
		assertNotNull(jobService.getJobConfig(namespace, jobName));
	}

	@Test
	public void testGetJobStatusFailByJobNotExist() throws SaturnJobConsoleException {
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(null);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("不能获取该作业（%s）的状态，因为该作业不存在", jobName));
		jobService.getJobStatus(namespace, jobName);
	}

	@Test
	public void testGetJobStatusSuccess() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(true);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		assertEquals(jobService.getJobStatus(namespace, jobName), JobStatus.READY);

		JobConfig jobConfig = new JobConfig();
		jobConfig.setEnabled(true);
		assertEquals(jobService.getJobStatus(namespace, jobConfig), JobStatus.READY);
	}

	@Test
	public void testGetServerList() throws SaturnJobConsoleException {
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getChildren(JobNodePath.getServerNodePath(jobName))).thenReturn(null);
		assertTrue(jobService.getJobServerList(namespace, jobName).isEmpty());
		when(curatorFrameworkOp.getChildren(JobNodePath.getServerNodePath(jobName)))
				.thenReturn(Lists.newArrayList("executor"));
		assertEquals(jobService.getJobServerList(namespace, jobName).size(), 1);
	}

	@Test
	public void testGetJobConfigVoFailByJobNotExist() throws SaturnJobConsoleException {
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(null);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("该作业(%s)不存在", jobName));
		jobService.getJobConfigVo(namespace, jobName);
	}

	@Test
	public void testGetJobConfigVoSuccess() throws SaturnJobConsoleException {
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName))
				.thenReturn(new JobConfig4DB());
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		assertNotNull(jobService.getJobConfigVo(namespace, jobName));
	}

	@Test
	public void testUpdateJobConfigFailByJobNotExist() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobConfig.getJobName()))
				.thenReturn(null);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("该作业(%s)不存在", jobName));
		jobService.getJobConfigVo(namespace, jobName);
	}

	@Test
	public void testUpdateJobConfigSuccess() throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(jobName);
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobConfig.getJobName()))
				.thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		jobService.getJobConfigVo(namespace, jobName);
	}

	@Test
	public void testGetAllJobNamesFromZK() throws SaturnJobConsoleException {
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getChildren(JobNodePath.get$JobsNodePath())).thenReturn(null);
		assertTrue(jobService.getAllJobNamesFromZK(namespace).isEmpty());

		when(curatorFrameworkOp.getChildren(JobNodePath.get$JobsNodePath())).thenReturn(Lists.newArrayList(jobName));
		when(curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(jobName))).thenReturn(true);
		assertEquals(jobService.getAllJobNamesFromZK(namespace).size(), 1);
	}

	@Test
	public void testUpdateJobCronFailByCronInvalid() throws SaturnJobConsoleException {
		String cron = "error";
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("The cron expression is invalid: %s", cron));
		jobService.updateJobCron(namespace, jobName, cron, null, userName);
	}

	@Test
	public void testUpdateJobCronFailByJobNotExist() throws SaturnJobConsoleException {
		String cron = "0 */2 * * * ?";
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(jobName))).thenReturn(false);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("The job does not exists: %s", jobName));
		jobService.updateJobCron(namespace, jobName, cron, null, userName);
	}

	@Test
	public void testUpdateJobCronSuccess() throws SaturnJobConsoleException {
		String cron = "0 */2 * * * ?";
		Map<String, String> customContext = Maps.newHashMap();
		customContext.put("test", "test");
		CuratorFramework curatorFramework = mock(CuratorFramework.class);
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		when(curatorFrameworkOp.getCuratorFramework()).thenReturn(curatorFramework);
		when(curatorFramework.getNamespace()).thenReturn(namespace);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(jobName))).thenReturn(true);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		jobService.updateJobCron(namespace, jobName, cron, customContext, userName);
	}

	@Test
	public void testGetJobServers() throws SaturnJobConsoleException {
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getChildren(JobNodePath.getServerNodePath(jobName)))
				.thenReturn(Lists.newArrayList("executor"));
		when(curatorFrameworkOp.getData(JobNodePath.getLeaderNodePath(jobName, "election/host")))
				.thenReturn("127.0.0.1");
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(true);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		assertEquals(jobService.getJobServers(namespace, jobName).size(), 1);
	}

	@Test
	public void testGetJobServerStatus() throws SaturnJobConsoleException {
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getChildren(JobNodePath.getServerNodePath(jobName)))
				.thenReturn(Lists.newArrayList("executor"));
		assertEquals(jobService.getJobServersStatus(namespace, jobName).size(), 1);
	}

	@Test
	public void testRunAtOneFailByNotReady() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(false);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("该作业(%s)不处于READY状态，不能立即执行", jobName));
		jobService.runAtOnce(namespace, jobName);
	}

	@Test
	public void testRunAtOnceFailByNoExecutor() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(true);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getChildren(JobNodePath.getServerNodePath(jobName))).thenReturn(null);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("没有executor接管该作业(%s)，不能立即执行", jobName));
		jobService.runAtOnce(namespace, jobName);
	}

	@Test
	public void testRunAtOnceFailByNoOnlineExecutor() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(true);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getChildren(JobNodePath.getServerNodePath(jobName)))
				.thenReturn(Lists.newArrayList("executor"));
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage("没有ONLINE的executor，不能立即执行");
		jobService.runAtOnce(namespace, jobName);
	}

	@Test
	public void testRunAtOnceSuccess() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(true);
		String executor = "executor";
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.getChildren(JobNodePath.getServerNodePath(jobName)))
				.thenReturn(Lists.newArrayList(executor));
		when(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executor, "status"))).thenReturn("true");
		jobService.runAtOnce(namespace, jobName);
		verify(curatorFrameworkOp).create(JobNodePath.getRunOneTimePath(jobName, executor), "null");
	}

	@Test
	public void testStopAtOneFailByNotStopping() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(true);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("该作业(%s)不处于STOPPING状态，不能立即终止", jobName));
		jobService.stopAtOnce(namespace, jobName);
	}

	@Test
	public void testStopAtOnceFailByNoExecutor() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(false);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName)))
				.thenReturn(Lists.newArrayList("1"));
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(jobName, "1", "running")))
				.thenReturn(true);
		expectedException.expect(SaturnJobConsoleException.class);
		expectedException.expectMessage(String.format("没有executor接管该作业(%s)，不能立即终止", jobName));
		jobService.stopAtOnce(namespace, jobName);
	}

	@Test
	public void testStopAtOnceSuccess() throws SaturnJobConsoleException {
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(false);
		String executor = "executor";
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName)))
				.thenReturn(Lists.newArrayList("1"));
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		when(curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(jobName, "1", "running")))
				.thenReturn(true);
		when(curatorFrameworkOp.getChildren(JobNodePath.getServerNodePath(jobName)))
				.thenReturn(Lists.newArrayList(executor));
		when(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executor, "status"))).thenReturn("true");
		jobService.stopAtOnce(namespace, jobName);
		verify(curatorFrameworkOp).create(JobNodePath.getStopOneTimePath(jobName, executor));
	}

	@Test
	public void testGetExecutionStatusByJobHasStopped() throws SaturnJobConsoleException {
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		// test get execution status by job has stopped
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setJobName(jobName);
		jobConfig4DB.setEnabled(false);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		assertTrue(jobService.getExecutionStatus(namespace, jobName).isEmpty());
	}

	@Test
	public void testGetExecutionStatusByWithoutItem() throws SaturnJobConsoleException {
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setEnabled(true);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName))).thenReturn(null);
		assertTrue(jobService.getExecutionStatus(namespace, jobName).isEmpty());
	}

	@Test
	public void testGetExecutionStatus() throws SaturnJobConsoleException {
		when(registryCenterService.getCuratorFrameworkOp(namespace)).thenReturn(curatorFrameworkOp);
		JobConfig4DB jobConfig4DB = new JobConfig4DB();
		jobConfig4DB.setEnabled(true);
		when(currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName)).thenReturn(jobConfig4DB);
		when(curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName)))
				.thenReturn(Lists.newArrayList("1"));
		when(curatorFrameworkOp.getChildren(JobNodePath.getServerNodePath(jobName)))
				.thenReturn(Lists.newArrayList("server"));
		when(curatorFrameworkOp.getData(JobNodePath.getServerSharding(jobName, "server"))).thenReturn("0");
	}

}