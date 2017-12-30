package com.vip.saturn.job.console.service.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.JobStatus;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ServerAllocationInfo;
import com.vip.saturn.job.console.domain.ServerBriefInfo;
import com.vip.saturn.job.console.domain.ServerStatus;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.mybatis.entity.CurrentJobConfig;
import com.vip.saturn.job.console.mybatis.service.CurrentJobConfigService;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.service.JobOperationService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.SaturnConstants;
import com.vip.saturn.job.console.utils.ThreadLocalCuratorClient;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import javax.annotation.Resource;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableCellFeatures;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @author xiaopeng.he
 */
@Service
public class ExecutorServiceImpl implements ExecutorService {

	public static final String IP_NODE_NAME = "ip";

	private static final Logger log = LoggerFactory.getLogger(ExecutorServiceImpl.class);

	private static final int DEFAULT_MAX_JOB_NUM = 100;

	private DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM,
			Locale.SIMPLIFIED_CHINESE);

	@Resource
	private CuratorRepository curatorRepository;

	@Resource
	private JobDimensionService jobDimensionService;

	@Resource
	private JobOperationService jobOperationService;

	@Resource
	private CurrentJobConfigService currentJobConfigService;

	@Resource
	private SystemConfigService systemConfigService;

	@Resource
	private RegistryCenterService registryCenterService;

	private Random random = new Random();

	@Override
	public List<ServerBriefInfo> getExecutors(String namespace) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);

		List<String> executors = curatorFrameworkOp.getChildren(ExecutorNodePath.getExecutorNodePath());
		if (executors == null || executors.size() == 0) {
			return Lists.newArrayList();
		}

		List<ServerBriefInfo> executorInfoList = Lists.newArrayList();
		for (String executor : executors) {
			ServerBriefInfo executorInfo = getServerBriefInfo(executor, curatorFrameworkOp);
			executorInfoList.add(executorInfo);
		}

		return executorInfoList;
	}

	@Override
	public ServerBriefInfo getExecutor(String namespace, String executorName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);

		if (!curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath(executorName))) {
			return null;
		}

		return getServerBriefInfo(executorName, curatorFrameworkOp);
	}

	private ServerBriefInfo getServerBriefInfo(String executorName, CuratorFrameworkOp curatorFrameworkOp) {
		ServerBriefInfo executorInfo = new ServerBriefInfo(executorName);
		String ip = curatorFrameworkOp.getData(ExecutorNodePath.getExecutorNodePath(executorName, "ip"));
		executorInfo.setServerIp(ip);
		if (!Strings.isNullOrEmpty(ip)) {
			executorInfo.setStatus(ServerStatus.ONLINE);
		} else {
			executorInfo.setStatus(ServerStatus.OFFLINE);
		}
		executorInfo.setNoTraffic(curatorFrameworkOp
				.checkExists(ExecutorNodePath.getExecutorNodePath(executorName, "noTraffic")));
		String lastBeginTime = curatorFrameworkOp
				.getData(ExecutorNodePath.getExecutorNodePath(executorInfo.getExecutorName(), "lastBeginTime"));
		executorInfo.setLastBeginTime(
				null == lastBeginTime ? null : dateFormat.format(new Date(Long.parseLong(lastBeginTime))));
		executorInfo.setVersion(
				curatorFrameworkOp.getData(ExecutorNodePath.getExecutorNodePath(executorName, "version")));
		String task = curatorFrameworkOp.getData(ExecutorNodePath.getExecutorNodePath(executorName, "task"));
		if (StringUtils.isNotBlank(task)) {
			executorInfo.setGroupName(task);
			executorInfo.setContainer(true);
		}
		return executorInfo;
	}

	@Override
	public ServerAllocationInfo getExecutorAllocation(String namespace, String executorName)
			throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);

		List<String> jobs = null;
		try {
			jobs = jobDimensionService.getAllUnSystemJobs(curatorFrameworkOp);
		} catch (SaturnJobConsoleException e) {
			log.error(e.getMessage(), e);
		}

		ServerAllocationInfo serverAllocationInfo = new ServerAllocationInfo(executorName);

		if (jobs == null || jobs.size() == 0) {
			return serverAllocationInfo;
		}

		for (String jobName : jobs) {
			String serverNodePath = JobNodePath.getServerNodePath(jobName);
			if (!curatorFrameworkOp.checkExists(serverNodePath)) {
				continue;
			}

			String sharding = curatorFrameworkOp
					.getData(JobNodePath.getServerNodePath(jobName, executorName, "sharding"));
			if (StringUtils.isNotBlank(sharding)) {
				// 作业状态为STOPPED的即使有残留分片也不显示该分片
				if (JobStatus.STOPPED.equals(jobDimensionService.getJobStatus(jobName, curatorFrameworkOp))) {
					continue;
				}
				// concat executorSharding
				serverAllocationInfo.getAllocationMap().put(jobName, sharding);
				// calculate totalLoad
				String loadLevelNode = curatorFrameworkOp
						.getData(JobNodePath.getConfigNodePath(jobName, "loadLevel"));
				Integer loadLevel = 1;
				if (StringUtils.isNotBlank(loadLevelNode)) {
					loadLevel = Integer.parseInt(loadLevelNode);
				}

				int shardingItemNum = sharding.split(",").length;
				int curJobLoad = shardingItemNum * loadLevel;
				int totalLoad = serverAllocationInfo.getTotalLoadLevel();
				serverAllocationInfo.setTotalLoadLevel(totalLoad + curJobLoad);
			}
		}

		return serverAllocationInfo;
	}

	@Override
	public void trafficExtraction(String namespace, String executorName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);
		validateIfExecutorNameExisted(executorName, curatorFrameworkOp);
		curatorFrameworkOp.create(ExecutorNodePath.getExecutorNoTrafficNodePath(executorName));
	}

	@Override
	public void trafficRecovery(String namespace, String executorName) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);
		validateIfExecutorNameExisted(executorName, curatorFrameworkOp);
		curatorFrameworkOp.deleteRecursive(ExecutorNodePath.getExecutorNoTrafficNodePath(executorName));
	}

	private void validateIfExecutorNameExisted(String executorName,
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
		if (!curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath(executorName))) {
			throw new SaturnJobConsoleException("The executorName(" + executorName + ") is not existed.");
		}
	}

	@Override
	public List<String> getAliveExecutorNames() {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		if (curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath())) {
			List<String> executorNames = curatorFrameworkOp.getChildren(ExecutorNodePath.getExecutorNodePath());
			if (executorNames != null) {
				List<String> aliveExecutorNames = new ArrayList<>(executorNames.size());
				for (String executorName : executorNames) {
					if (executorName != null) {
						String ip = null;
						try {
							ip = curatorFrameworkOp
									.getData(ExecutorNodePath.getExecutorNodePath(executorName, IP_NODE_NAME));
						} catch (Throwable t) {
							log.error(t.getMessage(), t);
						}
						if (StringUtils.isNotBlank(ip)) {
							aliveExecutorNames.add(executorName);
						}
					}
				}
				return aliveExecutorNames;
			}
		}
		return null;
	}

	@Override
	public boolean jobIncExceeds(int maxJobNum, int inc) throws SaturnJobConsoleException {
		if (maxJobNum <= 0) {
			return false;
		}
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		int curJobSize = jobDimensionService.getAllUnSystemJobs(curatorFrameworkOp).size();
		return (curJobSize + inc) > maxJobNum;
	}

	@Override
	public int getMaxJobNum() {
		int result = systemConfigService.getIntegerValue(SystemConfigProperties.MAX_JOB_NUM, DEFAULT_MAX_JOB_NUM);
		return result <= 0 ? DEFAULT_MAX_JOB_NUM : result;
	}

	@Override
	public RequestResult addJobs(JobConfig jobConfig) {
		RequestResult requestResult = new RequestResult();
		requestResult.setMessage("");
		requestResult.setSuccess(true);
		try {
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
			String jobName = jobConfig.getJobName();
			if (!curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(jobName))) {
				int maxJobNum = getMaxJobNum();
				if (jobIncExceeds(maxJobNum, 1)) {
					requestResult.setSuccess(false);
					String errorMsg = String.format("总作业数超过最大限制(%d)，作业名%s创建失败", maxJobNum, jobName);
					requestResult.setMessage(errorMsg);
				} else {
					if (jobConfig.getIsCopyJob()) {// 复制作业
						jobOperationService.copyAndPersistJob(jobConfig, curatorFrameworkOp);
					} else {
						jobOperationService.persistJob(jobConfig, curatorFrameworkOp);// 新增作业
					}
				}
			} else {
				requestResult.setSuccess(false);
				requestResult.setMessage("作业名" + jobName + "已经存在");
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			requestResult.setSuccess(false);
			requestResult.setMessage(t.getMessage());
		}
		return requestResult;
	}

	@Override
	public String removeJob(String jobName) {
		try {
			Stat itemStat = ThreadLocalCuratorClient.getCuratorClient().checkExists()
					.forPath(JobNodePath.getJobNodePath(jobName));
			if (itemStat != null) {
				long createTimeDiff = System.currentTimeMillis() - itemStat.getCtime();
				if (createTimeDiff < SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT) {
					return "作业【" + jobName + "】创建后" + (SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT / 60 / 1000)
							+ "分钟内不允许删除";
				}
			}

			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
			deleteJobFromDb(curatorFrameworkOp, jobName);

			// 1.作业的executor全online的情况，添加toDelete节点，触发监听器动态删除节点
			String toDeleteNodePath = JobNodePath.getConfigNodePath(jobName, "toDelete");
			if (curatorFrameworkOp.checkExists(toDeleteNodePath)) {
				curatorFrameworkOp.deleteRecursive(toDeleteNodePath);
			}
			curatorFrameworkOp.create(toDeleteNodePath);

			for (int i = 0; i < 20; i++) {
				// 2.作业的executor全offline的情况，或有几个online，几个offline的情况
				String jobServerPath = JobNodePath.getServerNodePath(jobName);
				if (!curatorFrameworkOp.checkExists(jobServerPath)) {
					// (1)如果不存在$Job/JobName/servers节点，说明该作业没有任何executor接管，可直接删除作业节点
					curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobName));
					return SaturnConstants.DEAL_SUCCESS;
				}
				// (2)如果该作业servers下没有任何executor，可直接删除作业节点
				List<String> executors = curatorFrameworkOp.getChildren(jobServerPath);
				if (CollectionUtils.isEmpty(executors)) {
					curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobName));
					return SaturnConstants.DEAL_SUCCESS;
				}
				// (3)只要该作业没有一个能运行的该作业的executor在线，那么直接删除作业节点
				boolean hasOnlineExecutor = false;
				for (String executor : executors) {
					if (curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath(executor, "ip"))
							&& curatorFrameworkOp.checkExists(JobNodePath.getServerStatus(jobName, executor))) {
						hasOnlineExecutor = true;
					} else {
						curatorFrameworkOp.deleteRecursive(JobNodePath.getServerNodePath(jobName, executor));
					}
				}
				if (!hasOnlineExecutor) {
					curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobName));
				}
				Thread.sleep(200);
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			return t.getMessage();
		}
		return SaturnConstants.DEAL_SUCCESS;
	}

	private void deleteJobFromDb(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp, String jobName)
			throws SaturnJobConsoleHttpException {
		String namespace = curatorFrameworkOp.getCuratorFramework().getNamespace();
		CurrentJobConfig currentJobConfig = currentJobConfigService.findConfigByNamespaceAndJobName(namespace, jobName);
		if (currentJobConfig == null) {
			log.warn("currentJobConfig from db does not exists,namespace and jobName is:" + namespace + " " + jobName);
			return;
		}
		try {
			currentJobConfigService.deleteByPrimaryKey(currentJobConfig.getId());
		} catch (Exception e) {
			log.error("exception is thrown during delete job config from db", e);
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	@Override
	public File getExportJobFile() throws SaturnJobConsoleException {
		try {
			File tmp = new File(SaturnConstants.CACHES_FILE_PATH,
					"tmp_exportFile_" + System.currentTimeMillis() + "_" + random.nextInt(1000) + ".xls");
			if (!tmp.exists()) {
				FileUtils.forceMkdir(tmp.getParentFile());
				tmp.createNewFile();
			}
			WritableWorkbook writableWorkbook = Workbook.createWorkbook(tmp);
			WritableSheet sheet1 = writableWorkbook.createSheet("Sheet1", 0);
			sheet1.addCell(new Label(0, 0, "作业名称"));
			sheet1.addCell(new Label(1, 0, "作业类型"));
			sheet1.addCell(new Label(2, 0, "作业实现类"));
			sheet1.addCell(new Label(3, 0, "cron表达式"));
			sheet1.addCell(new Label(4, 0, "作业描述"));

			Label localModeLabel = new Label(5, 0, "本地模式");
			setCellComment(localModeLabel, "对于非本地模式，默认为false；对于本地模式，该配置无效，固定为true");
			sheet1.addCell(localModeLabel);

			Label shardingTotalCountLabel = new Label(6, 0, "分片数");
			setCellComment(shardingTotalCountLabel, "对本地作业无效");
			sheet1.addCell(shardingTotalCountLabel);

			Label timeoutSecondsLabel = new Label(7, 0, "超时（Kill线程/进程）时间");
			setCellComment(timeoutSecondsLabel, "0表示无超时");
			sheet1.addCell(timeoutSecondsLabel);

			sheet1.addCell(new Label(8, 0, "自定义参数"));
			sheet1.addCell(new Label(9, 0, "分片序列号/参数对照表"));
			sheet1.addCell(new Label(10, 0, "Queue名"));
			sheet1.addCell(new Label(11, 0, "执行结果发送的Channel"));

			Label preferListLabel = new Label(12, 0, "优先Executor");
			setCellComment(preferListLabel, "可填executorName，多个元素使用英文逗号隔开");
			sheet1.addCell(preferListLabel);

			Label usePreferListOnlyLabel = new Label(13, 0, "只使用优先Executor");
			setCellComment(usePreferListOnlyLabel, "默认为false");
			sheet1.addCell(usePreferListOnlyLabel);

			sheet1.addCell(new Label(14, 0, "统计处理数据量的间隔秒数"));
			sheet1.addCell(new Label(15, 0, "负荷"));
			sheet1.addCell(new Label(16, 0, "显示控制台输出日志"));
			sheet1.addCell(new Label(17, 0, "暂停日期段"));
			sheet1.addCell(new Label(18, 0, "暂停时间段"));

			Label useSerialLabel = new Label(19, 0, "串行消费");
			setCellComment(useSerialLabel, "默认为false");
			sheet1.addCell(useSerialLabel);

			Label jobDegreeLabel = new Label(20, 0, "作业重要等级");
			setCellComment(jobDegreeLabel, "0:没有定义,1:非线上业务,2:简单业务,3:一般业务,4:重要业务,5:核心业务");
			sheet1.addCell(jobDegreeLabel);

			Label enabledReportLabel = new Label(21, 0, "上报运行状态");
			setCellComment(enabledReportLabel, "对于定时作业，默认为true；对于消息作业，默认为false");
			sheet1.addCell(enabledReportLabel);

			Label jobModeLabel = new Label(22, 0, "作业模式");
			setCellComment(jobModeLabel, "用户不能添加系统作业");
			sheet1.addCell(jobModeLabel);

			Label dependenciesLabel = new Label(23, 0, "依赖的作业");
			setCellComment(dependenciesLabel, "作业的启用、禁用会检查依赖关系的作业的状态。依赖多个作业，使用英文逗号给开。");
			sheet1.addCell(dependenciesLabel);

			Label groupsLabel = new Label(24, 0, "所属分组");
			setCellComment(groupsLabel, "作业所属分组，一个作业只能属于一个分组，一个分组可以包含多个作业");
			sheet1.addCell(groupsLabel);

			Label timeout4AlarmSecondsLabel = new Label(25, 0, "超时（告警）时间");
			setCellComment(timeout4AlarmSecondsLabel, "0表示无超时");
			sheet1.addCell(timeout4AlarmSecondsLabel);

			Label timeZoneLabel = new Label(26, 0, "时区");
			setCellComment(timeZoneLabel, "作业运行时区");
			sheet1.addCell(timeZoneLabel);

			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
			List<String> jobNames = jobDimensionService.getAllUnSystemJobs(curatorFrameworkOp);
			for (int i = 0; i < jobNames.size(); i++) {
				try {
					String jobName = jobNames.get(i);
					sheet1.addCell(new Label(0, i + 1, jobName));
					sheet1.addCell(new Label(1, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobType"))));
					sheet1.addCell(new Label(2, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobClass"))));
					sheet1.addCell(new Label(3, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "cron"))));
					sheet1.addCell(new Label(4, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "description"))));
					sheet1.addCell(new Label(5, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "localMode"))));
					sheet1.addCell(new Label(6, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "shardingTotalCount"))));
					sheet1.addCell(new Label(7, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeoutSeconds"))));
					sheet1.addCell(new Label(8, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobParameter"))));
					sheet1.addCell(new Label(9, i + 1, curatorFrameworkOp
							.getData(JobNodePath.getConfigNodePath(jobName, "shardingItemParameters"))));
					sheet1.addCell(new Label(10, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "queueName"))));
					sheet1.addCell(new Label(11, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "channelName"))));
					sheet1.addCell(new Label(12, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "preferList"))));
					String useDispreferList = curatorFrameworkOp
							.getData(JobNodePath.getConfigNodePath(jobName, "useDispreferList"));
					if (useDispreferList != null) {
						useDispreferList = String.valueOf(!Boolean.valueOf(useDispreferList));
					}
					sheet1.addCell(new Label(13, i + 1, useDispreferList));
					sheet1.addCell(new Label(14, i + 1, curatorFrameworkOp
							.getData(JobNodePath.getConfigNodePath(jobName, "processCountIntervalSeconds"))));
					sheet1.addCell(new Label(15, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "loadLevel"))));
					sheet1.addCell(new Label(16, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "showNormalLog"))));
					sheet1.addCell(new Label(17, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodDate"))));
					sheet1.addCell(new Label(18, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodTime"))));
					sheet1.addCell(new Label(19, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "useSerial"))));
					sheet1.addCell(new Label(20, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobDegree"))));
					sheet1.addCell(new Label(21, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "enabledReport"))));
					sheet1.addCell(new Label(22, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobMode"))));
					sheet1.addCell(new Label(23, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "dependencies"))));
					sheet1.addCell(new Label(24, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "groups"))));
					sheet1.addCell(new Label(25, i + 1, curatorFrameworkOp
							.getData(JobNodePath.getConfigNodePath(jobName, "timeout4AlarmSeconds"))));
					sheet1.addCell(new Label(26, i + 1,
							curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeZone"))));
				} catch (Exception e) {
					log.error("export job exception:", e);
					continue;
				}
			}

			writableWorkbook.write();
			writableWorkbook.close();

			return tmp;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		}
	}

	private void setCellComment(WritableCell cell, String comment) {
		WritableCellFeatures cellFeatures = new WritableCellFeatures();
		cellFeatures.setComment(comment);
		cell.setCellFeatures(cellFeatures);
	}

	@Override
	public RequestResult shardAllAtOnce() throws SaturnJobConsoleException {
		try {
			RequestResult requestResult = new RequestResult();
			CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
			String shardAllAtOnceNodePath = ExecutorNodePath.getExecutorShardingNodePath("shardAllAtOnce");
			if (curatorFrameworkOp.checkExists(shardAllAtOnceNodePath)) {
				curatorFrameworkOp.deleteRecursive(shardAllAtOnceNodePath);
			}
			curatorFrameworkOp.create(shardAllAtOnceNodePath);
			requestResult.setMessage("");
			requestResult.setSuccess(true);
			return requestResult;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		}
	}

	@Override
	public void shardAll(String namespace) throws SaturnJobConsoleException {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = getCuratorFrameworkOp(namespace);
		String shardAllAtOnceNodePath = ExecutorNodePath.getExecutorShardingNodePath("shardAllAtOnce");
		curatorFrameworkOp.deleteRecursive(shardAllAtOnceNodePath);
		curatorFrameworkOp.create(shardAllAtOnceNodePath);
	}


	private CuratorFrameworkOp getCuratorFrameworkOp(String namespace) throws SaturnJobConsoleException {
		CuratorFrameworkOp curatorFrameworkOp;
		try {
			curatorFrameworkOp = registryCenterService.getCuratorFrameworkOp(namespace);
		} catch (SaturnJobConsoleException e) {
			throw new SaturnJobConsoleException("no CuratorFramework found for namespace:" + namespace);
		}
		return curatorFrameworkOp;
	}
}
