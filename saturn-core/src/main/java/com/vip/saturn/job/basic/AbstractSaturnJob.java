package com.vip.saturn.job.basic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.PropertyPlaceholderHelper;

import com.google.common.base.Strings;
import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.SaturnSystemErrorGroup;
import com.vip.saturn.job.SaturnSystemReturnCode;
import com.vip.saturn.job.exception.SaturnJobException;
import com.vip.saturn.job.internal.statistics.ProcessCountStatistics;

/**
 * Saturn抽象父类
 * @author linzhaoming
 */
public abstract class AbstractSaturnJob extends AbstractElasticJob {
	private static Logger log = LoggerFactory.getLogger(AbstractSaturnJob.class);

	protected static PropertyPlaceholderHelper placeHolderHelper = new PropertyPlaceholderHelper("{", "}");
	
    @Override
    protected final void executeJob(final JobExecutionMultipleShardingContext shardingContext) {
		if (! (shardingContext instanceof SaturnExecutionContext)) {
			log.error("[{}] msg=!!! The context must be instance of SaturnJobExecutionContext !!!", jobName);
			return;
		}
		long start = System.currentTimeMillis();
		
		SaturnExecutionContext saturnContext = (SaturnExecutionContext) shardingContext;
		saturnContext.setSaturnJob(true);
		
		
		//begin
		Map<Integer, SaturnJobReturn> retMap = new HashMap<Integer, SaturnJobReturn>();

		// shardingItemParameters为参数表解析出来的Key/Value值
		Map<Integer, String> shardingItemParameters = shardingContext.getShardingItemParameters();

		// items为需要处理的作业分片
		List<Integer> items = shardingContext.getShardingItems();

		log.info("[{}] msg=Job {} handle items: {}", jobName, jobName, items);

		for (Integer item : items) {
			// 兼容配置错误，如配置3个分片, 参数表配置为0=*, 2=*, 则1分片不会执行
			if (!shardingItemParameters.containsKey(item)) {
				log.error("The {} item's parameter is not valid, will not execute the business code, please check shardingItemParameters", item);
				SaturnJobReturn errRet = new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL, 
						"Config of parameter is not valid, check shardingItemParameters", SaturnSystemErrorGroup.FAIL);
				retMap.put(item, errRet);
			}
		}

		Map<Integer, SaturnJobReturn> handleJobMap = handleJob(saturnContext);
		if (handleJobMap != null) {
			retMap.putAll(handleJobMap);
		}
		//end
		
		// 汇总修改
		if (retMap.size() > 0) {
			for (int item : saturnContext.getShardingItems()) {
				updateExecuteResult(retMap.get(item), saturnContext, item);
			}
		}
		long end = System.currentTimeMillis();
		log.info("[{}] msg={} finished, totalCost={}, return={}", jobName, jobName, (end - start), retMap);
	}
	
	protected void updateExecuteResult(SaturnJobReturn saturnJobReturn, SaturnExecutionContext saturnContext, int item) {
		int successCount = 0;
		int errorCount = 0;
		SaturnJobReturn jobReturn = saturnJobReturn;
		if (jobReturn == null) {
			jobReturn = new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL, "Can not find the corresponding SaturnJobReturn", SaturnSystemErrorGroup.FAIL);
			errorCount++;
		} else {
			if (SaturnSystemReturnCode.JOB_NO_COUNT != jobReturn.getReturnCode()) {
				int errorGroup = jobReturn.getErrorGroup();
				if (errorGroup == SaturnSystemErrorGroup.SUCCESS) {
					successCount++;
				} else if (errorGroup == SaturnSystemErrorGroup.TIMEOUT) {
					errorCount++;
					onTimeout(item);
				} else {
					errorCount++;
				}
			}
		}
		// 为了展现分片处理失败的状态
		saturnContext.getShardingItemResults().put(item, jobReturn);
		//执行次数加1
		ProcessCountStatistics.increaseTotalCountDelta(executorName, jobName);
		// 只要有出错和失败的分片，就认为是处理失败; 否则认为处理成功
		if (errorCount == 0 && successCount >= 0) {
			ProcessCountStatistics.incrementProcessSuccessCount(executorName, jobName, successCount);
		} else {
			ProcessCountStatistics.increaseErrorCountDelta(executorName, jobName);
			ProcessCountStatistics.incrementProcessFailureCount(executorName, jobName, errorCount);
		}
	}

	
	public Properties parseKV(String path) {
		if (Strings.isNullOrEmpty(path)) {
			return null;
		}
		Properties kv = new Properties();
		String[] paths = path.split(",");
		if (paths != null && paths.length > 0) {
			for (String p : paths) {
				String[] tmps = p.split("=");
				if (tmps != null && tmps.length == 2) {
					kv.put(tmps[0].trim(), tmps[1].trim());
				} else {
					log.error("msg=Param is not valid {}", p);
				}
			}
		}

		return kv;
	}
	
	
	/**
	 * 获取作业超时时间(秒)
	 */
	public int getTimeoutSeconds() {
		return getConfigService().getTimeoutSeconds();
	}
	

	/**
	 * 获取替换后的作业分片执行值
	 * @param jobParameter 作业参数
	 * @param jobValue 作业value
	 * @return 替换后的值
	 */
	protected String getRealItemValue(String jobParameter, String jobValue) {
		// 处理自定义参数
		Properties kvProp = parseKV(jobParameter);
		int kvSize = kvProp != null ? kvProp.size() : 0;
		final String itemVal; // 作业分片的对应值
		if (kvSize > 0) {
			// 有自定义参数, 解析完替换
			itemVal = placeHolderHelper.replacePlaceholders(jobValue, kvProp);
		} else {
			itemVal = jobValue;
		}
		return itemVal.replaceAll("!!", "\"").replaceAll("@@", "=").replaceAll("##", ",");
	}

	public String logBusinessExceptionIfNecessary(String jobName, Exception e) {
		String message = null;
		if(e instanceof ReflectiveOperationException) {
			Throwable cause = e.getCause();
			if(cause != null) {
				message = cause.getMessage();
				log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, message), e);
			}
		}
		log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, e.getMessage()), e);
		if(message == null) {
			message = e.getMessage();
		}
		return message;
	}

	/**
	 * 更改作业cron表达式，请确认作业名是正确的。
	 * @param jobName 作业名
	 * @param cron cron表达式
	 * @param customContext 自定义上下文
	 * @throws SaturnJobException 可能抛的异常有：type为0，表示cron表达式无效；type为1，表示作业名在这个namespace下不存在；type为3，表示customContext内容超出1M；type为4，表示作业名有误，不能为$SaturnExecutors。
	 */
	public void updateJobCron(String jobName, String cron, Map<String, String> customContext) throws SaturnJobException {
		this.getConfigService().updateJobCron(jobName, cron, customContext);
	}
	
	/**
	 * 实际处理逻辑
	 * @param shardingContext 上下文
	 * @return 每个分片返回一个SaturnJobReturn. 若为null，表示执行失败
	 */
	protected abstract Map<Integer, SaturnJobReturn> handleJob(SaturnExecutionContext shardingContext);
	
	public abstract SaturnJobReturn doExecution(String jobName, Integer key, String value,
			SaturnExecutionContext shardingContext, JavaShardingItemCallable callable) throws Throwable;
}
