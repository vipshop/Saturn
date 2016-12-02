/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.service.impl;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.vip.saturn.job.console.constants.SaturnConstants;
import com.vip.saturn.job.console.domain.*;
import com.vip.saturn.job.console.domain.ExecutionInfo.ExecutionStatus;
import com.vip.saturn.job.console.domain.JobBriefInfo.JobType;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.JobDimensionService;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.BooleanWrapper;
import com.vip.saturn.job.console.utils.CommonUtils;
import com.vip.saturn.job.console.utils.ExecutorNodePath;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.*;
@Service
public class JobDimensionServiceImpl implements JobDimensionService {

	protected static Logger log = LoggerFactory.getLogger(JobDimensionServiceImpl.class);

    @Resource
    private CuratorRepository curatorRepository;

    private DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.SIMPLIFIED_CHINESE);

	protected static Logger AUDITLOGGER = LoggerFactory.getLogger("AUDITLOG");

    @Resource
    private RegistryCenterService registryCenterService;

    @Override
    public Collection<JobBriefInfo> getAllJobsBriefInfo() {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		List<String> jobNames = CommonUtils.getJobNames(curatorFrameworkOp);
		List<JobBriefInfo> result = new ArrayList<>(jobNames.size());
        for (String jobName : jobNames) {
        	try{
	            JobBriefInfo jobBriefInfo = new JobBriefInfo();
	            jobBriefInfo.setJobName(jobName);
	            jobBriefInfo.setIsJobEnabled(isJobEnabled(jobName));
	            jobBriefInfo.setDescription(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "description")));
	            jobBriefInfo.setStatus(getJobStatus(jobName));
	            jobBriefInfo.setJobParameter(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobParameter")));
	            jobBriefInfo.setShardingItemParameters(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "shardingItemParameters")));
	            jobBriefInfo.setQueueName(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "queueName")));
	            jobBriefInfo.setChannelName(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "channelName")));
	            jobBriefInfo.setLoadLevel(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "loadLevel")));
	            jobBriefInfo.setShardingTotalCount(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "shardingTotalCount")));
	            jobBriefInfo.setTimeoutSeconds(Integer.parseInt(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeoutSeconds"))));
	            jobBriefInfo.setPausePeriodDate(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodDate")));
	            jobBriefInfo.setPausePeriodTime(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodTime")));
	            jobBriefInfo.setShowNormalLog(Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "showNormalLog"))));
	            jobBriefInfo.setLocalMode(Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "localMode"))));
	            jobBriefInfo.setUseSerial(Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "useSerial"))));
	            jobBriefInfo.setUseDispreferList((Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "useDispreferList")))));
	            jobBriefInfo.setProcessCountIntervalSeconds(Integer.parseInt(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "processCountIntervalSeconds"))));
	            jobBriefInfo.setJobRate(geJobRunningInfo(jobName));
	            String preferList = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "preferList"));
	    		StringBuilder allPreferExecutorsBuilder = new StringBuilder();
	    		if(!Strings.isNullOrEmpty(preferList)){
	    			List<String> executors = null;
	                String executorsNodePath = SaturnExecutorsNode.getExecutorsNodePath();
	        		if(curatorFrameworkOp.checkExists(executorsNodePath)) {
	        			executors = curatorFrameworkOp.getChildren(executorsNodePath);
	        		}
	    			boolean hasExecutors = !CollectionUtils.isEmpty(executors);
					String[] preferExecutorList = preferList.split(",");
					for(String preferExecutor : preferExecutorList){
						if(hasExecutors && !executors.contains(preferExecutor)){ //NOSONAR
							allPreferExecutorsBuilder.append(preferExecutor + "(已删除)").append(",");
						}else{
							allPreferExecutorsBuilder.append(preferExecutor).append(",");
						}
					}
					if(!Strings.isNullOrEmpty(allPreferExecutorsBuilder.toString())){
						jobBriefInfo.setPreferList(allPreferExecutorsBuilder.substring(0,allPreferExecutorsBuilder.length()-1));
					}
				}
	            jobBriefInfo.setCron(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "cron")));
	            jobBriefInfo.setJobClass(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobClass")));
	            jobBriefInfo.setJobType(JobType.getJobType(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobType"))));
	            if(JobType.UNKOWN_JOB.equals(jobBriefInfo.getJobType())) {
	            	if (jobBriefInfo.getJobClass() != null && jobBriefInfo.getJobClass().indexOf("SaturnScriptJob") != -1) {
	            		jobBriefInfo.setJobType(JobType.SHELL_JOB);
	    			} else {
	    				jobBriefInfo.setJobType(JobType.JAVA_JOB);
	    			}
	            }
	            if(!JobStatus.STOPPED.equals(jobBriefInfo.getStatus())){// 作业如果是STOPPED状态，不需要显示已分配的executor
	            	String executorsPath = JobNodePath.getServerNodePath(jobName);
	            	if(curatorFrameworkOp.checkExists(executorsPath)) {
	            		List<String> executors = curatorFrameworkOp.getChildren(executorsPath);
	            		if (executors != null && !executors.isEmpty()) {
	            			StringBuilder shardingListSb = new StringBuilder();
	            			for(String executor : executors){
	            				String sharding = curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName,executor,"sharding"));
	            				if(!Strings.isNullOrEmpty(sharding)){
	            					shardingListSb.append(executor).append(",");
	            				}
	            			}
	            			if(shardingListSb != null && shardingListSb.length() > 0){
	            				jobBriefInfo.setShardingList(shardingListSb.substring(0, shardingListSb.length() - 1));
	            			}
	            		}
	            	}
	            }
	            // set nextfire time
	            String executionRootpath = JobNodePath.getExecutionNodePath(jobName);
	            if (curatorFrameworkOp.checkExists(executionRootpath)) {
	            	List<String> items = curatorFrameworkOp.getChildren(executionRootpath);
	            	if (items != null && !items.isEmpty()) {
	            		List<String> lastBeginTimeList = new ArrayList<>();
	                	List<String> lastCompleteTimeList = new ArrayList<>();
	                	List<String> nextFireTimeList = new ArrayList<>();
	                	int runningItemSize = 0;
	            		for(String item : items){
	            			if(getRunningIP(item, jobName) == null) {
	            				continue;
	                        }
	            			++runningItemSize;
	            			String lastBeginTime = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "lastBeginTime"));
	            			if(null != lastBeginTime){
	            				lastBeginTimeList.add(lastBeginTime);
	            			}
	            			String lastCompleteTime = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "lastCompleteTime"));
	            			if(null != lastCompleteTime){
	            				boolean isItemCompleted = curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(jobName, item, "completed"));
	            				boolean isItemRunning = curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(jobName, item, "running"));
	            	    		if(isItemCompleted && !isItemRunning){// 如果作业分片已执行完毕，则添加该完成时间到集合中进行排序
	            	    			lastCompleteTimeList.add(lastCompleteTime);
	            	    		}
	            			}
	            			String nextFireTime = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "nextFireTime"));
	            			if(null != nextFireTime){
	            				nextFireTimeList.add(nextFireTime);
	            			}
	            		}
	            		if(!CollectionUtils.isEmpty(lastBeginTimeList)){
	                		Collections.sort(lastBeginTimeList);// 对时间戳进行排序
	                		jobBriefInfo.setLastBeginTime(dateFormat.format(new Date(Long.parseLong(lastBeginTimeList.get(0)))));// 所有分片中最近最早的开始时间
	                	}
	                	if(!CollectionUtils.isEmpty(lastCompleteTimeList) && lastCompleteTimeList.size() == runningItemSize){// 所有分配都完成才显示最近最晚的完成时间
	                		Collections.sort(lastCompleteTimeList);// 对时间戳进行排序
	                		jobBriefInfo.setLastCompleteTime(dateFormat.format(new Date(Long.parseLong(lastCompleteTimeList.get(lastCompleteTimeList.size() - 1)))));// 所有分片中最近最晚的完成时间
	                	}
	                	if(!CollectionUtils.isEmpty(nextFireTimeList)){
	                		Collections.sort(nextFireTimeList);// 对时间戳进行排序
	                		jobBriefInfo.setNextFireTime(dateFormat.format(new Date(Long.parseLong(nextFireTimeList.get(0)))));// 所有分片中下次最早的开始时间
	                	}
					}
	            }
	            result.add(jobBriefInfo);
        	}catch(Exception e){
        		log.error(e.getMessage(), e);
        		continue;
        	}
        }
        Collections.sort(result);
        return result;
    }

	public String geJobRunningInfo(final String jobName) {
		String serverNodePath = JobNodePath.getServerNodePath(jobName);
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
    	if(!curatorFrameworkOp.checkExists(serverNodePath)) {
    		return "";
    	}
		List<String> servers = curatorFrameworkOp.getChildren(serverNodePath);
		// server为空表示没有Server工作，这个时候作业状态应该是Crashed
		if (servers == null || servers.size() == 0) {
			return "";
		}

		int processSuccessCount = 0;
		int processFailureCount = 0;

		for (String each : servers) {
			String processSuccessCountStr = curatorFrameworkOp
					.getData(JobNodePath.getServerNodePath(jobName, each, "processSuccessCount"));
			if (!Strings.isNullOrEmpty(processSuccessCountStr)) {
				processSuccessCount += Integer.parseInt(processSuccessCountStr);
			}

			String processFailureCountStr = curatorFrameworkOp
					.getData(JobNodePath.getServerNodePath(jobName, each, "processFailureCount"));
			if (!Strings.isNullOrEmpty(processFailureCountStr)) {
				processFailureCount += Integer.parseInt(processFailureCountStr);
			}
		}

		int count = processSuccessCount;
		int total = processSuccessCount + processFailureCount;
		if (total == 0) {
			return "";
		}
		NumberFormat numberFormat = NumberFormat.getInstance();
		// 设置精确到小数点后2位
		numberFormat.setMaximumFractionDigits(2);
		String result = numberFormat.format((float) count / (float) total * 100);
		return result + "%";
	}

    @Override
    public JobStatus getJobStatus(final String jobName) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
    	// see if all the shards is finished.
    	List<String> executionItems = curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName));
    	boolean isAllShardsFinished = true;
    	if (executionItems != null && !executionItems.isEmpty()) {
	    	for (String itemStr: executionItems) {
	    		boolean isItemCompleted = curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(jobName, itemStr, "completed"));
	    		boolean isItemRunning = curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(jobName, itemStr, "running"));
	    		// if executor is kill by -9 while it is running, completed node won't exists as well as running node.
	    		// under this circumstance, we consider it is completed.
	    		if (!isItemCompleted && isItemRunning) {
	    			isAllShardsFinished = false;
	    			break;
	    		}
			}
    	}
    	// see if the job is enabled or not.
    	boolean enabled = Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "enabled")));
    	if (enabled) {
    		if (isAllShardsFinished) {
        		return JobStatus.READY;
			}
    		return JobStatus.RUNNING;
    	} else {
    		if (isAllShardsFinished) {
        		return JobStatus.STOPPED;
			}
    		return JobStatus.STOPPING;
    	}
    }

    @Override
    public JobSettings getJobSettings(final String jobName, RegistryCenterConfiguration configInSession) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
        JobSettings result = new JobSettings();
        result.setJobName(jobName);
        result.setJobClass(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobClass")));
        result.setShardingTotalCount(Integer.parseInt(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "shardingTotalCount"))));
        result.setCron(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "cron")));
        result.setPausePeriodDate(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodDate")));
        result.setPausePeriodTime(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "pausePeriodTime")));
        result.setShardingItemParameters(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "shardingItemParameters")));
        result.setJobParameter(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobParameter")));
        result.setProcessCountIntervalSeconds(Integer.parseInt(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "processCountIntervalSeconds"))));
		result.setTimeoutSeconds(
				Integer.parseInt(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "timeoutSeconds"))));
        String lv = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "loadLevel"));
        if (Strings.isNullOrEmpty(lv)) {
       	 	result.setLoadLevel(1);
        } else {
            result.setLoadLevel(Integer.parseInt(lv));
        }
        result.setEnabled(Boolean.valueOf(JobNodePath.getConfigNodePath(jobName, "enabled")));//默认是禁用的
        result.setPreferList(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "preferList")));
        result.setPreferListCandidate(getAllExecutors(jobName));
        String useDispreferList = curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "useDispreferList"));
        if(Strings.isNullOrEmpty(useDispreferList)){
        	result.setUseDispreferList(null);
        }else{
        	result.setUseDispreferList(Boolean.valueOf(useDispreferList));
        }
        result.setUseSerial(Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "useSerial"))));
        result.setLocalMode(Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "localMode"))));
        // result.setFailover(Boolean.valueOf(curatorRepository.getData(JobNodePath.getConfigNodePath(jobName, "failover"))));
        result.setDescription(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "description")));
        result.setQueueName(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "queueName")));
        result.setChannelName(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "channelName")));
		if (curatorFrameworkOp.checkExists(JobNodePath.getConfigNodePath(jobName, "showNormalLog")) == false) {
			curatorFrameworkOp.create(JobNodePath.getConfigNodePath(jobName, "showNormalLog"));
		}
        result.setJobType(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobType")));
        // 兼容旧版没有msg_job。
        if (StringUtils.isBlank(result.getJobType())) {
			if (result.getJobClass().indexOf("script") > 0) {
				result.setJobType(JobType.SHELL_JOB.name());
			} else {
				result.setJobType(JobType.JAVA_JOB.name());
			}
		}
        result.setShowNormalLog(Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "showNormalLog"))));
        return result;
    }

    @Override
    public String updateJobSettings(final JobSettings jobSettings, RegistryCenterConfiguration configInSession) {
    	// Modify JobSettings.updateFields() sync, if the update fields changed.
		jobSettings.setDefaultValues();
		BooleanWrapper bw = new BooleanWrapper(false);
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
    	try {
			curatorFrameworkOp.inTransaction()
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "shardingTotalCount"), jobSettings.getShardingTotalCount(), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "loadLevel"), jobSettings.getLoadLevel(), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "cron"), StringUtils.trim(jobSettings.getCron()), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "pausePeriodDate"), jobSettings.getPausePeriodDate(), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "pausePeriodTime"), jobSettings.getPausePeriodTime(), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "shardingItemParameters"), jobSettings.getShardingItemParameters(), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "jobParameter"), jobSettings.getJobParameter(), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "processCountIntervalSeconds"), jobSettings.getProcessCountIntervalSeconds(), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "timeoutSeconds"), jobSettings.getTimeoutSeconds(), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "description"), jobSettings.getDescription(), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "channelName"), StringUtils.trim(jobSettings.getChannelName()), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "queueName"), StringUtils.trim(jobSettings.getQueueName()), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "showNormalLog"), jobSettings.getShowNormalLog(), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "preferList"), jobSettings.getPreferList(), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "useDispreferList"), jobSettings.getUseDispreferList(), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "failover"), jobSettings.getFailover(), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "localMode"), jobSettings.getLocalMode(), bw)
				.replaceIfchanged(JobNodePath.getConfigNodePath(jobSettings.getJobName(), "useSerial"), jobSettings.getUseSerial(), bw)
				.commit();
		} catch (Exception e) {
			log.error("update settings to zk failed: {}", e.getMessage());
			log.error(e.getMessage(),e);
			return e.getMessage();
		}
        return null;
    }

    @Override
    public Collection<JobServer> getServers(final String jobName) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
    	String serverNodePath = JobNodePath.getServerNodePath(jobName);
    	List<String> serverIps = new ArrayList<>();
    	if(curatorFrameworkOp.checkExists(serverNodePath)) {
    		serverIps = curatorFrameworkOp.getChildren(serverNodePath);
    	}
        String leaderIp = curatorFrameworkOp.getData(JobNodePath.getLeaderNodePath(jobName, "election/host"));
        Collection<JobServer> result = new ArrayList<>(serverIps.size());
        for (String each : serverIps) {
            result.add(getJobServer(jobName, leaderIp, each));
        }
        return result;
    }

    @Override
    public void getServersVersion(final String jobName,List<HealthCheckJobServer> allJobServers,RegistryCenterConfiguration registryCenterConfig) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
    	String serverNodePath = JobNodePath.getServerNodePath(jobName);
    	List<String> executorNames = new ArrayList<>();
    	if(curatorFrameworkOp.checkExists(serverNodePath)) {
    		executorNames = curatorFrameworkOp.getChildren(serverNodePath);
    	}
        for (String executorName : executorNames) {
        	if(allJobServers.size() >= SaturnConstants.HEALTH_CHECK_VERSION_MAX_SIZE){// 容量控制，最多查询10000条
        		break;
        	}
        	allJobServers.add(getJobServerVersion(jobName, executorName, registryCenterConfig));
        }
    }

    private JobServer getJobServer(final String jobName, final String leaderIp, final String serverIp) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
        JobServer result = new JobServer();
        result.setExecutorName(serverIp);
        result.setIp(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, serverIp, "ip")));
        result.setVersion(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, serverIp, "version")));
        String processSuccessCount = curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, serverIp, "processSuccessCount"));
        result.setProcessSuccessCount(null == processSuccessCount ? 0 : Integer.parseInt(processSuccessCount));
        String processFailureCount = curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, serverIp, "processFailureCount"));
        result.setProcessFailureCount(null == processFailureCount ? 0 : Integer.parseInt(processFailureCount));
        result.setSharding(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, serverIp, "sharding")));
        result.setStatus(getServerStatus(jobName, serverIp));
        result.setLeader(serverIp.equals(leaderIp));
        result.setJobStatus(getJobStatus(jobName));
        return result;
    }

    private HealthCheckJobServer getJobServerVersion(final String jobName, final String executorName, RegistryCenterConfiguration registryCenterConfig) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
    	HealthCheckJobServer result = new HealthCheckJobServer();
        result.setExecutorName(executorName);
        result.setJobName(jobName);
        result.setNamespace(registryCenterConfig.getNamespace());
        result.setVersion(curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, executorName, "version")));
        return result;
    }

    private ServerStatus getServerStatus(final String jobName, final String serverIp) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
        String ip = curatorFrameworkOp.getData(ExecutorNodePath.getExecutorNodePath(serverIp, "ip"));
        return ServerStatus.getServerStatus(ip);
    }

    @Override
    public Collection<ExecutionInfo> getExecutionInfo(final String jobName) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
    	if(JobStatus.STOPPED.equals(getJobStatus(jobName))){
    		return Collections.emptyList();
    	}
        String executionRootpath = JobNodePath.getExecutionNodePath(jobName);
        if (!curatorFrameworkOp.checkExists(executionRootpath)) {
            return Collections.emptyList();
        }
        List<String> items = curatorFrameworkOp.getChildren(executionRootpath);
        List<ExecutionInfo> result = new ArrayList<>(items.size());
        for (String each : items) {
            if(getRunningIP(each, jobName) != null) { //  || hasFailoverExecutor(each, jobName)
                result.add(getExecutionInfo(jobName, each));
            }
        }
        Collections.sort(result);
        return result;
    }

	@Override
	public ExecutionInfo getExecutionJobLog(String jobName, int item) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		ExecutionInfo result = new ExecutionInfo();
		String logMsg = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, String.valueOf(item), "jobLog"));
		result.setLogMsg(logMsg);
		return result;
	}

    private ExecutionInfo getExecutionInfo(final String jobName, final String item) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
        ExecutionInfo result = new ExecutionInfo();
        result.setJobName(jobName);
        result.setItem(Integer.parseInt(item));
        boolean running = curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(jobName, item, "running"));
        boolean completed = curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(jobName, item, "completed"));
        boolean failed = curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(jobName, item, "failed"));
        boolean timeout = curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(jobName, item, "timeout"));

        result.setStatus(ExecutionStatus.getExecutionStatus(running, completed, failed, timeout));

        String jobMsg = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "jobMsg"));
        result.setJobMsg(jobMsg);

		String runningIp = getRunningIP(item, jobName);
		result.setRunningIp(runningIp == null ? "未找到" : runningIp);

        if (curatorFrameworkOp.checkExists(JobNodePath.getExecutionNodePath(jobName, item, "failover"))) {
            result.setFailoverExecutor(curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "failover")));
        }
        String lastBeginTime = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "lastBeginTime"));
        result.setLastBeginTime(null == lastBeginTime ? null : dateFormat.format(new Date(Long.parseLong(lastBeginTime))));
        String nextFireTime = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "nextFireTime"));
        result.setNextFireTime(null == nextFireTime ? null : dateFormat.format(new Date(Long.parseLong(nextFireTime))));
        String pausePeriodEffected = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "pausePeriodEffected"));
        result.setPausePeriodEffected(Boolean.parseBoolean(pausePeriodEffected));
        String lastCompleteTime = curatorFrameworkOp.getData(JobNodePath.getExecutionNodePath(jobName, item, "lastCompleteTime"));
    	result.setLastCompleteTime(null == lastCompleteTime ? null : dateFormat.format(new Date(Long.parseLong(lastCompleteTime))));
        if( result.getStatus().equals(ExecutionStatus.RUNNING) ) {
            result.setTimeConsumed( (new Date().getTime() - Long.parseLong(lastBeginTime))/1000 );
        }
        return result;
    }

	/**
	 * 查找运行item的服务器IP
	 *
	 * @param item 作业分片
	 * @param jobName 作业名称
	 * @return 运行item的服务器IP
	 */
	private String getRunningIP(String item, String jobName) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		String runningIp = null;
		String serverNodePath = JobNodePath.getServerNodePath(jobName);
		if(!curatorFrameworkOp.checkExists(serverNodePath)) {
			return runningIp;
		}
		List<String> servers = curatorFrameworkOp.getChildren(serverNodePath);
		for (String server : servers) {
			String sharding = curatorFrameworkOp.getData(JobNodePath.getServerNodePath(jobName, server, "sharding"));
			String toFind = "";
			if (Strings.isNullOrEmpty(sharding)) {
				continue;
			}
			for (String itemKey : Splitter.on(',').split(sharding)) {
				if (item.equals(itemKey)) {
					toFind = itemKey;
					break;
				}
			}
			if (!Strings.isNullOrEmpty(toFind)) {
				runningIp = server;
				break;
			}
		}

		return runningIp;
	}

	@Override
	public String getJobType(String jobName) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		return curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "jobType"));
	}


	/**
	 * 查找所有$SaturnExecutors下online的executor加上preferList配置中被删除的executor
	 *
	 * @param jobName 作业名称
	 * @return 所有executors服务器列表:executorName(ip)
	 */
	public String getAllExecutors(String jobName) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		String executorsNodePath = SaturnExecutorsNode.getExecutorsNodePath();
		if(!curatorFrameworkOp.checkExists(executorsNodePath)) {
			return null;
		}
		StringBuilder allExecutorsBuilder = new StringBuilder();
		StringBuilder offlineExecutorsBuilder = new StringBuilder();
		List<String> executors = curatorFrameworkOp.getChildren(executorsNodePath);
		if(executors != null && executors.size()>0){
			for (String executor : executors) {
				String ip = curatorFrameworkOp.getData(SaturnExecutorsNode.getExecutorIpNodePath(executor));
				if(StringUtils.isNotBlank(ip)){// if ip exists, means the executor is online
					allExecutorsBuilder.append(executor+"("+ip+")").append(",");
					continue;
				}
				offlineExecutorsBuilder.append(executor+"(该executor已离线)").append(",");// if ip is not exists,means the executor is offline
			}
		}
		allExecutorsBuilder.append(offlineExecutorsBuilder.toString());
		String preferListNodePath = JobNodePath.getConfigNodePath(jobName, "preferList");
		if(curatorFrameworkOp.checkExists(preferListNodePath)) {
			String preferList = curatorFrameworkOp.getData(preferListNodePath);
			if(!Strings.isNullOrEmpty(preferList)){
				String[] preferExecutorList = preferList.split(",");
				for(String preferExecutor : preferExecutorList){
					if(executors != null && !executors.contains(preferExecutor)){
						allExecutorsBuilder.append(preferExecutor + "(该executor已删除)").append(",");
					}
				}
			}
		}
		return allExecutorsBuilder.toString();
	}

	@Override
	public JobMigrateInfo getJobMigrateInfo(String jobName) throws SaturnJobConsoleException {
		return null;
	}

	@Override
	public void migrateJobNewTask(String jobName, String taskNew) throws SaturnJobConsoleException {

	}

	@Override
	public boolean isJobEnabled(String jobName) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		return Boolean.valueOf(curatorFrameworkOp.getData(JobNodePath.getConfigNodePath(jobName, "enabled")));
	}


	/**
	 * <blockquote><pre>
	 * 检查否是新版本的executor(新的域)
	 * 旧域：该域下必须至少有一个executor并且所有的executor都没有版本号version节点
	 * 新域：该域下必须至少有一个executor并且所有的executor都有版本号version节点(新版本的executor才在启动时添加了这个节点)
	 * 未知域：该域下没有任何executor或executor中既有新版的又有旧版的Executor
	 *
	 * @param version 指定的executor的版本
	 * @return 当version参数为空时：1：新域 0：旧域 -1：未知域(无法判断新旧域)
	 *         当version参数不为空时，说明要判断是否大于该版本，仅适用于1.1.0及其之后的版本比较：
	 *         	 2：该域下所有Executor的版本都大于等于指定的版本
     *        	-2：该域下所有Executor的版本都小于指定的版本
     *         	-3：Executor的版本存在大于、等于或小于指定的版本
     * </pre></blockquote>
	 */
	public int isNewSaturn(String version) {
		CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.inSessionClient();
		if(!curatorFrameworkOp.checkExists(ExecutorNodePath.getExecutorNodePath())){
			return -1;
		}
		List<String> executors = curatorFrameworkOp.getChildren(ExecutorNodePath.getExecutorNodePath());
		if(executors == null || executors.size() == 0){
			return -1;
		}
		int oldExecutorSize = 0;
		int newExecutorSize = 0;
		int lessThanExecutorSize = 0;
		int moreThanExecutorSize = 0;
		boolean isCompareVersion = !Strings.isNullOrEmpty(version);
		for(String executor : executors){
			String executorVersionPath = ExecutorNodePath.getExecutorNodePath(executor, "version");
			if (!curatorFrameworkOp.checkExists(executorVersionPath)) {
				++oldExecutorSize;
				continue;
			}
			++newExecutorSize;
			if(isCompareVersion){// 1.1.0及之后的版本比较，1.1.0及其以后的executor才有version节点
				String executorVersion = curatorFrameworkOp.getData(executorVersionPath);
				try{
					if(Strings.isNullOrEmpty(executorVersion)){
						++lessThanExecutorSize;// 如果取到的版本号为空串，默认认为是比当前指定版本要低
						continue;
					}
					int compareResult = compareVersion(executorVersion,version);
					if(compareResult < 0){// 比指定版本小
						++lessThanExecutorSize;
						continue;
					}
					++moreThanExecutorSize;// 大于等于指定版本
				}catch(NumberFormatException e){
					++lessThanExecutorSize;// 如果遇到非数字（非1.1.x）的版本号，如saturn-dev，默认认为是比当前指定版本要低
				}
			}
		}
		int executorSize = executors.size();
		if(oldExecutorSize == executorSize){// 先判断如果是全是旧版本的话直接返回
			return 0;
		}
		if(isCompareVersion){// 新版本才存在需要比较版本号的情况
			if(lessThanExecutorSize > 0 && moreThanExecutorSize > 0){
				return -3;
			}
			if(lessThanExecutorSize == executorSize){
				return -2;
			}
			if(moreThanExecutorSize == executorSize){
				return 2;
			}
			return -1;// 该域下的executor有些有version节点，有些没有version节点，无法判断
		}
		if(newExecutorSize == executorSize){
			return 1;
		}
		return -1;
	}

	/**
	 * 比较两个executor的版本，以“.”分割成数组，从第一个数开始逐一比较
	 * 
	 * <p>Examples:
     * <blockquote><pre>
     *     1.0.1 < 1.1.0
	 *     1.0.1 < 1.0.10
	 *     1.0.9 < 1.0.10
	 *     1.0.1 = 1.0.1
	 *     2.0.0 > 1.1.9
	 *     1.0.1.0 > 1.0.0.10
	 * </blockquote></pre>
	 * 
	 * @param version1 executor1的版本
	 * @param version2 executor2的版本
	 * @return 1:version1的版本大于version2的版本
	 *         0:version1的版本等于version2的版本
	 *         -1:version1的版本小于version2的版本
	 */
	private int compareVersion(String version1, String version2) throws NumberFormatException{
		String[] version1Arr = version1.split("\\.");
		String[] version2Arr = version2.split("\\.");
		int versionLength = Math.min(version1Arr.length, version2Arr.length);
		for(int i=0;i<versionLength;i++){
			int v1 = Integer.parseInt(version1Arr[i]);
			int v2 = Integer.parseInt(version2Arr[i]);
			if(v1 > v2){// 只要比较到某一位v1大于v2，就认为version1比version2大，如1.1.0和1.0.1.1的第二位就可以看出1.1.0>1.0.1.1
				return 1;
			}
			if(v1 < v2){
				return -1;
			}
		}
		if(version1Arr.length == version2Arr.length){// 1.0.1 = 1.0.1
			return 0;
		}
		if(version1Arr.length > version2Arr.length){// 1.0.0.1 > 1.0.0
			return 1;
		}
		return -1;// 1.0.0 < 1.0.0.1
	}
}
