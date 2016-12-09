package com.vip.saturn.job.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class SystemEnvProperties {
	static Logger log = LoggerFactory.getLogger(SystemEnvProperties.class);

	private static String NAME_VIP_SATURN_ZK_CONNECTION = "VIP_SATURN_ZK_CONNECTION";	
	/**
	 * ZK连接串
	 */
	public static String VIP_SATURN_ZK_CONNECTION = trim(System.getProperty(SystemEnvProperties.NAME_VIP_SATURN_ZK_CONNECTION, System.getenv(SystemEnvProperties.NAME_VIP_SATURN_ZK_CONNECTION)));

	private static String NAME_VIP_SATURN_IS_CRON_JOB_LOG = "VIP_SATURN_IS_CRON_LOG";
	/**
	 * 是否记录定时作业(Java/Shell)实时日志到ZK: 默认打开
	 */
	public static boolean VIP_SATURN_IS_CRON_JOB_LOG = true;
	

	private static String NAME_VIP_SATURN_MAX_NUMBER_OF_JOBS = "VIP_SATURN_MAX_NUMBER_OF_JOBS";	
	/**
	 * 每个域最大作业数量
	 */
	public static int VIP_SATURN_MAX_NUMBER_OF_JOBS = 500;

	private static String NAME_VIP_SATURN_EXECUTOR_CLEAN = "VIP_SATURN_EXECUTOR_CLEAN";
	/**
	 * Executor离线时，其zk节点信息是否被清理
	 */
	public static boolean VIP_SATURN_EXECUTOR_CLEAN = Boolean.parseBoolean(System.getProperty(NAME_VIP_SATURN_EXECUTOR_CLEAN, System.getenv(NAME_VIP_SATURN_EXECUTOR_CLEAN)));

	/**
	 * <pre>
	 * shell作业的结果回写的文件全路径（如果需要返回一些执行结果，只需要将结果写入该文件），JSON结构:
	 *  {
	 *    returnMsg: 返回消息内容
	 *    errorGroup: 200=SUCCESS, 500/550: error
	 *    returnCode: 自定义返回码,
	 *    prop: {k:v} 属性对
	 *  }
	 * </pre>
	 */
	public static String NAME_VIP_SATURN_OUTPUT_PATH = "VIP_SATURN_OUTPUT_PATH";

	private static String NAME_VIP_SATURN_DCOS_TASK = "VIP_SATURN_DCOS_TASK";
	public static String VIP_SATURN_DCOS_TASK = System.getProperty(NAME_VIP_SATURN_DCOS_TASK, System.getenv(NAME_VIP_SATURN_DCOS_TASK));
	
	/**
	 * Executor优雅退出的全局默认超时时间（单位：精确到秒，默认1分钟）
	 */
	public static int VIP_SATURN_SHUTDOWN_TIMEOUT = 60;
	public static int VIP_SATURN_SHUTDOWN_TIMEOUT_MAX = 5*60 - 10;

	private static String NAME_VIP_SATURN_SHUTDOWN_TIMEOUT = "VIP_SATURN_SHUTDOWN_TIMEOUT";

	static {
		
		//定时任务(Java/Shell)实时日志开关
		String isCronLog = System.getProperty(NAME_VIP_SATURN_IS_CRON_JOB_LOG, System.getenv(NAME_VIP_SATURN_IS_CRON_JOB_LOG));
		if (!Strings.isNullOrEmpty(isCronLog)) {
			VIP_SATURN_IS_CRON_JOB_LOG = Boolean.valueOf(isCronLog);
		}

		if (VIP_SATURN_IS_CRON_JOB_LOG) {
			log.warn("Cron Log is opened.");
		} else {
			log.warn("Cron Log is closed.");
		}
		
		
		String maxNumberOfJobs = System.getProperty(NAME_VIP_SATURN_MAX_NUMBER_OF_JOBS, System.getenv(NAME_VIP_SATURN_MAX_NUMBER_OF_JOBS));
		if(!Strings.isNullOrEmpty(maxNumberOfJobs)) {
			try {
				VIP_SATURN_MAX_NUMBER_OF_JOBS = Integer.valueOf(maxNumberOfJobs);
			} catch (Throwable t) {
				log.error("msg=" + t.getMessage(), t);
			}
		}
		
		String shutdownTimeout = System.getProperty(NAME_VIP_SATURN_SHUTDOWN_TIMEOUT, System.getenv(NAME_VIP_SATURN_SHUTDOWN_TIMEOUT));
		if(!Strings.isNullOrEmpty(shutdownTimeout)) {
			try {
				VIP_SATURN_SHUTDOWN_TIMEOUT = Integer.valueOf(shutdownTimeout);
			} catch (Throwable t) {
				log.error("msg=" + t.getMessage(), t);
			}
		}
		if(VIP_SATURN_SHUTDOWN_TIMEOUT > VIP_SATURN_SHUTDOWN_TIMEOUT_MAX){
			VIP_SATURN_SHUTDOWN_TIMEOUT = VIP_SATURN_SHUTDOWN_TIMEOUT_MAX;
		}
	}

	
	protected static String trim(String property) {
		if (property != null && property.length()>0) {
			return property.trim();
		}
		return property;
	}
}
