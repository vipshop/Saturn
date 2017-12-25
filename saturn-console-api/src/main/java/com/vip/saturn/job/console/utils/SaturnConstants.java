package com.vip.saturn.job.console.utils;

import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

/**
 * @author hebelala
 */
public class SaturnConstants {

	public static final int HEALTH_CHECK_VERSION_MAX_SIZE = 10000;

	public static final String DEAL_SUCCESS = "ok";

	public static final int JOB_CAN_BE_DELETE_TIME_LIMIT = 2 * 60 * 1000;// 作业可以被删除的时间限制(单位：ms)

	/**
	 * 容器伸缩计划作业名前缀
	 */
	public static final String SYSTEM_SCALE_JOB_PREFEX = "system_scale_";

	/**
	 * 获取sys_config表数据的间隔时间
	 */
	public static final long GET_SYS_CONFIG_DATA_REFRESH_TIME = 1000 * 60 * 10;

	public static final String TIME_ZONE_ID_DEFAULT = "Asia/Shanghai";

	public static final List<String> TIME_ZONE_IDS = Arrays.asList(TimeZone.getAvailableIDs());

	public static final String FILE_SEPARATOR = System.getProperty("file.separator");

	public static final String CACHES_FILE_PATH = System.getProperty("user.home") + FILE_SEPARATOR + ".saturn"
			+ FILE_SEPARATOR + "saturn_console" + FILE_SEPARATOR + "caches";

}
