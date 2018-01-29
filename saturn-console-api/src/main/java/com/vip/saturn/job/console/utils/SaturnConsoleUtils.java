package com.vip.saturn.job.console.utils;

import com.vip.saturn.job.console.domain.JobType;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository.CuratorFrameworkOp;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.TimeZone;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils for saturn console.
 *
 * @author kfchu
 */
public class SaturnConsoleUtils {

	private static final Logger log = LoggerFactory.getLogger(SaturnConsoleUtils.class);

	private static DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

	private static Random random = new Random();

	public static String parseMillisecond2DisplayTime(String longInStr) {
		return parseMillisecond2DisplayTime(longInStr, null);
	}

	public static String parseMillisecond2DisplayTime(String longInStr, TimeZone timezone) {
		if (StringUtils.isBlank(longInStr)) {
			return null;
		}

		return dtf.print(new DateTime(Long.parseLong(longInStr), DateTimeZone.forTimeZone(timezone)));
	}

	/**
	 * 如果存在/config/enabledReport节点，则返回节点的内容； 如果不存在/config/enabledReport节点，如果作业类型是Java或者Shell，则返回true；否则，返回false；
	 */
	public static boolean checkIfJobIsEnabledReport(String jobName, CuratorFrameworkOp curatorFrameworkOp) {
		String enabledReportNodePath = JobNodePath.getEnabledReportNodePath(jobName);

		if (curatorFrameworkOp.checkExists(enabledReportNodePath)) {
			return Boolean.parseBoolean(curatorFrameworkOp.getData(enabledReportNodePath));
		}

		String jobTypeNodePath = JobNodePath.getConfigNodePath(jobName, "jobType");

		// if enabledReportNodePath不存在, 如果作业类型是JAVA或者Shell，默认上报
		JobType jobType = JobType.getJobType(curatorFrameworkOp.getData(jobTypeNodePath));

		return jobType == JobType.JAVA_JOB || jobType == JobType.SHELL_JOB;
	}

	public static File createTmpFile() throws SaturnJobConsoleException, IOException {
		int loopTimes = 5;

		int i = 0;
		File tmp = null;
		for (; i < loopTimes; i++) {
			tmp = new File(SaturnConstants.CACHES_FILE_PATH, genTmpFileName());
			if (!tmp.exists()) {
				break;
			}
		}

		if (i == loopTimes) {
			throw new SaturnJobConsoleException("fail to create temp file.");
		}

		FileUtils.forceMkdir(tmp.getParentFile());
		tmp.createNewFile();

		return tmp;
	}

	private static String genTmpFileName() {
		return "tmp_exportFile_" + System.currentTimeMillis() + "_" + random.nextInt(1000) + ".xls";
	}

	public static void exportExcelFile(HttpServletResponse response, File srcFile, String exportFileName,
			boolean deleteTmpFile) throws SaturnJobConsoleException {
		try {
			InputStream inputStream = new FileInputStream(srcFile);
			exportExcelFile(response, inputStream, exportFileName);
		} catch (FileNotFoundException e) {
			throw new SaturnJobConsoleException("file not found:" + srcFile.getName());
		} finally {
			if (deleteTmpFile && srcFile != null) {
				srcFile.delete();
			}
		}
	}

	public static void exportExcelFile(HttpServletResponse response, InputStream inputStream, String exportFileName)
			throws SaturnJobConsoleException {
		try {
			response.setContentType("application/octet-stream");
			response.setHeader("Content-disposition",
					"attachment; filename=" + new String(exportFileName.getBytes("UTF-8"), "ISO8859-1"));

			BufferedInputStream bis = null;
			BufferedOutputStream bos = null;
			try {
				bis = new BufferedInputStream(inputStream);
				bos = new BufferedOutputStream(response.getOutputStream());
				byte[] buff = new byte[2048];
				int bytesRead;
				while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
					bos.write(buff, 0, bytesRead);
				}
			} finally {
				if (bis != null) {
					try {
						bis.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
				if (bos != null) {
					try {
						bos.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		} catch (Exception e) {
			throw new SaturnJobConsoleException(e);
		}
	}

}
