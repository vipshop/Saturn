package com.vip.saturn.job.executor;

/**
 *
 * @author hebelala
 *
 */
public abstract class SaturnExecutorExtension {

	protected String executorName;
	protected String namespace;
	protected ClassLoader jobClassLoader;
	protected ClassLoader executorClassLoader;

	public SaturnExecutorExtension(String executorName, String namespace, ClassLoader executorClassLoader,
			ClassLoader jobClassLoader) {
		this.executorName = executorName;
		this.namespace = namespace;
		this.jobClassLoader = jobClassLoader;
		this.executorClassLoader = executorClassLoader;
	}

	public abstract void initBefore();

	public abstract void initLogDirEnv();

	public abstract void initLog();

	public abstract void initAfter();

	public abstract void registerJobType();

	public abstract void validateNamespaceExisting(String connectString) throws Exception;

	public abstract void init();

	public abstract Class getExecutorConfigClass();

}
