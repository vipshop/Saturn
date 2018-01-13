package com.vip.saturn.job.executor.utils;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.loader.ExecutableArchiveLauncher;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;

public class URLUtils {

	/**
	 * Get file urls, like file:/apps/lib/
	 */
	public static List<URL> getFileUrls(File file) throws MalformedURLException {
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
					urls.addAll(getFileUrls(tmp));
				}
			}
			return urls;
		} else if (file.isFile()) {
			urls.add(file.toURI().toURL());
		}
		return urls;
	}

	/**
	 * Get archive urls.
	 *
	 * @return empty list if fail to get jar urls.
	 */
	public static List<URL> getArchiveUrls(File jarFile) {
		try {
			return new ExecutableArchiveHandler(jarFile).retrieveUrls();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ArrayList<>();
	}

	public static List<URL> getJarURLs(List<URL> fileUrls) {
		List<URL> jarUrls = Lists.newLinkedList();
		for (URL url : fileUrls) {
			if (url.getFile().endsWith(".jar")) {
				jarUrls.add(url);
			}
		}

		return jarUrls;
	}


	public static URL[] toArray(List<URL> urlList) {
		return urlList.toArray(new URL[urlList.size()]);
	}

	/**
	 * Get fat jar urls.
	 */
	private static class ExecutableArchiveHandler extends ExecutableArchiveLauncher {

		private static final String BOOT_INF_CLASSES = "BOOT-INF/classes/";

		private static final String BOOT_INF_LIB = "BOOT-INF/lib/";

		protected ExecutableArchiveHandler(File file) throws IOException {
			super(new JarFileArchive(file));
		}

		public List<URL> retrieveUrls() throws Exception {
			List<Archive> archives = getClassPathArchives();
			List<URL> urls = new ArrayList<>(archives.size());
			for (Archive archive : archives) {
				urls.add(archive.getUrl());
			}
			return urls;
		}

		@Override
		protected boolean isNestedArchive(Archive.Entry entry) {
			if (entry.isDirectory()) {
				return entry.getName().equals(BOOT_INF_CLASSES);
			}
			return entry.getName().startsWith(BOOT_INF_LIB);
		}
	}
}
