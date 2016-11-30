package com.vip.saturn.job.shell;

import java.lang.reflect.Field;

import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.Watchdog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.SaturnConstant;
import com.vip.saturn.job.utils.ScriptPidUtils;

/**
 * Saturn的ExecuteWatchdog
 * 
 * @author linzhaoming
 *
 */
public class SaturnExecuteWatchdog extends ExecuteWatchdog {
	static Logger log = LoggerFactory.getLogger(SaturnExecuteWatchdog.class);

	private Process monitoringProcess;

	private String jobName;

	private int jobItem;

	private String execParam;

	private boolean hasKilledProcess;

	private long pid = -1;

	private String executorName;

	/**
	 * Creates a new watchdog with a given timeout.
	 * 
	 * @param timeout the timeout for the process in milliseconds. It must be greater than 0 or 'INFINITE_TIMEOUT'
	 * @param jobName Job Name
	 * @param jobItem Job Item
	 * @param execParam Exec Param
	 */
	public SaturnExecuteWatchdog(final long timeout, final String jobName, final int jobItem, final String execParam) {
		super(timeout);
		this.jobName = jobName;
		this.jobItem = jobItem;
		this.execParam = execParam;
	}

	public Process getMonitoringProcess() {
		return monitoringProcess;
	}


	public String getJobName() {
		return jobName;
	}

	public synchronized int getJobItem() {
		return jobItem;
	}

	public synchronized void setJobItem(int jobItem) {
		this.jobItem = jobItem;
	}

	public synchronized String getExecParam() {
		return execParam;
	}

	public synchronized void setExecParam(String execParam) {
		this.execParam = execParam;
	}

	public synchronized void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	/**
	 * Watches the given process and terminates it, if it runs for too long. All information from the previous run are
	 * reset.
	 * 
	 * @param processToMonitor the process to monitor. It cannot be {@code null}
	 * @throws IllegalStateException if a process is still being monitored.
	 */
	public synchronized void start(final Process processToMonitor) {
		super.start(processToMonitor);
		this.monitoringProcess = processToMonitor;

		// 保存Pid文件文件
		pid = getPidByProcess(monitoringProcess);
		if (pid > 0) {
			ScriptPidUtils.writePidToFile(executorName, jobName, jobItem, pid);
		}
	}

	/**
	 * Called after watchdog has finished.
	 */
	public synchronized void timeoutOccured(final Watchdog w) {
		try {
			try {
				if (monitoringProcess != null) {
					monitoringProcess.exitValue();
				}
			} catch (final IllegalThreadStateException itse) {// NOSONAR
				if (isWatching()) {
					hasKilledProcess = true;
					if (pid != -1) {
						try {
							ScriptPidUtils.killAllChildrenByPid(pid, false);
						} catch (InterruptedException e) {
							log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, e.getMessage()), e);
						}
					}
				}
			}
		} catch (final Exception e) {
			log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, e.getMessage()), e);
		} finally {
			cleanUp();
		}
	}

	public synchronized boolean killedProcess() {
		return hasKilledProcess;
	}

	public Long getProcessId() {
		return pid;
	}

	/**
	 * @param p 执行Process
	 * @return 执行的脚本进程ID
	 */
	public static long getPidByProcess(Process p) {
		if (!OS.isFamilyUnix()) {
			return -1;
		}

		try {
			String clsName = "java.lang.UNIXProcess";
			Class<?> cls = Class.forName(clsName);
			Field field = cls.getDeclaredField("pid");
			field.setAccessible(true);
			Object pid = field.get(p);
			log.debug("Get Process Id: {}", pid);
			return Long.valueOf(pid.toString());
		} catch (Exception e) {
			log.error("msg=Getting pid error: {}", e.getMessage(), e);
			return Long.valueOf(-1);
		}
	}
}
