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

	public SaturnExecutorExtension(String executorName, String namespace, ClassLoader jobClassLoader) {
		this.executorName = executorName;
		this.namespace = namespace;
		this.jobClassLoader = jobClassLoader;
	}

	public abstract void initBefore();
	
	public abstract void initLogDirEnv();
	
	public abstract void initLogBefore();
	
	public abstract void initAfter();
	
	public abstract void registerJobType();
	
	public abstract void validateNamespaceExisting(String connectString) throws Exception;
	
	public abstract void init();

}
