package com.vip.saturn.job.utils;

import com.vip.saturn.job.basic.SaturnConstant;
import com.vip.saturn.job.executor.SaturnExecutorsNode;
import com.vip.saturn.job.internal.config.ConfigurationNode;
import com.vip.saturn.job.internal.execution.ExecutionNode;
import com.vip.saturn.job.internal.storage.JobNodePath;
import com.vip.saturn.job.reg.base.CoordinatorRegistryCenter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于处理Shell的相关pid功能
 *
 * @author linzhaoming
 */
public class ScriptPidUtils {

	private static final Logger log = LoggerFactory.getLogger(ScriptPidUtils.class);

	public static final long UNKNOWN_PID = -1;

	/**
	 * 系统分隔符
	 */
	protected static final String FILESEPARATOR = System.getProperty("file.separator");

	/**
	 * Saturn的运行目录 <p> ${HOME}/.saturn/executing
	 */
	public static final String EXECUTINGPATH = System.getProperty("user.home") + FILESEPARATOR + ".saturn"
			+ FILESEPARATOR + "executing";

	/**
	 * Saturn的运行目录 <p> ${HOME}/.saturn/output
	 */
	public static final String OUTPUT_PATH = System.getProperty("user.home") + FILESEPARATOR + ".saturn" + FILESEPARATOR
			+ "output";

	/**
	 * 作业执行的运行目录 <p> 目录: ${HOME}/.saturn/executing/[executorName]/[jobName]
	 */
	public static final String EXECUTINGJOBPATH = EXECUTINGPATH + FILESEPARATOR + "%s" + FILESEPARATOR + "%s";

	/**
	 * 作业执行的Pid文件 <p> 目录: ${HOME}/.saturn/executing/[executorName]/[jobName]/[jobItem]/PID
	 */
	public static final String JOBITEMPIDSPATH = EXECUTINGJOBPATH + FILESEPARATOR + "%s" + FILESEPARATOR + "PIDS";
	public static final String JOBITEMPATH = EXECUTINGJOBPATH + FILESEPARATOR + "%s";

	public static final String JOBITEMPIDPATH2 = EXECUTINGJOBPATH + FILESEPARATOR + "%s" + FILESEPARATOR + "PIDS"
			+ FILESEPARATOR + "%s";

	/**
	 * Shell作业执行的回写结果路径文件 <p> 目录: ${HOME}/.saturn/output/[executorName]/[jobName]/[jobItem]/[randomId/messageId]/[timestamp]
	 */
	public static final String JOBITEMOUTPUTPATH = OUTPUT_PATH + FILESEPARATOR + "%s" + FILESEPARATOR + "%s"
			+ FILESEPARATOR + "%s" + FILESEPARATOR + "%s" + FILESEPARATOR + "%s";

	private static final String CHECK_RUNNING_JOB_THREAD_NAME = "check-if-job-%s-done";

	/**
	 * 获取当前Saturn的执行目录(executing)
	 *
	 * @return Saturn的执行目录
	 */
	public static File getSaturnExecutingHome() {
		File executingHome = new File(EXECUTINGPATH);

		try {
			FileUtils.forceMkdir(executingHome);
		} catch (Exception ex) {
			log.error("msg=Creating directory error", ex);
		}

		if (executingHome.exists() && executingHome.isDirectory()) {
			return executingHome;
		} else {
			return null;
		}
	}

	/**
	 * 写入对应的作业分片的pid文件
	 *
	 * @param executorName Executor name
	 * @param jobName 作业名字
	 * @param jobItem 作业分片
	 * @param pid 进程pid
	 */
	public static void writePidToFile(String executorName, String jobName, int jobItem, long pid) {
		String dir = String.format(JOBITEMPIDSPATH, executorName, jobName, jobItem);
		String path = String.format(JOBITEMPIDPATH2, executorName, jobName, jobItem, pid);
		try {
			FileUtils.forceMkdir(new File(dir));
			File itemFile = new File(path);
			FileUtils.writeStringToFile(itemFile, String.valueOf(pid));
		} catch (IOException e) {
			log.error(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName, "Writing the pid file error"), e);
		}
	}

	/**
	 * @deprecated 仅用于兼容旧版，获取 PID
	 */
	@Deprecated
	public static long _getPidFromFile(String executorName, String jobName, String jobItem) {
		String path = String.format(JOBITEMPATH, executorName, jobName, jobItem);

		File itemFile = new File(path);
		if (!itemFile.exists() || !itemFile.isFile()) {
			return UNKNOWN_PID;
		}
		try {
			String pid = FileUtils.readFileToString(itemFile);
			try {
				return Long.parseLong(pid);
			} catch (NumberFormatException e) {
				log.error(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName, "Parsing the pid file error"),
						e);
				return UNKNOWN_PID;
			}
		} catch (IOException e) {
			log.error(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName, "Reading the pid file error"), e);
			return UNKNOWN_PID;
		}
	}

	public static long getFirstPidFromFile(String executorName, String jobName, String jobItem) {
		List<Long> pids = getPidsFromFile(executorName, jobName, jobItem);
		if (pids.isEmpty()) {
			return UNKNOWN_PID;
		}
		return pids.get(0);
	}

	/**
	 * 获取对应作业分片的pid, -1表示不存在或读取出错
	 *
	 * @param executorName Executor Name
	 * @param jobName 作业名
	 * @param jobItem 作业分片
	 * @return pid
	 */
	public static List<Long> getPidsFromFile(String executorName, String jobName, String jobItem) {
		List<Long> pids = new ArrayList<Long>();
		// 兼容旧版PID目录
		Long pid = _getPidFromFile(executorName, jobName, jobItem);
		if (pid > 0) {
			pids.add(pid);
		}

		String path = String.format(JOBITEMPIDSPATH, executorName, jobName, jobItem);

		File dir = new File(path);
		if (!dir.exists() || !dir.isDirectory()) {
			return pids;
		}
		File[] files = dir.listFiles();

		if (files == null || files.length == 0) {
			return pids;
		}

		for (File file : files) {
			try {
				pids.add(Long.valueOf(file.getName()));
			} catch (Exception e) {
				log.error(String.format(SaturnConstant.LOG_FORMAT_FOR_STRING, jobName, "Parsing the pid file error"),
						e);
			}
		}

		return pids;
	}

	/**
	 * 获取对应作业的分片pid文件列表
	 *
	 * @param executorName Executor Name
	 * @param jobName 作业名
	 * @return pid文件列表
	 */
	public static String[] getItemsPaths(String executorName, String jobName) {
		String jobNamePath = String.format(EXECUTINGJOBPATH, executorName, jobName);

		File jobNameFile = new File(jobNamePath);
		if (!jobNameFile.exists() || jobNameFile.isFile()) {
			return new String[0];
		}
		File[] files = jobNameFile.listFiles();

		if (files == null || files.length == 0) {
			return new String[]{};
		}

		String[] filePaths = new String[files.length];

		int i = 0;
		for (File file : files) {
			filePaths[i++] = file.getAbsolutePath();
		}

		return filePaths;
	}

	/**
	 * 删除作业分片的全部pid文件
	 *
	 * @param executorName Executor Name
	 * @param jobName 作业名
	 * @param jobItem 作业分片
	 * @return 删除是否成功
	 */
	public static boolean removeAllPidFile(String executorName, String jobName, String jobItem) {
		String path = String.format(JOBITEMPATH, executorName, jobName, jobItem);

		File itemFile = new File(path);
		if (!itemFile.exists()) {
			return false;
		}
		try {
			FileUtils.forceDelete(itemFile);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return true;
	}

	public static boolean removePidFile(String executorName, String jobName, String jobItem, long pid) {
		String path = String.format(JOBITEMPIDPATH2, executorName, jobName, jobItem, pid);

		File itemFile = new File(path);
		if (!itemFile.exists()) {
			return false;
		}
		try {
			FileUtils.forceDelete(itemFile);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return true;
	}

	public static boolean removeAllPidFile(String executorName, String jobName, int jobItem) {
		return removeAllPidFile(executorName, jobName, "" + Integer.toString(jobItem));
	}

	/**
	 * This method will kill all the child/grandchild/... processes.
	 *
	 * @param pid pid to kill.
	 */
	public static void killAllChildrenByPid(long pid, boolean force) {
		if (pid <= UNKNOWN_PID) {
			return;
		}
		String pidStr = Long.toString(pid) + "";
		List<String> pidList = new ArrayList<>();
		pidList.add(pidStr);
		while (null != (pidStr = exeCmdWithoutPipe(CommandLine.parse("pgrep -P " + pidStr), null, null))) {
			String[] pids = pidStr.split(System.getProperty("line.separator"));
			for (int i = 0; i < pids.length; i++) {
				pidList.add(pids[i]);
			}
			pidStr = StringUtils.join(pids, ",");
		}
		// make sure kill the son before kill the parent.
		for (int i = pidList.size() - 1; i >= 0; i--) {
			String ppid = pidList.get(i);
			if (!isPidRunning(ppid)) {
				continue;
			}
			if (force) {
				exeWholeCmd("kill -9 " + ppid);
			} else {
				exeWholeCmd("kill " + ppid);
			}
		}
	}

	public static String exeWholeCmd(String cmd) {
		// Common apache exec doesn't support piple operation.
		// It's the shell (e.g. bash) that interprets the pipe and does special processing when you type that
		// commandline into the shell.
		// But we could use a ByteArrayInputStream to feed the outuput of one command to another.
		if (cmd.contains("|")) {
			String[] cmds = cmd.split("\\|");
			String out = null;
			for (int i = 0; i < cmds.length; i++) {
				CommandLine cmdLine = CommandLine.parse(cmds[i]);
				if (i == 0) {
					out = exeCmdWithoutPipe(cmdLine, null, loadEnv());
				}
				if (out != null) {
					out = exeCmdWithoutPipe(cmdLine, new ByteArrayInputStream(out.getBytes(Charset.forName("utf-8"))),
							loadEnv());
				}
			}
			return out;
		} else {
			CommandLine cmdLine = CommandLine.parse(cmd);
			return exeCmdWithoutPipe(cmdLine, null, loadEnv());
		}
	}

	public static String exeCmdWithoutPipe(CommandLine cmdLine, ByteArrayInputStream input, Map<String, String> env) {
		DefaultExecutor executor = new DefaultExecutor();
		ExecuteWatchdog dog = new ExecuteWatchdog(3 * 1000);
		executor.setWatchdog(dog);
		executor.setExitValue(0);
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			SaturnLogOutputStream errorOS = new SaturnLogOutputStream(log, SaturnLogOutputStream.LEVEL_ERROR);
			PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorOS, input);
			executor.setStreamHandler(streamHandler);
			log.info("msg=exec command: {}", cmdLine);
			int value = executor.execute(cmdLine, env);
			if (value == 0) {
				String out = outputStream.toString();
				return out;
			} else {
				return null;
			}
		} catch (Exception e) {
			log.error("msg=" + e.getMessage(), e);
			return null;
		}
	}

	/**
	 * 检查是否已经已有作业名重复运行
	 */
	public static void checkAllExistJobs(final CoordinatorRegistryCenter regCenter) {
		List<String> zkJobNames = regCenter.getChildrenKeys(JobNodePath.ROOT);
		if (zkJobNames == null || zkJobNames.isEmpty()) {
			return;
		}
		for (final String jobName : zkJobNames) {
			checkOneExistJob(regCenter, jobName);
		}
	}

	public static void forceStopRunningShellJob(final String executorName, final String jobName) {
		String[] itemPaths = ScriptPidUtils.getItemsPaths(executorName, jobName);
		if (itemPaths.length == 0) {
			log.info("[{}] msg={} no pids to kill", jobName, jobName);
			return;
		}
		for (String path : itemPaths) {
			String itemStr = StringUtils.substringAfterLast(path, File.separator);
			int jobItem = Integer.parseInt(itemStr);

			List<Long> pids = ScriptPidUtils.getPidsFromFile(executorName, jobName, "" + Integer.toString(jobItem));
			for (Long pid : pids) {
				if (pid > 0 && ScriptPidUtils.isPidRunning("" + pid)) {
					ScriptPidUtils.killAllChildrenByPid(pid, true);
				}
			}

			ScriptPidUtils.removeAllPidFile(executorName, jobName, jobItem);
		}
	}

	public static void checkOneExistJob(final CoordinatorRegistryCenter regCenter, final String jobName) {
		final String executorName = regCenter.getExecutorName();

		String[] itemPaths = ScriptPidUtils.getItemsPaths(executorName, jobName);
		if (itemPaths.length == 0) {
			return;
		}
		String jobTypePath = JobNodePath.getNodeFullPath(jobName, ConfigurationNode.JOB_TYPE);
		String jobType = regCenter.get(jobTypePath);
		// 只检查Shell作业
		if (!"SHELL_JOB".equals(jobType)) {
			log.info("{} is not shell job ,igore checking ", jobName);
			return;
		}
		String enabledPath = JobNodePath.getNodeFullPath(jobName, ConfigurationNode.ENABLED);
		String isEnabledStr = regCenter.get(enabledPath);
		log.info("[{}] msg={} pidFromFile size :{};isEnabledStr:{}", jobName, jobName, itemPaths.length, isEnabledStr);

		// null means new job, if there are pid files, kill -9.
		// if it's true, means it's an enabled job, there shouldn't exist the pid files. kill them with no mercy.
		if ("true".equals(isEnabledStr) || isEnabledStr == null) {
			killRunningShellProcess(executorName, jobName, itemPaths);
		} else {
			// if there are other executors, failover will occur. This executor only has to kill the pids.
			if (areThereOtherExecutorsRunningTheShards(regCenter, jobName)) {
				killRunningShellProcess(executorName, jobName, itemPaths);
			} else {
				// enabled job with pid files existed and no other executors, means that the job is exited improperly.
				// under this situation, we need to restore the running job status.
				final List<String> shardItems = new ArrayList();

				for (String path : itemPaths) {
					String itemStr = StringUtils.substringAfterLast(path, File.separator);
					int jobItem = Integer.parseInt(itemStr);
					long pid = ScriptPidUtils.getFirstPidFromFile(executorName, jobName,
							"" + Integer.toString(jobItem));
					if (pid > 0 && ScriptPidUtils.isPidRunning("" + Long.toString(pid))) {
						String runningPath = JobNodePath.getNodeFullPath(jobName,
								String.format(ExecutionNode.RUNNING, Integer.valueOf(itemStr)));
						regCenter.persistEphemeral(runningPath, "");
						log.info("[{}] msg={}-{} restores running status, path={}", jobName, jobName, path,
								runningPath);
						System.out.println(jobName + "-" + path + " restores running status, path=" + runningPath);// NOSONAR
						shardItems.add(itemStr);
						log.info("[{}] msg={}-{} is running, pid={}", jobName, jobName, path, pid);
					} else {
						ScriptPidUtils.removeAllPidFile(executorName, jobName, itemStr);
						log.info("[{}] msg={}-{} is not running, pid={}", jobName, jobName, path, pid);
					}
				}

				if (shardItems.isEmpty()) {
					return;
				}

				asyncCheckShellProcessIsDone(regCenter, jobName, executorName, shardItems);
			}
		}
	}

	private static void asyncCheckShellProcessIsDone(final CoordinatorRegistryCenter regCenter, final String jobName,
			final String executorName, final List<String> shardItems) {
		// start a thread to check if shell process is done, if yes, remove pid file -> add completed -> clear
		// running
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (!Thread.interrupted()) {
					try {
						TimeUnit.MILLISECONDS.sleep(500);
					} catch (InterruptedException ignore) {
						log.warn(ignore.getMessage());
					}

					boolean finished = true;
					for (String shardItem : shardItems) {
						long pid = ScriptPidUtils.getFirstPidFromFile(executorName, jobName, shardItem);
						if (pid > 0 && ScriptPidUtils.isPidRunning("" + Long.toString(pid))) {
							finished = false;
							continue;
						} else {
							// remove pid file -> add completed -> clear running
							// make sure u have added completed node before remove running node. otherwise
							// failover will triggered.
							ScriptPidUtils.removeAllPidFile(executorName, jobName, shardItem);
							String completedPath = JobNodePath.getNodeFullPath(jobName,
									String.format(ExecutionNode.COMPLETED, shardItem));
							regCenter.persist(completedPath, "");
							String runningPath = JobNodePath.getNodeFullPath(jobName,
									String.format(ExecutionNode.RUNNING, shardItem));
							regCenter.remove(runningPath);
							log.info("[{}] msg={} - {} is done, write complete node path {}", jobName, jobName,
									shardItem, completedPath);
							System.out.println(jobName + "-" + shardItem + " is done.");// NOSONAR
						}
					}
					if (finished) {
						log.info("[{}] msg=all running shell processes are done. now quit the thread.");
						System.out.println("all running shell processes are done. now quit the thread.");// NOSONAR
						break;
					}
				}
			}
		}, String.format(CHECK_RUNNING_JOB_THREAD_NAME, jobName)).start();
	}

	private static void killRunningShellProcess(String executorName, String jobName, String[] itemPaths) {
		for (String path : itemPaths) {
			Integer item = Integer.valueOf(StringUtils.substringAfterLast(path, File.separator));
			long pid = ScriptPidUtils.getFirstPidFromFile(executorName, jobName, "" + item);
			System.out.println("pid found for jobName:" + jobName + " executorName:" + executorName + ", kill -9 " + pid);// NOSONAR
			killAllChildrenByPid(pid, true);
			ScriptPidUtils.removeAllPidFile(executorName, jobName, item);
		}

	}

	private static boolean areThereOtherExecutorsRunningTheShards(final CoordinatorRegistryCenter regCenter,
			String jobName) {
		final String executorName = regCenter.getExecutorName();
		List<String> executors = regCenter.getChildrenKeys(SaturnExecutorsNode.SATURN_EXECUTORS_EXECUTORS_NODE_NAME);
		if (executors != null && !executors.isEmpty()) {
			for (String executor : executors) {
				if (!executorName.equals(executor)) {
					// check if this executor has taken care of the failovers.
					String sharding = regCenter.get(JobNodePath.getNodeFullPath(jobName, executor + "/sharding"));
					if (StringUtils.isNoneBlank(sharding)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/*
	 * public static boolean isPidRunning(long pid) { CommandLine cmdLine =
	 * CommandLine.parse(String.format(CHECK_PID_CMD, pid)); String outPut = exeCmdWithoutPipe(cmdLine, null, null); if
	 * (StringUtils.isBlank(outPut)) { return false; } return true; }
	 */

	public static boolean isPidRunning(long pid) {
		String path = "/proc/" + pid;
		return new File(path).exists();
	}

	public static boolean isPidRunning(String pid) {
		String path = "/proc/" + pid;
		return new File(path).exists();
	}

	public static Map<String, String> parseString2Map(String source) {
		Map<String, String> map = new HashMap<>();
		String[] lines = source.split(System.getProperty("line.separator"));
		String lastKey = null;
		for (String oneLine : lines) {
			String[] kvs = oneLine.split("=");
			if (kvs.length == 2) {
				map.put(kvs[0], kvs[1]);
				lastKey = kvs[0];
			} else if (kvs.length > 2) {
				map.put(kvs[0], oneLine.replace(kvs[0] + "=", ""));
				lastKey = kvs[0];
			} else if (kvs.length == 1 && StringUtils.isNotBlank(lastKey)) {
				String lastValue = map.get(lastKey);
				map.put(lastKey, lastValue + kvs[0]);
			}
		}
		return map;
	}

	public static Map<String, String> loadEnv() {
		Map<String, String> env = new HashMap<>();
		try {
			final CommandLine cmdLine = new CommandLine("/bin/sh");
			cmdLine.addArguments(new String[]{"-c", "source /etc/profile && env"}, false);
			String output = exeCmdWithoutPipe(cmdLine, null, null);
			if (output == null) {
				return env;
			}
			env = parseString2Map(output);
		} catch (Exception e) {
			log.error("msg=" + e.getMessage(), e);
		}
		return env;
	}

	public static String filterEnvInCmdStr(Map<String, String> env, String cmd) {
		String patternString = "\\$\\{?(" + StringUtils.join(env.keySet(), "|") + ")\\}?";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(cmd);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(sb, env.get(matcher.group(1)));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

}
