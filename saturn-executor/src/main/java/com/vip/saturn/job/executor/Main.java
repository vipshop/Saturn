package com.vip.saturn.job.executor;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
	private boolean executorClassLoaderShouldBeClosed;
	private boolean jobClassLoaderShouldBeClosed;

	protected void parseArgs(String[] inArgs) throws Exception {
		String[] args = inArgs.clone();

		for (int i = 0; i < args.length; i++) {
			String param = args[i].trim();

			switch (param) {
				case "-namespace":
					this.namespace = obtainParam(args, ++i, "namespace");
					System.setProperty("app.instance.name", this.namespace);
					System.setProperty("namespace", this.namespace);
					break;
				case "-executorName":
					this.executorName = obtainParam(args, ++i, "executorName");
					break;
				case "-saturnLibDir":
					this.saturnLibDir = obtainParam(args, ++i, "saturnLibDir");
					break;
				case "-appLibDir":
					this.appLibDir = obtainParam(args, ++i, "appLibDir");
					break;
				default:
					break;
			}
		}

		validateMandatoryParameters();
	}

	private String obtainParam(String[] args, int position, String paramName) {
		String value = null;
		if (position < args.length) {
			value = args[position].trim();
		}
		if (isBlank(value)) {
			throw new RuntimeException(String.format("Please set the value of parameter:%s", paramName));
		}
		return value;
	}

	private void validateMandatoryParameters() {
		if (isBlank(namespace)) {
			throw new RuntimeException("Please set the namespace parameter");
		}
	}

	private boolean isBlank(String str) {
		return str == null || str.trim().isEmpty();
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

	private static List<URL> getUrls(File file) throws MalformedURLException {
		List<URL> urls = new ArrayList<>();
		if (!file.exists()) {
			return urls;
		}
		if (file.isDirectory()) {
			if ("classes".equals(file.getName())) {
				urls.add(file.toURI().toURL());
				return urls;
			}
			File[] files = file.listFiles();
			if (files != null && files.length > 0) {
				for (File tmp : files) {
					urls.addAll(getUrls(tmp));
				}
			}
			return urls;
		}
		if (file.isFile()) {
			urls.add(file.toURI().toURL());
		}
		return urls;
	}

	private void initClassLoader(ClassLoader executorClassLoader, ClassLoader jobClassLoader) throws Exception {
		setExecutorClassLoader(executorClassLoader);
		setJobClassLoader(jobClassLoader);
	}

	private void setJobClassLoader(ClassLoader jobClassLoader) throws MalformedURLException {
		if (jobClassLoader == null) {
			if (new File(appLibDir).isDirectory()) {
				List<URL> urls = getUrls(new File(appLibDir));
				this.jobClassLoader = new JobClassLoader(urls.toArray(new URL[urls.size()]));
				this.jobClassLoaderShouldBeClosed = true;
			} else {
				this.jobClassLoader = this.executorClassLoader;
				this.jobClassLoaderShouldBeClosed = false;
			}
		} else {
			this.jobClassLoader = jobClassLoader;
			this.jobClassLoaderShouldBeClosed = false;
		}
	}

	private void setExecutorClassLoader(ClassLoader executorClassLoader) throws MalformedURLException {
		if (executorClassLoader == null) {
			List<URL> urls = getUrls(new File(saturnLibDir));
			this.executorClassLoader = new SaturnClassLoader(urls.toArray(new URL[urls.size()]),
					Main.class.getClassLoader());
			this.executorClassLoaderShouldBeClosed = true;
		} else {
			this.executorClassLoader = executorClassLoader;
			this.executorClassLoaderShouldBeClosed = false;
		}
	}

	private void startExecutor(Object saturnApplication) throws Exception {
		ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(executorClassLoader);
		try {
			Class<?> startExecutorClass = getSaturnExecutorClass();
			saturnExecutor = startExecutorClass
					.getMethod("buildExecutor", String.class, String.class, ClassLoader.class, ClassLoader.class,
							Object.class)
					.invoke(null, namespace, executorName, executorClassLoader, jobClassLoader, saturnApplication);
			startExecutorClass.getMethod("execute").invoke(saturnExecutor);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCL);
		}
	}

	public void launch(String[] args, ClassLoader jobClassLoader) throws Exception {
		parseArgs(args);
		initClassLoader(null, jobClassLoader);
		startExecutor(null);
	}

	public void launch(String[] args, ClassLoader jobClassLoader, Object saturnApplication) throws Exception {
		parseArgs(args);
		initClassLoader(null, jobClassLoader);
		startExecutor(saturnApplication);
	}

	public void launchInner(String[] args, ClassLoader executorClassLoader, ClassLoader jobClassLoader)
			throws Exception {
		parseArgs(args);
		initClassLoader(executorClassLoader, jobClassLoader);
		startExecutor(null);
	}

	public void shutdown() throws Exception {
		shutdown("shutdown");
	}

	public void shutdownGracefully() throws Exception {
		shutdown("shutdownGracefully");
	}

	private void shutdown(String methodName) throws Exception {
		ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(executorClassLoader);
		try {
			Class<?> startExecutorClass = getSaturnExecutorClass();
			startExecutorClass.getMethod(methodName).invoke(saturnExecutor);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCL);
			closeClassLoader();
		}
	}

	private Class<?> getSaturnExecutorClass() throws ClassNotFoundException {
		return executorClassLoader.loadClass("com.vip.saturn.job.executor.SaturnExecutor");
	}

	private void closeClassLoader() {
		try {
			if (jobClassLoaderShouldBeClosed && jobClassLoader != null && jobClassLoader instanceof Closeable) {
				((Closeable) jobClassLoader).close();
			}
		} catch (IOException e) { // NOSONAR
		}
		try {
			if (executorClassLoaderShouldBeClosed && executorClassLoader != null
					&& executorClassLoader instanceof Closeable) {
				((Closeable) executorClassLoader).close();
			}
		} catch (IOException e) { // NOSONAR
		}
	}

	public static void main(String[] args) {
		try {
			Main main = new Main();
			main.parseArgs(args);
			main.initClassLoader(null, null);
			main.startExecutor(null);
		} catch (InvocationTargetException ite) {// NOSONAR
			printThrowableAndExit(ite.getCause());
		} catch (Throwable t) {// NOSONAR
			printThrowableAndExit(t);
		}
	}

	private static void printThrowableAndExit(Throwable t) {
		if (t != null) {
			t.printStackTrace(); // NOSONAR
		}
		System.exit(1);
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