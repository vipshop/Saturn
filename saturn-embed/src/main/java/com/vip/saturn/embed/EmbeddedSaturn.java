package com.vip.saturn.embed;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class EmbeddedSaturn {

	private Object main;
	private Object saturnApplication;

	public void start() throws Exception {
		try {
			String saturnZip = System.getProperty("saturn.zipfile", System.getenv("SATURN_ZIP_FILE"));
			if (saturnZip != null && !saturnZip.isEmpty()) {
				File zipFile = new File(saturnZip);
				File saturnHome = null;
				if (zipFile.canRead()) {
					saturnHome = new File(System.getProperty("user.home") + File.separator + "saturn");
					deleteDir(saturnHome);
					unzip(zipFile, saturnHome);
					System.setProperty("saturn.home", saturnHome.listFiles()[0].getAbsolutePath());
				}
			}
		} catch (Exception e) {// NOSONAR
			e.printStackTrace();
		}

		String saturnHome = System.getProperty("saturn.home", System.getenv("SATURN_HOME"));
		if (saturnHome == null || saturnHome.isEmpty()) {
			throw new Exception("saturn.home is not set");
		}
		if (!new File(saturnHome).isDirectory()) {
			throw new Exception("saturn executor not found in " + saturnHome);
		}

		String namespace = System.getProperty("saturn.app.namespace", System.getenv("SATURN_APP_NAMESPACE"));
		String executorName = System.getProperty("saturn.app.executorName", System.getenv("SATURN_APP_EXECUTOR_NAME"));
		if (namespace == null || namespace.isEmpty()) {
			throw new Exception("saturn.app.namespace is not set");
		}

		URLClassLoader executorClassLoader = new URLClassLoader(
				new URL[]{new File(saturnHome, "saturn-executor.jar").toURI().toURL()}, null);
		final List<String> argList = new ArrayList<>();
		String saturnLibDir = saturnHome + File.separator + "lib";
		argList.add("-saturnLibDir");
		argList.add(saturnLibDir);

		argList.add("-namespace");
		argList.add(namespace);

		if (executorName != null && !executorName.isEmpty()) {
			argList.add("-executorName");
			argList.add(executorName);
		}

		Class<?> mainClass = executorClassLoader.loadClass("com.vip.saturn.job.executor.Main");
		main = mainClass.newInstance();
		Method launchMethod = mainClass.getMethod("launch", String[].class, ClassLoader.class, Object.class);

		String[] args = new String[argList.size()];
		int i = 0;
		for (String arg : argList) {
			args[i] = arg;
			i++;
		}

		launchMethod.invoke(main, args, EmbeddedSaturn.class.getClassLoader(), saturnApplication);
	}

	/**
	 * 递归删除目录下的所有文件及子目录下所有文件
	 * @param dir 将要删除的文件目录
	 * @return boolean Returns "true" if all deletions were successful. If a deletion fails, the method stops attempting
	 * to delete and returns "false".
	 */
	private boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// 目录此时为空，可以删除
		return dir.delete();
	}

	private void unzip(File zip, File directory) throws IOException {
		ZipFile zipFile = new ZipFile(zip);
		try {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				if (zipEntry.isDirectory()) {
					File temp = new File(directory + File.separator + zipEntry.getName());
					temp.mkdirs();
					continue;
				}
				BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(zipEntry));
				try {
					File f = new File(directory + File.separator + zipEntry.getName());
					File f_p = f.getParentFile();
					if (f_p != null && !f_p.exists()) {
						f_p.mkdirs();
					}
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
					try {
						int len = -1;
						byte[] bs = new byte[2048];
						while ((len = bis.read(bs, 0, 2048)) != -1) {
							bos.write(bs, 0, len);
						}
						bos.flush();
					} finally {
						bos.close();
					}
				} finally {
					bis.close();
				}
			}
		} finally {
			zipFile.close();
		}
	}

	public void stop() throws Exception {
		if (main != null) {
			main.getClass().getMethod("shutdown").invoke(main);
		}
	}

	public void stopGracefully() throws Exception {
		if (main != null) {
			main.getClass().getMethod("shutdownGracefully").invoke(main);
		}
	}

	public Object getSaturnApplication() {
		return saturnApplication;
	}

	public void setSaturnApplication(Object saturnApplication) {
		this.saturnApplication = saturnApplication;
	}

}
