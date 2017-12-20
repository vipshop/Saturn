package com.vip.saturn.job.executor;

import com.vip.saturn.job.threads.SaturnThreadFactory;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Regular truncate saturn-executor.log while the size is over specified limit.
 */
public class PeriodicTruncateNohupOutService {

	private static final Logger log = LoggerFactory.getLogger(PeriodicTruncateNohupOutService.class);

	private static final long TRUNCATE_SIZE = Math.min(4096, SystemEnvProperties.VIP_SATURN_NOHUPOUT_SIZE_LIMIT_IN_BYTES);

	private ScheduledExecutorService truncateLogService;

	public PeriodicTruncateNohupOutService(String executorName) {
		truncateLogService = Executors.newScheduledThreadPool(1, new SaturnThreadFactory(executorName + "-truncate-nohup-out-thread", false));
	}

	public void start() {
		log.info("start PeriodicTruncateNohupOutService");
		if (StringUtils.isBlank(SystemEnvProperties.VIP_SATURN_LOG_OUTFILE)) {
			log.warn("file path is not set, please check.");
			return;
		}

		truncateLogService.scheduleAtFixedRate(new TruncateLogRunnable(),
				new Random().nextInt(10), SystemEnvProperties.VIP_SATURN_CHECK_NOHUPOUT_SIZE_INTERVAL_IN_SEC, TimeUnit.SECONDS);
	}

	public void shutdown() {
		log.info("shutdown PeriodicTruncateNohupOutService");
		if (truncateLogService != null) {
			truncateLogService.shutdown();
		}
	}

	private static class TruncateLogRunnable implements Runnable {

		@Override
		public void run() {
			FileChannel fc = null;
			RandomAccessFile file = null;
			try {
				file = new RandomAccessFile(SystemEnvProperties.VIP_SATURN_LOG_OUTFILE, "rw");
				fc = file.getChannel();
				if (fc.size() > SystemEnvProperties.VIP_SATURN_NOHUPOUT_SIZE_LIMIT_IN_BYTES) {
					log.info("truncate {} as size over {} bytes", SystemEnvProperties.VIP_SATURN_LOG_OUTFILE, SystemEnvProperties.VIP_SATURN_NOHUPOUT_SIZE_LIMIT_IN_BYTES);
					fc.truncate(TRUNCATE_SIZE);
				}
			} catch (FileNotFoundException e) {
				log.debug("File not found:", SystemEnvProperties.VIP_SATURN_LOG_OUTFILE, e);
			} catch (Throwable e) {
				log.debug("exception throws during handle saturn-executor-log.log", e);
			} finally {
				try {
					if (fc != null) {
						fc.close();
					}
				} catch (IOException e) {
					log.warn("exception throws during close file channel", e);
				}

				try {
					if (file != null) {
						file.close();
					}
				} catch (IOException e) {
					log.warn("exception throws during close file", e);
				}
			}
		}
	}
}
