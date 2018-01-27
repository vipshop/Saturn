package com.vip.saturn.job.internal.statistics;

import com.vip.saturn.job.basic.JobRegistry;
import com.vip.saturn.job.basic.JobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行统计信息清零的动作
 * 
 * @author linzhaoming
 *
 */
public class ProcessCountResetTask extends TimerTask {
	static Logger log = LoggerFactory.getLogger(ProcessCountResetTask.class);

	private String executorName;

	public ProcessCountResetTask(String executorName) {
		this.executorName = executorName;
	}

	@Override
	public void run() {
		try {
			Map<String, ConcurrentHashMap<String, JobScheduler>> schedulerMap = JobRegistry.getSchedulerMap();
			if (schedulerMap == null) {
				return;
			}

			// 只清零本executor的统计信息数据
			if (schedulerMap.containsKey(executorName)) {
				ConcurrentHashMap<String, JobScheduler> jobSchedulerMap = schedulerMap.get(executorName);
				if (jobSchedulerMap == null) {
					return;
				}

				Iterator<Map.Entry<String, JobScheduler>> iterator = jobSchedulerMap.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, JobScheduler> next = iterator.next();
					String jobName = next.getKey();
					JobScheduler jobScheduler = next.getValue();
					// 清零内存统计值
					ProcessCountStatistics.resetSuccessFailureCount(executorName, jobName);
					// 清零zk统计值
					jobScheduler.getServerService().persistProcessFailureCount(0);
					jobScheduler.getServerService().persistProcessSuccessCount(0);
					log.info("msg={} reset the job {}'s statistics data", executorName, jobName);
				}
			}
		} catch (Throwable t) {
			log.error("process count reset error", t);
		}
	}
}
