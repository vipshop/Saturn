package com.vip.saturn.job.console.utils;

import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Utils for saturn console.
 *
 * @author kfchu
 */
public class SaturnConsoleUtils {

	private static DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

	public static String parseMillisecond2DisplayTime(String longInStr) {
		return parseMillisecond2DisplayTime(longInStr, null);
	}

	public static String parseMillisecond2DisplayTime(String longInStr, TimeZone timezone) {
		if (StringUtils.isBlank(longInStr)) {
			return null;
		}

		return dtf.print(new DateTime(Long.parseLong(longInStr), DateTimeZone.forTimeZone(timezone)));
	}

	/**
	 * 如果存在/config/enabledReport节点，则返回节点的内容； 如果不存在/config/enabledReport节点，如果作业类型是Java或者Shell，则返回true；否则，返回false；
	 */
	public static boolean checkIfJobIsEnabledReport(String jobName, CuratorFrameworkOp curatorFrameworkOp) {
		String enabledReportNodePath = JobNodePath.getEnabledReportNodePath(jobName);

		if (curatorFrameworkOp.checkExists(enabledReportNodePath)) {
			return Boolean.valueOf(curatorFrameworkOp.getData(enabledReportNodePath));
		}

		String jobTypeNodePath = JobNodePath.getConfigNodePath(jobName, "jobType");

		// if enabledReportNodePath不存在, 如果作业类型是JAVA或者Shell，默认上报
		if ("JAVA_JOB".equals(jobTypeNodePath)
				|| "SHELL_JOB".equals(jobTypeNodePath)) {
			return true;
		}

		return false;
	}
}
