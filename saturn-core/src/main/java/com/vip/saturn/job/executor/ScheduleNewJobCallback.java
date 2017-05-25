package com.vip.saturn.job.executor;

/**
 * 
 * @author hebelala
 *
 */
public interface ScheduleNewJobCallback {

	/**
	 * call the initialize job callback, return true if success, return false if fail
	 */
	boolean call(String jobName);
	
}
