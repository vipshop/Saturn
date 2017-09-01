/**
 * 
 */
package com.vip.saturn.job.executor;

/**
 * @author chembo.huang
 *
 */
public class SaturnExecutorsNode {

	public static final String EXECUTOR_NODE_NAME = "$SaturnExecutors";

	public static final String SATURN_EXECUTORS_EXECUTORS_NODE_NAME = String.format("/%s/%s", EXECUTOR_NODE_NAME,
			"executors");

	public static final String EXECUTORS_ROOT = "/" + EXECUTOR_NODE_NAME + "/executors";

}
