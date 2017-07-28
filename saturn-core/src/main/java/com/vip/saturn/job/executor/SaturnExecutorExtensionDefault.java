package com.vip.saturn.job.executor;

import com.vip.saturn.job.basic.JobTypeManager;
import com.vip.saturn.job.java.SaturnJavaJob;
import com.vip.saturn.job.shell.SaturnScriptJob;
import com.vip.saturn.job.utils.LocalHostService;
import com.vip.saturn.job.utils.SystemEnvProperties;

/**
 * 
 * @author hebelala
 *
 */
public class SaturnExecutorExtensionDefault extends SaturnExecutorExtension {

	private static final String NAME_SATURN_LOG_DIR = "SATURN_LOG_DIR";

	public SaturnExecutorExtensionDefault(String executorName, String namespace, ClassLoader jobClassLoader) {
		super(executorName, namespace, jobClassLoader);
	}

	@Override
	public void initBefore() {
		
	}
	
	@Override
	public void initLogDirEnv() {
		String SATURN_LOG_DIR = System.getProperty(NAME_SATURN_LOG_DIR, getEnv(NAME_SATURN_LOG_DIR, getDefaultLogDir(executorName)));
		System.setProperty("saturn.log.dir", SATURN_LOG_DIR); // for logback.xml
	}
	
	private static String getEnv(String key, String defaultValue) {
		String v = System.getenv(key);
		if (v == null || v.isEmpty()) {
			return defaultValue;
		}
		return v;
	}
	
	private static String getDefaultLogDir(String executorName) {
		return "/apps/logs/saturn/" + System.getProperty("namespace") + "/" + executorName + "-" + LocalHostService.cachedIpAddress;
	}

	@Override
	public void initLogBefore() {
		
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
		
	}

	@Override
	public void init() {
		initBefore();
		initLogDirEnv();
		initLogBefore();
		initAfter();
		registerJobType();		
	}

}
