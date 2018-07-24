package com.vip.saturn.job.plugin.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * 
 * @author xiaopeng.he
 *
 */
public class CommonUtils {

	private static final String FILE_SEPARATOR = System.getProperty("file.separator");

	private CommonUtils() {}

	public static boolean initSaturnHome() {
		File saturnHome = new File(System.getProperty("user.home") + FILE_SEPARATOR + ".saturn");
		saturnHome.mkdirs();
		File saturnCaches = new File(saturnHome, "caches");
		saturnCaches.mkdirs();
		return saturnCaches.exists();
	}

	public static File getSaturnHomeCaches() {
		return new File(System.getProperty("user.home") + FILE_SEPARATOR + ".saturn" + FILE_SEPARATOR + "caches");
	}

	public static void unzip(File zip, File directory) throws IOException {
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
				File f = new File(directory + File.separator + zipEntry.getName());
				try(BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(zipEntry));
						BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f))) {
					File f_p = f.getParentFile();
					if (f_p != null && !f_p.exists()) {
						f_p.mkdirs();
					}
					int len = -1;
					byte[] bs = new byte[2048];
					while ((len = bis.read(bs, 0, 2048)) != -1) {
						bos.write(bs, 0, len);
					}
					bos.flush();
				}
			}
		} finally {
			zipFile.close();
		}
	}

	public static void zip(List<File> runtimeLibFiles, File saturnContainerDir, File zipFile) throws IOException {
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
		/*
		 * for(File file : saturnContainerDir.listFiles()) { zip(file, "saturn", zos); }
		 */

		for (File file : runtimeLibFiles) {
			zip(file, "app" + FILE_SEPARATOR + "lib", zos);
		}
		zos.close();
	}

	private static void zip(File file, String parent, ZipOutputStream zos) throws IOException {
		if (file == null || !file.exists())
			return;
		if (file.isFile()) {
			String entryName = parent == null ? file.getName() : parent + FILE_SEPARATOR + file.getName();
			zos.putNextEntry(new ZipEntry(entryName));
			try(FileInputStream fis = new FileInputStream(file)) {
				int len = -1;
				byte[] bs = new byte[2048];
				while ((len = fis.read(bs, 0, 2048)) != -1) {
					zos.write(bs, 0, len);
				}
			}
		} else if (file.isDirectory()) {
			String entryName = parent == null ? file.getName() : parent + FILE_SEPARATOR + file.getName();
			zos.putNextEntry(new ZipEntry(entryName + "/"));
			File[] listFiles = file.listFiles();
			if (listFiles != null) {
				for (File tmp : file.listFiles()) {
					zip(tmp, entryName, zos);
				}
			}
		}
	}

}
