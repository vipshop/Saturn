package com.vip.saturn.job.basic;

/**
 * @author chembo.huang
 */
public class SaturnConstant {

	public static final String LOG_FORMAT = "[{}] msg={}";

	public static final String LOG_FORMAT_FOR_STRING = "[%s] msg=%s";

	public static final String TIME_ZONE_ID_DEFAULT = "Asia/Shanghai";
	// max datalength 1MB
	public static final int MAX_ZNODE_DATA_LENGTH = 1048576;

	public static final String ERR_MSG_TEMPLATE_INIT_FAIL = "Job [%s] init business instance fail, jobClass=[%s], caused by:%s";

	public static final String ERR_MSG_INVOKE_METHOD_FAIL = "Job [%s] init business instance fail during call method=[%s] of jobClass=[%s], caused by:%s";
}
