package com.vip.saturn.job.executor;

import com.vip.saturn.job.executor.classloader.JobClassLoaderFactory;
import com.vip.saturn.job.executor.classloader.SaturnClassLoader;
import com.vip.saturn.job.executor.utils.URLUtils;
import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * Entrance of Saturn executor.
 *
 * @author xiaopeng.he
 *
 */
public class Main {

	private String namespace;
	private String executorName;
	private String saturnLibDir = getLibDir("lib");
	private String appLibDir = getLibDir("lib");
	private ClassLoader executorClassLoader;
	private ClassLoader jobClassLoader;
	private Object saturnExecutor;

	protected void parseArgs(String[] inArgs) throws Exception {
		String[] args = inArgs.clone();

		for (int i = 0; i < args.length; i++) {
			String param = args[i].trim();

			if ("-namespace".equals(param)) {
				try {
					this.namespace = args[++i].trim();// NOSONAR
					System.setProperty("app.instance.name", this.namespace); // For logback.
					System.setProperty("namespace", this.namespace); // For logback.
				} catch (Exception e) {
					throw new Exception("Please set namespace value, exception message: " + e.getMessage(), e);
				}
			} else if ("-executorName".equals(param)) {
				try {
					executorName = args[++i].trim();// NOSONAR
				} catch (Exception e) {
					throw new Exception("Please set executorName value, exception message: " + e.getMessage(), e);
				}
			} else if ("-saturnLibDir".equals(param)) {
				try {
					saturnLibDir = args[++i].trim();// NOSONAR
				} catch (Exception e) {
					throw new Exception("Please set saturnLibDir value, exception message: " + e.getMessage(), e);
				}
			} else if ("-appLibDir".equals(param)) {
				try {
					appLibDir = args[++i].trim();// NOSONAR
				} catch (Exception e) {
					throw new Exception("Please set appLibDir value, exception message: " + e.getMessage(), e);
				}
			}
		}

		validateParameters();
	}

	private void validateParameters() {
		if (namespace == null) {
			throw new RuntimeException("Please add the namespace parameter");
		}
	}

	public String getExecutorName() {
		if (saturnExecutor != null) {
			try {
				return (String) saturnExecutor.getClass().getMethod("getExecutorName").invoke(saturnExecutor);
			} catch (Exception e) {// NOSONAR
			}
		}
		return executorName;
	}

	private static String getLibDir(String dirName) {
		File root = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile();
		File lib = new File(root, dirName);
		if (!lib.isDirectory()) {
			return null;
		}
		return lib.getAbsolutePath();
	}

	private void initClassLoader() throws Exception {
		List<URL> urls = URLUtils.getFileUrls(new File(saturnLibDir));
		executorClassLoader = new SaturnClassLoader(urls.toArray(new URL[urls.size()]), Main.class.getClassLoader());// NOSONAR
		jobClassLoader = JobClassLoaderFactory.getJobClassLoader(appLibDir);
		if (jobClassLoader == null) {
			jobClassLoader = executorClassLoader;
		}
	}

	private void startExecutor() throws Exception {
		ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(executorClassLoader);
		try {
			Class<?> startExecutorClass = getSaturnExecutorClass();
			saturnExecutor = startExecutorClass
					.getMethod("buildExecutor", String.class, String.class, ClassLoader.class, ClassLoader.class)
					.invoke(null, namespace, executorName, executorClassLoader, jobClassLoader);
			startExecutorClass.getMethod("execute").invoke(saturnExecutor);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCL);
		}
	}

	public void launch(String[] args, ClassLoader jobClassLoader) throws Exception {
		parseArgs(args);
		initClassLoader();
		this.jobClassLoader = jobClassLoader;
		startExecutor();
	}

	public void launchInner(String[] args, ClassLoader saturnClassLoader, ClassLoader jobClassLoader) throws Exception {
		parseArgs(args);
		this.executorClassLoader = saturnClassLoader;
		this.jobClassLoader = jobClassLoader;
		startExecutor();
	}

	public void shutdown() throws Exception {
		ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(executorClassLoader);
		try {
			Class<?> startExecutorClass = getSaturnExecutorClass();
			startExecutorClass.getMethod("shutdown").invoke(saturnExecutor);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCL);
		}
	}

	private Class<?> getSaturnExecutorClass() throws ClassNotFoundException {
		return executorClassLoader.loadClass("com.vip.saturn.job.executor.SaturnExecutor");
	}

	public static void main(String[] args) {
		try {
			Main main = new Main();
			main.parseArgs(args);
			main.initClassLoader();
			main.startExecutor();
		} catch (Throwable t) {// NOSONAR
			t.printStackTrace(); // NOSONAR
			System.exit(1);
		}
	}

	public Object getSaturnExecutor() {
		return saturnExecutor;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
}