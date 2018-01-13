package com.vip.saturn.job.executor.classloader;

import com.vip.saturn.job.executor.utils.URLUtils;
import java.io.File;
import java.net.URL;
import java.util.List;
import org.springframework.boot.loader.LaunchedURLClassLoader;

public class JobClassLoaderFactory {

	/**
	 * 获取作业classloader
	 *
	 * @param appLibPath 作业代码类库路径
	 */
	public static ClassLoader getJobClassLoader(String appLibPath) throws Exception {
		File appLibFile = new File(appLibPath);
		// should be a folder, otherwise return null.
		if (appLibFile.isFile()) {
			return null;
		}

		List<URL> fileUrls = URLUtils.getFileUrls(appLibFile);
		if (fileUrls == null || fileUrls.size() == 0) {
			return null;
		}
		// 当且仅当只有1个jar包，才会尝试去使用LaunchedURLClassLoader去检测URL
		List<URL> jarUrls = URLUtils.getJarURLs(fileUrls);
		if (jarUrls.size() == 1) {
			List<URL> archiveUrls = URLUtils.getArchiveUrls(new File(jarUrls.get(0).getFile()));
			if (archiveUrls != null && archiveUrls.size() > 0) {
				return new LaunchedURLClassLoader(URLUtils.toArray(archiveUrls), null);
			}
		}

		return new SaturnClassLoader(URLUtils.toArray(fileUrls), null);
	}

}
