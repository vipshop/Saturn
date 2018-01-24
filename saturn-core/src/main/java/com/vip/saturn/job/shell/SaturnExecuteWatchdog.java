package com.vip.saturn.job.shell;

import com.vip.saturn.job.basic.SaturnConstant;
import com.vip.saturn.job.utils.ScriptPidUtils;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.Watchdog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Watchdog for shell job process, provide this function that kill process when timeout or forceStop.
 *
 * @author linzhaoming
 * @author hebelala
 */
public class SaturnExecuteWatchdog extends ExecuteWatchdog {

	private static final int INIT = 0;

	private static final int TIMEOUT = 1;

	private static final int FORCE_STOP = 2;

	private static final Logger log = LoggerFactory.getLogger(SaturnExecuteWatchdog.class);

	private String jobName;

	private int jobItem;

	private String execParam;

	private String executorName;

	private long pid = -1;

	private AtomicInteger status = new AtomicInteger(INIT);

	public SaturnExecuteWatchdog(final long timeout, final String jobName, final int jobItem, final String execParam,
			final String executorName) {
		super(timeout);
		this.jobName = jobName;
		this.jobItem = jobItem;
		this.execParam = execParam;
		this.executorName = executorName;
	}

	public String getJobName() {
		return jobName;
	}

	public int getJobItem() {
		return jobItem;
	}

	public String getExecParam() {
		return execParam;
	}

	public long getPid() {
		return pid;
	}

	public String getExecutorName() {
		return executorName;
	}

	public boolean isTimeout() {
		return status.get() == TIMEOUT;
	}

	public boolean isForceStop() {
		return status.get() == FORCE_STOP;
	}

	@Override
	public synchronized void start(final Process processToMonitor) {
		super.start(processToMonitor);

		// get and save pid to file
		pid = getPidByProcess(processToMonitor);
		if (pid != -1) {
			ScriptPidUtils.writePidToFile(executorName, jobName, jobItem, pid);
		}
	}

	/**
	 * Set status to forceStop. Destroy the running process and stop watchdog. At last, use kill, not kill -9.
	 */
	@Override
	public synchronized void destroyProcess() {
		status.compareAndSet(INIT, FORCE_STOP);
		super.destroyProcess();
	}

	@Override
	public synchronized void timeoutOccured(final Watchdog w) {
		status.compareAndSet(INIT, TIMEOUT);
		super.timeoutOccured(w);
		// if the process is still running, use kill command to destroy it.
		if (pid != -1) {
			try {
				ScriptPidUtils.killAllChildrenByPid(pid, false);
			} catch (Exception e) {
				log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, e.getMessage()), e);
			}
		}
	}

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
			return Long.parseLong(pid.toString());
		} catch (Exception e) {
			log.error("msg=Getting pid error: {}", e.getMessage(), e);
			return -1;
		}
	}
}
