package com.vip.saturn.job.executor;

import java.io.File;
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

	private void initClassLoader() throws Exception {
		List<URL> urls = getUrls(new File(saturnLibDir));
		executorClassLoader = new SaturnClassLoader(urls.toArray(new URL[urls.size()]), Main.class.getClassLoader());
		if (new File(appLibDir).isDirectory()) {
			urls = getUrls(new File(appLibDir));
			jobClassLoader = new JobClassLoader(urls.toArray(new URL[urls.size()]));
		} else {
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