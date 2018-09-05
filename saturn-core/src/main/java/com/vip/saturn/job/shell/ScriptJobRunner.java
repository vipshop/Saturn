package com.vip.saturn.job.shell;

import com.alibaba.fastjson.JSON;
import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.SaturnSystemErrorGroup;
import com.vip.saturn.job.SaturnSystemReturnCode;
import com.vip.saturn.job.basic.AbstractSaturnJob;
import com.vip.saturn.job.basic.SaturnConstant;
import com.vip.saturn.job.basic.SaturnExecutionContext;
import com.vip.saturn.job.utils.ScriptPidUtils;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ScriptJobRunner {

	private static Logger log = LoggerFactory.getLogger(ScriptJobRunner.class);

	private static final String PREFIX_COMAND = " source /etc/profile; ";

	private Map<String, String> envMap = new HashMap<>();

	private AbstractSaturnJob job;

	private Integer item;

	private String itemValue;

	private SaturnExecutionContext saturnExecutionContext;

	private String jobName;

	private SaturnExecuteWatchdog watchdog;

	private boolean businessReturned = false;

	private File saturnOutputFile;

	public ScriptJobRunner(Map<String, String> envMap, AbstractSaturnJob job, Integer item, String itemValue,
			SaturnExecutionContext saturnExecutionContext) {
		if (envMap != null) {
			this.envMap.putAll(envMap);
		}
		this.job = job;
		this.item = item;
		this.itemValue = itemValue;
		this.saturnExecutionContext = saturnExecutionContext;
		if (job != null) {
			this.jobName = job.getJobName();
		}
	}

	public boolean isBusinessReturned() {
		return businessReturned;
	}

	private void createSaturnJobReturnFile() throws IOException {
		if (envMap.containsKey(SystemEnvProperties.NAME_VIP_SATURN_OUTPUT_PATH)) {
			String saturnOutputPath = envMap.get(SystemEnvProperties.NAME_VIP_SATURN_OUTPUT_PATH);
			saturnOutputFile = new File(saturnOutputPath);
			if (!saturnOutputFile.exists()) {
				FileUtils.forceMkdir(saturnOutputFile.getParentFile());
				if (!saturnOutputFile.createNewFile()) {
					log.warn("file {} already exsits.", saturnOutputPath);
				}
			}
		}
	}

	private CommandLine createCommandLine(Map<String, String> env) {
		StringBuilder envStringBuilder = new StringBuilder();
		if (envMap != null && !envMap.isEmpty()) {
			for (Entry<String, String> envEntrySet : envMap.entrySet()) {
				envStringBuilder.append("export ").append(envEntrySet.getKey()).append('=')
						.append(envEntrySet.getValue()).append(';');
			}
		}
		String execParameter = envStringBuilder.toString() + PREFIX_COMAND
				+ ScriptPidUtils.filterEnvInCmdStr(env, itemValue);
		final CommandLine cmdLine = new CommandLine("/bin/sh");
		cmdLine.addArguments(new String[]{"-c", execParameter}, false);
		return cmdLine;
	}

	private SaturnJobReturn readSaturnJobReturn() {
		SaturnJobReturn tmp = null;
		if (saturnOutputFile != null && saturnOutputFile.exists()) {
			try {
				String fileContents = FileUtils.readFileToString(saturnOutputFile);
				if (fileContents != null && !fileContents.trim().isEmpty()) {
					tmp = JSON.parseObject(fileContents.trim(), SaturnJobReturn.class);
					businessReturned = true; // 脚本成功返回数据
				}
			} catch (Throwable t) {
				log.error("[{" + jobName + "}] msg={" + jobName + "}-{" + item + "} read SaturnJobReturn from {"
						+ saturnOutputFile.getAbsolutePath() + "} error", t);
				tmp = new SaturnJobReturn(SaturnSystemReturnCode.USER_FAIL, "Exception: " + t,
						SaturnSystemErrorGroup.FAIL);
			}
		}
		return tmp;
	}

	public synchronized SaturnExecuteWatchdog getWatchdog() {
		if (watchdog == null) {
			long timeoutSeconds = saturnExecutionContext.getTimetoutSeconds();
			String executorName = job.getExecutorName();
			if (timeoutSeconds > 0) {
				watchdog = new SaturnExecuteWatchdog(timeoutSeconds * 1000, jobName, item, itemValue, executorName);
				log.info("[{}] msg=Job {} enable timeout control : {} s ", jobName, jobName, timeoutSeconds);
			} else { // 需要指定超时值，才会启用watchdog: 强行指定为5年
				watchdog = new SaturnExecuteWatchdog(5L * 365 * 24 * 3600 * 1000, jobName, item, itemValue,
						executorName);
				if (log.isDebugEnabled()) {
					log.debug("[{}] msg=Job {} disable timeout control", jobName, jobName);
				}
			}
		}
		return watchdog;
	}

	public SaturnJobReturn runJob() {
		SaturnJobReturn saturnJobReturn = null;
		long timeoutSeconds = saturnExecutionContext.getTimetoutSeconds();
		try {
			createSaturnJobReturnFile();
			saturnJobReturn = execute(timeoutSeconds);
		} catch (Throwable t) {
			log.error("[{" + jobName + "}] msg={" + jobName + "}-{" + item + "} Exception", t);
			saturnJobReturn = new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL, "Exception: " + t,
					SaturnSystemErrorGroup.FAIL);
		} finally {
			FileUtils.deleteQuietly(saturnOutputFile.getParentFile());
		}

		if (saturnJobReturn.getProp() == null) {
			saturnJobReturn.setProp(new HashMap());
		}
		return saturnJobReturn;
	}

	private SaturnJobReturn execute(long timeoutSeconds) {
		SaturnJobReturn saturnJobReturn;
		ProcessOutputStream processOutputStream = new ProcessOutputStream(1);
		DefaultExecutor executor = new DefaultExecutor();
		PumpStreamHandler streamHandler = new PumpStreamHandler(processOutputStream);
		streamHandler.setStopTimeout(timeoutSeconds * 1000); // 关闭线程等待时间, (注意commons-exec会固定增加2秒的addition)
		executor.setExitValue(0);
		executor.setStreamHandler(streamHandler);
		executor.setWatchdog(getWatchdog());

		// filter env key in execParameter. like cd ${mypath} -> cd /root/my.
		Map<String, String> env = ScriptPidUtils.loadEnv();
		CommandLine commandLine = createCommandLine(env);

		try {
			long start = System.currentTimeMillis();
			log.info("[{}] msg=Begin executing {}-{} {}", jobName, jobName, item, commandLine);
			int exitValue = executor.execute(commandLine, env);
			long end = System.currentTimeMillis();
			log.info("[{}] msg=Finish executing {}-{} {}, the exit value is {}, cost={}ms", jobName, jobName, item,
					commandLine, exitValue, (end - start));

			SaturnJobReturn tmp = readSaturnJobReturn();
			if (tmp == null) {
				tmp = new SaturnJobReturn("the exit value is " + exitValue);
			}
			saturnJobReturn = tmp;
		} catch (Exception e) {
			saturnJobReturn = handleException(timeoutSeconds, e);
		} finally {
			try {
				// 将日志set进jobLog, 写不写zk再由ExecutionService控制
				handleJobLog(processOutputStream.getJobLog());
				processOutputStream.close();
			} catch (Exception ex) {
				log.error("[{}] msg={}-{} Error at closing output stream. Should not be concern: {}", jobName,
						jobName, item, ex);
			}
			stopStreamHandler(streamHandler);
			ScriptPidUtils.removePidFile(job.getExecutorName(), jobName, "" + item, watchdog.getPid());
		}
		return saturnJobReturn;
	}

	private void handleJobLog(String jobLog) {
		// 出于系统保护考虑，jobLog不能超过1M
		if (jobLog != null && jobLog.length() > SaturnConstant.MAX_ZNODE_DATA_LENGTH) {
			jobLog = jobLog.substring(0, SaturnConstant.MAX_ZNODE_DATA_LENGTH);
		}

		saturnExecutionContext.putJobLog(item, jobLog);

		// 提供给saturn-job-executor.log日志输出shell命令jobLog，以后若改为重定向到日志，则可删除此输出
		System.out.println("[" + jobName + "] msg=" + jobName + "-" + item + ":" + jobLog);// NOSONAR

		log.info("[{}] msg={}-{}: {}", jobName, jobName, item, jobLog);
	}

	private void stopStreamHandler(PumpStreamHandler streamHandler) {
		try {
			streamHandler.stop();
		} catch (IOException ex) {
			log.debug("[{}] msg={}-{} Error at closing log stream. Should not be concern: {}", jobName, jobName,
					item, ex);
		}
	}

	private SaturnJobReturn handleException(long timeoutSeconds, Exception e) {
		SaturnJobReturn saturnJobReturn;
		String errMsg = e.toString();
		if (watchdog.isTimeout()) {
			saturnJobReturn = new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL,
					String.format("execute job timeout(%sms), %s", timeoutSeconds * 1000, errMsg),
					SaturnSystemErrorGroup.TIMEOUT);
			log.error("[{}] msg={}-{} timeout, {}", jobName, jobName, item, errMsg);
			return saturnJobReturn;
		}

		if (watchdog.isForceStop()) {
			saturnJobReturn = new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL,
					"the job was forced to stop, " + errMsg,
					SaturnSystemErrorGroup.FAIL);
			log.error("[{}] msg={}-{} force stopped, {}", jobName, jobName, item, errMsg);
			return saturnJobReturn;
		}

		saturnJobReturn = new SaturnJobReturn(SaturnSystemReturnCode.USER_FAIL, "Exception: " + errMsg,
				SaturnSystemErrorGroup.FAIL);
		log.error("[{}] msg={}-{} Exception: {}", jobName, jobName, errMsg, e);
		return saturnJobReturn;
	}
}
