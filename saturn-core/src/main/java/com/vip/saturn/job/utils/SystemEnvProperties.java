package com.vip.saturn.job.utils;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SystemEnvProperties {

	private static Logger log = LoggerFactory.getLogger(SystemEnvProperties.class);

	private static final String NAME_VIP_SATURN_MAX_NUMBER_OF_JOBS = "VIP_SATURN_MAX_NUMBER_OF_JOBS";
	/**
	 * 每个域最大作业数量
	 */
	public static int VIP_SATURN_MAX_NUMBER_OF_JOBS = 500;

	private static final String NAME_VIP_SATURN_EXECUTOR_CLEAN = "VIP_SATURN_EXECUTOR_CLEAN";
	/**
	 * Executor离线时，其zk节点信息是否被清理
	 */
	public static boolean VIP_SATURN_EXECUTOR_CLEAN = Boolean.parseBoolean(
			System.getProperty(NAME_VIP_SATURN_EXECUTOR_CLEAN, System.getenv(NAME_VIP_SATURN_EXECUTOR_CLEAN)));

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
	public static final String NAME_VIP_SATURN_OUTPUT_PATH = "VIP_SATURN_OUTPUT_PATH";

	private static final String NAME_VIP_SATURN_DCOS_TASK = "VIP_SATURN_DCOS_TASK";
	private static final String NAME_VIP_SATURN_K8S_DEPLOYMENT = "VIP_SATURN_K8S_DEPLOYMENT";
	public static String VIP_SATURN_CONTAINER_DEPLOYMENT_ID;

	/**
	 * Executor优雅退出的全局默认超时时间（单位：精确到秒，默认1分钟）
	 */
	public static int VIP_SATURN_SHUTDOWN_TIMEOUT = 60;
	public static int VIP_SATURN_SHUTDOWN_TIMEOUT_MAX = 5 * 60 - 10;

	/**
	 * Saturn Console URI.
	 */
	public static final String NAME_VIP_SATURN_CONSOLE_URI = "VIP_SATURN_CONSOLE_URI";
	public static String VIP_SATURN_CONSOLE_URI = trim(
			System.getProperty(NAME_VIP_SATURN_CONSOLE_URI, System.getenv(NAME_VIP_SATURN_CONSOLE_URI)));
	public static List<String> VIP_SATURN_CONSOLE_URI_LIST = new ArrayList<>();

	private static final String NAME_VIP_SATURN_SHUTDOWN_TIMEOUT = "VIP_SATURN_SHUTDOWN_TIMEOUT";

	/**
	 * Executor ZK Client session timeout
	 */
	public static final String NAME_VIP_SATURN_ZK_CLIENT_SESSION_TIMEOUT_IN_SECONDS = "VIP_SATURN_ZK_CLIENT_SESSION_TIMEOUT";
	public static int VIP_SATURN_ZK_CLIENT_SESSION_TIMEOUT_IN_SECONDS = -1;

	/**
	 * Executor ZK Client connection timeout
	 */
	public static final String NAME_VIP_SATURN_ZK_CLIENT_CONNECTION_TIMEOUT_IN_SECONDS = "VIP_SATURN_ZK_CLIENT_CONNECTION_TIMEOUT";
	public static int VIP_SATURN_ZK_CLIENT_CONNECTION_TIMEOUT_IN_SECONDS = -1;

	/**
	 * Executor ZK Client retry times
	 */
	public static final String NAME_VIP_SATURN_ZK_CLIENT_RETRY_TIMES = "VIP_SATURN_ZK_CLIENT_RETRY_TIMES";
	public static int VIP_SATURN_ZK_CLIENT_RETRY_TIMES = -1;


	private static final String NAME_VIP_SATURN_USE_UNSTABLE_NETWORK_SETTING = "VIP_SATURN_USE_UNSTABLE_NETWORK_SETTING";
	public static boolean VIP_SATURN_USE_UNSTABLE_NETWORK_SETTING = false;

	// For restart and dump
	public static boolean VIP_SATURN_ENABLE_EXEC_SCRIPT = Boolean.getBoolean("VIP_SATURN_ENABLE_EXEC_SCRIPT");
	public static final String NAME_VIP_SATURN_PRG = "VIP_SATURN_PRG";
	public static String VIP_SATURN_PRG = System.getProperty(NAME_VIP_SATURN_PRG);
	public static final String NAME_VIP_SATURN_LOG_OUTFILE = "VIP_SATURN_LOG_OUTFILE";
	public static String VIP_SATURN_LOG_OUTFILE = System.getProperty(NAME_VIP_SATURN_LOG_OUTFILE);

	// nohup file size checking by default is 600 seconds
	public static int VIP_SATURN_CHECK_NOHUPOUT_SIZE_INTERVAL_IN_SEC = 600;
	private static final String NAME_VIP_SATURN_CHECK_NOHUPOUT_SIZE_INTERVAL_IN_SEC = "VIP_SATURN_CHECK_NOHUPOUT_SIZE_INTERVAL";

	// nohup max file size by default is 500 MB
	public static long VIP_SATURN_NOHUPOUT_SIZE_LIMIT_IN_BYTES = 500 * 1024 * 1024L;
	private static final String NAME_VIP_SATURN_NOHUPOUT_SIZE_LIMIT_IN_BYTES = "VIP_SATURN_NOHUPOUT_SIZE_LIMIT";

	public static int VIP_SATURN_SESSION_TIMEOUT_IN_SECONDS_IN_UNSTABLE_NETWORK = 40;
	public static int VIP_SATURN_CONNECTION_TIMEOUT_IN_SECONDS_IN_UNSTABLE_NETWORK = 40;
	public static int VIP_SATURN_RETRY_TIMES_IN_UNSTABLE_NETWORK = 9;

	static {
		loadProperties();
	}

	public static void loadProperties() {
		String maxNumberOfJobs = System
				.getProperty(NAME_VIP_SATURN_MAX_NUMBER_OF_JOBS, System.getenv(NAME_VIP_SATURN_MAX_NUMBER_OF_JOBS));
		if (!Strings.isNullOrEmpty(maxNumberOfJobs)) {
			try {
				VIP_SATURN_MAX_NUMBER_OF_JOBS = Integer.parseInt(maxNumberOfJobs);
			} catch (Throwable t) {
				log.error("msg=" + t.getMessage(), t);
			}
		}

		String shutdownTimeout = System
				.getProperty(NAME_VIP_SATURN_SHUTDOWN_TIMEOUT, System.getenv(NAME_VIP_SATURN_SHUTDOWN_TIMEOUT));
		if (!Strings.isNullOrEmpty(shutdownTimeout)) {
			try {
				VIP_SATURN_SHUTDOWN_TIMEOUT = Integer.parseInt(shutdownTimeout);
			} catch (Throwable t) {
				log.error("msg=" + t.getMessage(), t);
			}
		}
		if (VIP_SATURN_SHUTDOWN_TIMEOUT > VIP_SATURN_SHUTDOWN_TIMEOUT_MAX) {
			VIP_SATURN_SHUTDOWN_TIMEOUT = VIP_SATURN_SHUTDOWN_TIMEOUT_MAX;
		}

		String dcosTaskId = System.getProperty(NAME_VIP_SATURN_DCOS_TASK, System.getenv(NAME_VIP_SATURN_DCOS_TASK));
		if (StringUtils.isNotBlank(dcosTaskId)) {
			VIP_SATURN_CONTAINER_DEPLOYMENT_ID = dcosTaskId;
		} else {
			VIP_SATURN_CONTAINER_DEPLOYMENT_ID = System
					.getProperty(NAME_VIP_SATURN_K8S_DEPLOYMENT, System.getenv(NAME_VIP_SATURN_K8S_DEPLOYMENT));
		}

		String zkClientSessionTimeoutStr = System.getProperty(NAME_VIP_SATURN_ZK_CLIENT_SESSION_TIMEOUT_IN_SECONDS,
				System.getenv(NAME_VIP_SATURN_ZK_CLIENT_SESSION_TIMEOUT_IN_SECONDS));
		if (!Strings.isNullOrEmpty(zkClientSessionTimeoutStr)) {
			try {
				VIP_SATURN_ZK_CLIENT_SESSION_TIMEOUT_IN_SECONDS = Integer.parseInt(zkClientSessionTimeoutStr);
			} catch (Throwable t) {
				log.error("msg=" + t.getMessage(), t);
			}
		}

		String zkClientConnectionTimeoutStr = System
				.getProperty(NAME_VIP_SATURN_ZK_CLIENT_CONNECTION_TIMEOUT_IN_SECONDS,
				System.getenv(NAME_VIP_SATURN_ZK_CLIENT_CONNECTION_TIMEOUT_IN_SECONDS));
		if (!Strings.isNullOrEmpty(zkClientConnectionTimeoutStr)) {
			try {
				VIP_SATURN_ZK_CLIENT_CONNECTION_TIMEOUT_IN_SECONDS = Integer.parseInt(zkClientConnectionTimeoutStr);
			} catch (Throwable t) {
				log.error("msg=" + t.getMessage(), t);
			}
		}

		String zkClientRetryTimes = System.getProperty(NAME_VIP_SATURN_ZK_CLIENT_RETRY_TIMES, System.getenv(NAME_VIP_SATURN_ZK_CLIENT_RETRY_TIMES));
		if (!Strings.isNullOrEmpty(zkClientRetryTimes)) {
			try {
				VIP_SATURN_ZK_CLIENT_RETRY_TIMES = Integer.parseInt(zkClientRetryTimes);
			} catch (Throwable t) {
				log.error("msg=" + t.getMessage(), t);
			}
		}

		String useUnstableNetworkSettings = System.getProperty(NAME_VIP_SATURN_USE_UNSTABLE_NETWORK_SETTING,
				System.getenv(NAME_VIP_SATURN_USE_UNSTABLE_NETWORK_SETTING));
		if (!Strings.isNullOrEmpty(useUnstableNetworkSettings)) {
			VIP_SATURN_USE_UNSTABLE_NETWORK_SETTING = Boolean.parseBoolean(useUnstableNetworkSettings);
		}

		String checkNohupOutSizeInterval = System.getProperty(NAME_VIP_SATURN_CHECK_NOHUPOUT_SIZE_INTERVAL_IN_SEC,
				System.getenv(NAME_VIP_SATURN_CHECK_NOHUPOUT_SIZE_INTERVAL_IN_SEC));
		if (!Strings.isNullOrEmpty(checkNohupOutSizeInterval)) {
			try {
				int interval_in_sec = Integer.parseInt(checkNohupOutSizeInterval);
				if (interval_in_sec > 0) {
					VIP_SATURN_CHECK_NOHUPOUT_SIZE_INTERVAL_IN_SEC = interval_in_sec;
				}
			} catch (Throwable t) {
				log.error("msg=" + t.getMessage(), t);
			}
		}

		String noHupOutSizeLimit = System.getProperty(NAME_VIP_SATURN_NOHUPOUT_SIZE_LIMIT_IN_BYTES,
				System.getenv(NAME_VIP_SATURN_NOHUPOUT_SIZE_LIMIT_IN_BYTES));
		if (!Strings.isNullOrEmpty(noHupOutSizeLimit)) {
			try {
				long sizeLimit = Long.parseLong(noHupOutSizeLimit);
				if (sizeLimit > 0) {
					VIP_SATURN_NOHUPOUT_SIZE_LIMIT_IN_BYTES = sizeLimit;
				}
			} catch (Throwable t) {
				log.error("msg=" + t.getMessage(), t);
			}
		}
	}

	protected static String trim(String property) {
		if (property != null && property.length() > 0) {
			return property.trim();
		}
		return property;
	}

	public static void init() {
		if (VIP_SATURN_CONSOLE_URI != null) {
			String[] split = VIP_SATURN_CONSOLE_URI.split(",");
			if (split != null) {
				for (String tmp : split) {
					tmp = tmp.trim();
					if (!tmp.isEmpty()) {
						VIP_SATURN_CONSOLE_URI_LIST.add(tmp);
					}
				}
			}
		}
	}

}
