package com.vip.saturn.job.console.service.impl;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.constants.SaturnConstants;
import com.vip.saturn.job.console.domain.JobBriefInfo.JobType;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.ExecutorService;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.utils.CommonUtils;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.ThreadLocalCuratorClient;
import jxl.Workbook;
import jxl.write.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.File;
import java.lang.Boolean;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 
 * @author xiaopeng.he
 *
 */
@Service
public class ExecutorServiceImpl implements ExecutorService {

	protected static Logger log = LoggerFactory.getLogger(ExecutorServiceImpl.class);
	public static final String ROOT = ExecutorNodePath.get$ExecutorNodePath();

	public static final String IP_NODE_NAME = "ip";

	@Resource
	private CuratorRepository curatorRepository;
	@Resource
	private JobDimensionService jobDimensionService;

	private Random random = new Random();

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
							ip = curatorFrameworkOp.getData(ExecutorNodePath.getExecutorNodePath(executorName,IP_NODE_NAME));
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
	
	/**
	 * 如果当前有新增作业，而且新增后超出该域最大作业数量限制，返回false；否则返回true。
	 * @param jobNames 新增作业
	 * @return 是否超出
	 */
	public boolean check(List<String> jobNames) {
		List<String> children = CommonUtils.getJobNames(curatorRepository.inSessionClient());
		Set<String> set = new HashSet<>();
		if(children != null && !children.isEmpty()) {
			for(String tmp : children) {
				set.add(tmp);
			}
		}
		boolean hasNew = false;
		if(jobNames != null && !jobNames.isEmpty()) {
			for(String tmp : jobNames) {
				if(!set.contains(tmp)) {
					hasNew = true;
					set.add(tmp);
				}
			}
		}
		return !(hasNew && set.size() > SaturnEnvProperties.MAX_NUMBER_OF_JOBS);
	}
	
	@Override
	public RequestResult addJobs(JobConfig jobConfig) {
		RequestResult requestResult = new RequestResult();
		requestResult.setMessage("");
		requestResult.setSuccess(true);
		try {
			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
			String jobName = jobConfig.getJobName();
			if (!curatorFrameworkOp.checkExists(JobNodePath.getJobNodePath(jobName))) {
				if(jobConfig.getIsCopyJob()){// 复制作业
					copyAndPersisJobJobConfig(jobConfig);
				}else{
					persisJobConfig(jobConfig);// 新增作业
				}
			} else {
				requestResult.setSuccess(false);
				requestResult.setMessage("作业名" + jobName + "已经存在" );
			}
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			requestResult.setSuccess(false);
			requestResult.setMessage(t.getMessage());
		}
		return requestResult;
	}

	private void persisJobConfig(JobConfig jobConfig) {
		jobConfig.setDefaultValues();
		String jobName = jobConfig.getJobName();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "enabled"), "false");
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "jobType"), jobConfig.getJobType());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "shardingItemParameters"), jobConfig.getShardingItemParameters());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "jobParameter"), jobConfig.getJobParameter());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "queueName"), jobConfig.getQueueName());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "channelName"), jobConfig.getChannelName());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "failover"), "true");
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "timeoutSeconds"), jobConfig.getTimeoutSeconds());
		if(JobType.MSG_JOB.name().equals(jobConfig.getJobType())){// MSG作业没有cron表达式
			curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "cron"), "");
		}else{
			curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "cron"), jobConfig.getCron());
		}
		if(!Strings.isNullOrEmpty(jobConfig.getPausePeriodDate())){
			curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "pausePeriodDate"), jobConfig.getPausePeriodDate());
		}
		if(!Strings.isNullOrEmpty(jobConfig.getPausePeriodTime())){
			curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "pausePeriodTime"), jobConfig.getPausePeriodTime());
		}
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "processCountIntervalSeconds"), jobConfig.getProcessCountIntervalSeconds());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "shardingTotalCount"), jobConfig.getShardingTotalCount());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "showNormalLog"), jobConfig.getShowNormalLog());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "loadLevel"), jobConfig.getLoadLevel());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "preferList"), jobConfig.getPreferList());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "useDispreferList"), jobConfig.getUseDispreferList());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "localMode"), jobConfig.getLocalMode());
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "useSerial"), jobConfig.getUseSerial());
		if(JobType.SHELL_JOB.name().equals(jobConfig.getJobType())){
			curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "jobClass"), "");
		}else{
			curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "jobClass"), jobConfig.getJobClass());
		}
		curatorFrameworkOp.fillJobNodeIfNotExist(JobNodePath.getConfigNodePath(jobName, "description"), jobConfig.getDescription());
	}
	
	private void copyAndPersisJobJobConfig(JobConfig jobConfig) throws Exception {
		String originJobName = jobConfig.getOriginJobName();
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		List<String> jobConfigNodes = curatorFrameworkOp.getChildren(JobNodePath.getConfigNodePath(originJobName));
		if(CollectionUtils.isEmpty(jobConfigNodes)){
			return;
		}
		Class<?> cls = jobConfig.getClass();
		for(String jobConfigNode : jobConfigNodes){
			String jobConfigPath = JobNodePath.getConfigNodePath(originJobName, jobConfigNode);
			String jobConfigValue = curatorFrameworkOp.getData(jobConfigPath);
			if("enabled".equals(jobConfigNode)){// enabled固定为false
				String fillJobNodePath = JobNodePath.getConfigNodePath(jobConfig.getJobName(), jobConfigNode);
				curatorFrameworkOp.fillJobNodeIfNotExist(fillJobNodePath,"false");
				continue;
			}
			if("failover".equals(jobConfigNode)){// failover固定为true
				String fillJobNodePath = JobNodePath.getConfigNodePath(jobConfig.getJobName(), jobConfigNode);
				curatorFrameworkOp.fillJobNodeIfNotExist(fillJobNodePath,"true");
				continue;
			}
			try{
				Field field = cls.getDeclaredField(jobConfigNode);
				field.setAccessible(true);
				Object fieldValue = field.get(jobConfig);
				if(fieldValue != null){
					jobConfigValue = fieldValue.toString();
				}
			}catch(NoSuchFieldException e){// 即使JobConfig类中不存在该属性也复制（一般是旧版作业的一些节点，可以在旧版Executor上运行）
				continue;
			}finally{
				String fillJobNodePath = JobNodePath.getConfigNodePath(jobConfig.getJobName(), jobConfigNode);
				curatorFrameworkOp.fillJobNodeIfNotExist(fillJobNodePath,jobConfigValue);
			}
		}
	}
	
	@Override
	public String removeJob(String jobName) {
		try {
			Stat itemStat = ThreadLocalCuratorClient.getCuratorClient().checkExists().forPath(JobNodePath.getJobNodePath(jobName));
			if(itemStat != null){
				long createTimeDiff = System.currentTimeMillis() - itemStat.getCtime();
				if(createTimeDiff < SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT){
					return "作业【"+jobName+"】创建后"+ (SaturnConstants.JOB_CAN_BE_DELETE_TIME_LIMIT / 60 / 1000) +"分钟内不允许删除";
				}
			}

			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
			//1.作业的executor全online的情况，添加toDelete节点，触发监听器动态删除节点
			String toDeleteNodePath = JobNodePath.getConfigNodePath(jobName, "toDelete");
			if(curatorFrameworkOp.checkExists(toDeleteNodePath)){
				curatorFrameworkOp.deleteRecursive(toDeleteNodePath);
			}
			curatorFrameworkOp.create(toDeleteNodePath);
			
			for(int i=0;i<20;i++){
				// 2.作业的executor全offline的情况，或有几个online，几个offline的情况
				String jobServerPath = JobNodePath.getServerNodePath(jobName);
				if(!curatorFrameworkOp.checkExists(jobServerPath)){
					// (1)如果不存在$Job/JobName/servers节点，说明该作业没有任何executor接管，可直接删除作业节点
					curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobName));
					return SaturnConstants.DEAL_SUCCESS;
				}
				// (2)如果该作业servers下没有任何executor，可直接删除作业节点
				List<String> executors = curatorFrameworkOp.getChildren(jobServerPath);
				if(CollectionUtils.isEmpty(executors)){
					curatorFrameworkOp.deleteRecursive(JobNodePath.getJobNodePath(jobName));
					return SaturnConstants.DEAL_SUCCESS;
				}
				// (3)只要该作业没有一个executor在线，那么直接删除作业节点
				boolean hasOnlineExecutor = false;
				for(String executor : executors){
					if(curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath(executor, "ip"))){
						hasOnlineExecutor = true;
						break;
					}
				}
				if(!hasOnlineExecutor){
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

	@Override
	public File getExportJobFile() throws SaturnJobConsoleException {
		try {
			String sep = System.getProperty("file.separator");
			File tmp = new File(System.getProperty("user.home") + sep + ".saturn" + sep + "saturn_console" + sep + "caches" + sep + "tmp_exportFile_" + System.currentTimeMillis() + "_" + random.nextInt(1000) + ".xls");
			if(!tmp.exists()) {
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

			Label timeoutSecondsLabel = new Label(7, 0, "超时时间");
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
			sheet1.addCell(new Label(16, 0, "显示正常日志"));
			sheet1.addCell(new Label(17, 0, "暂停日期段"));
			sheet1.addCell(new Label(18, 0, "暂停时间段"));

			Label useSerialLabel = new Label(19, 0, "串行消费");
			setCellComment(useSerialLabel, "默认为false");
			sheet1.addCell(useSerialLabel);

			CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
			List<String> jobNames = CommonUtils.getJobNames(curatorFrameworkOp);
			for (int i=0; i<jobNames.size(); i++) {
				String jobName = jobNames.get(i);
				sheet1.addCell(new Label(0, i + 1, jobName));
				sheet1.addCell(new Label(1, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobType"))));
				sheet1.addCell(new Label(2, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobClass"))));
				sheet1.addCell(new Label(3, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "cron"))));
				sheet1.addCell(new Label(4, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "description"))));
				sheet1.addCell(new Label(5, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "localMode"))));
				sheet1.addCell(new Label(6, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "shardingTotalCount"))));
				sheet1.addCell(new Label(7, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeoutSeconds"))));
				sheet1.addCell(new Label(8, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobParameter"))));
				sheet1.addCell(new Label(9, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "shardingItemParameters"))));
				sheet1.addCell(new Label(10, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "queueName"))));
				sheet1.addCell(new Label(11, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "channelName"))));
				sheet1.addCell(new Label(12, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "preferList"))));
				String useDispreferList = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "useDispreferList"));
				if(useDispreferList != null) {
					useDispreferList = String.valueOf(!Boolean.valueOf(useDispreferList));
				}
				sheet1.addCell(new Label(13, i + 1, useDispreferList));
				sheet1.addCell(new Label(14, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "processCountIntervalSeconds"))));
				sheet1.addCell(new Label(15, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "loadLevel"))));
				sheet1.addCell(new Label(16, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "showNormalLog"))));
				sheet1.addCell(new Label(17, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodDate"))));
				sheet1.addCell(new Label(18, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodTime"))));
				sheet1.addCell(new Label(19, i + 1, curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "useSerial"))));
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

}
