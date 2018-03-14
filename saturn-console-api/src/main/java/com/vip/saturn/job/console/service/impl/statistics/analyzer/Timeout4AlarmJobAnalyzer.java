package com.vip.saturn.job.console.service.impl.statistics.analyzer;

import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.Timeout4AlarmJob;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import com.vip.saturn.job.console.service.helper.DashboardServiceHelper;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.integrate.service.ReportAlarmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author timmy.hu
 */
public class Timeout4AlarmJobAnalyzer {

	private static final Logger log = LoggerFactory.getLogger(Timeout4AlarmJobAnalyzer.class);

	private ReportAlarmService reportAlarmService;

	private List<Timeout4AlarmJob> timeout4AlarmJobList = new ArrayList<Timeout4AlarmJob>();

	/**
	 * 查找超时告警作业
	 */
	public void analyze(CuratorFrameworkOp curatorFrameworkOp, List<Timeout4AlarmJob> oldTimeout4AlarmJobs,
			String jobName, String jobDegree, RegistryCenterConfiguration config) {
		Timeout4AlarmJob timeout4AlarmJob = new Timeout4AlarmJob(jobName, config.getNamespace(),
				config.getNameAndNamespace(), config.getDegree());
		if (isTimeout4AlarmJob(timeout4AlarmJob, oldTimeout4AlarmJobs, curatorFrameworkOp)) {
			timeout4AlarmJob.setJobDegree(jobDegree);
			addTimeout4AlarmJob(timeout4AlarmJob);
		}
	}

	private synchronized void addTimeout4AlarmJob(Timeout4AlarmJob timeout4AlarmJob) {
		timeout4AlarmJobList.add(timeout4AlarmJob);
	}

	/**
	 * 如果配置了超时告警时间，而且running节点存在时间大于它，则告警
	 */
	private boolean isTimeout4AlarmJob(Timeout4AlarmJob timeout4AlarmJob, List<Timeout4AlarmJob> oldTimeout4AlarmJobs,
			CuratorFrameworkOp curatorFrameworkOp) {
		String jobName = timeout4AlarmJob.getJobName();
		int timeout4AlarmSeconds = getTimeout4AlarmSeconds(curatorFrameworkOp, jobName);
		if (timeout4AlarmSeconds <= 0) {
			return false;
		}
		List<String> items = curatorFrameworkOp.getChildren(JobNodePath.getExecutionNodePath(jobName));
		if (items == null || items.isEmpty()) {
			return false;
		}
		computeTimeoutItems(timeout4AlarmJob, curatorFrameworkOp, jobName, timeout4AlarmSeconds, items);
		if (!timeout4AlarmJob.getTimeoutItems().isEmpty()) {
			Timeout4AlarmJob oldJob = DashboardServiceHelper.findEqualTimeout4AlarmJob(timeout4AlarmJob,
					oldTimeout4AlarmJobs);
			if (oldJob != null) {
				timeout4AlarmJob.setRead(oldJob.isRead());
				if (oldJob.getUuid() != null) {
					timeout4AlarmJob.setUuid(oldJob.getUuid());
				} else {
					timeout4AlarmJob.setUuid(UUID.randomUUID().toString());
				}
			} else {
				timeout4AlarmJob.setUuid(UUID.randomUUID().toString());
			}
			if (!timeout4AlarmJob.isRead()) {
				try {
					reportAlarmService.dashboardTimeout4AlarmJob(timeout4AlarmJob.getDomainName(), jobName,
							timeout4AlarmJob.getTimeoutItems(), timeout4AlarmSeconds);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
			return true;
		}
		return false;
	}

	private void computeTimeoutItems(Timeout4AlarmJob timeout4AlarmJob, CuratorFrameworkOp curatorFrameworkOp,
			String jobName, int timeout4AlarmSeconds, List<String> items) {
		long timeout4AlarmMills = timeout4AlarmSeconds * 1L * 1000;
		timeout4AlarmJob.setTimeout4AlarmSeconds(timeout4AlarmSeconds);
		for (String itemStr : items) {
			long ctime = curatorFrameworkOp.getCtime(JobNodePath.getExecutionNodePath(jobName, itemStr, "running"));
			if (ctime > 0 && System.currentTimeMillis() - ctime > timeout4AlarmMills) {
				timeout4AlarmJob.getTimeoutItems().add(Integer.valueOf(itemStr));
			}
		}
	}

	private int getTimeout4AlarmSeconds(CuratorFrameworkOp curatorFrameworkOp, String jobName) {
		String timeout4AlarmSecondsStr = curatorFrameworkOp
				.getData(JobNodePath.getConfigNodePath(jobName, "timeout4AlarmSeconds"));
		int timeout4AlarmSeconds = 0;
		if (timeout4AlarmSecondsStr != null) {
			try {
				timeout4AlarmSeconds = Integer.parseInt(timeout4AlarmSecondsStr);
			} catch (NumberFormatException e) {
				log.error(e.getMessage(), e);
			}
		}
		return timeout4AlarmSeconds;
	}

	public List<Timeout4AlarmJob> getTimeout4AlarmJobList() {
		return new ArrayList<Timeout4AlarmJob>(timeout4AlarmJobList);
	}

	public void setReportAlarmService(ReportAlarmService reportAlarmService) {
		this.reportAlarmService = reportAlarmService;
	}
}
