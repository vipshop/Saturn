package com.vip.saturn.job.executor;

import com.vip.saturn.job.basic.JobTypeManager;
import com.vip.saturn.job.java.SaturnJavaJob;
import com.vip.saturn.job.shell.SaturnScriptJob;
import com.vip.saturn.job.utils.LocalHostService;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hebelala
 *
 */
public class SaturnExecutorExtensionDefault extends SaturnExecutorExtension {

	private static Logger log;

	private static final String NAME_VIP_SATURN_LOG_DIR = "VIP_SATURN_LOG_DIR";

	public SaturnExecutorExtensionDefault(String executorName, String namespace, ClassLoader executorClassLoader,
			ClassLoader jobClassLoader) {
		super(executorName, namespace, executorClassLoader, jobClassLoader);
	}

	@Override
	public void initBefore() {
		// 默认没有逻辑，子类实现其逻辑
	}

	@Override
	public void initLogDirEnv() {
		String saturnLogDir = System
				.getProperty(NAME_VIP_SATURN_LOG_DIR, getEnv(NAME_VIP_SATURN_LOG_DIR, getDefaultLogDir(executorName)));
		System.setProperty("saturn.log.dir", saturnLogDir); // for logback.xml
	}

	private static String getEnv(String key, String defaultValue) {
		String v = System.getenv(key);
		if (v == null || v.isEmpty()) {
			return defaultValue;
		}
		return v;
	}

	private static String getDefaultLogDir(String executorName) {
		return "/apps/logs/saturn/" + System.getProperty("namespace") + "/" + executorName + "-"
				+ LocalHostService.cachedIpAddress;
	}

	@Override
	public void initLog() {
		setLogger();
	}

	private static void setLogger() {
		if (log == null) {
			log = LoggerFactory.getLogger(SaturnExecutorExtensionDefault.class);
		}
	}

	@Override
	public void initAfter() {
		SystemEnvProperties.init();
	}

	@Override
	public void registerJobType() {
		JobTypeManager.getInstance().registerHandler("JAVA_JOB", SaturnJavaJob.class);
		JobTypeManager.getInstance().registerHandler("SHELL_JOB", SaturnScriptJob.class);
	}

	@Override
	public void validateNamespaceExisting(String connectString) throws Exception {
		// 默认没有逻辑，子类实现其逻辑
	}

	@Override
	public void init() {
		initBefore();
		initLogDirEnv();
		initLog();
		initAfter();
		registerJobType();
	}

	@Override
	public Class getExecutorConfigClass() {
		return ExecutorConfig.class;
	}
}
