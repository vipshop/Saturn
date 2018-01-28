package com.vip.saturn.job.executor;

import com.vip.saturn.job.threads.SaturnThreadFactory;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Regular truncate saturn-nohup.out while the size is over specified limit.
 */
public class PeriodicTruncateNohupOutService {

	private static final Logger log = LoggerFactory.getLogger(PeriodicTruncateNohupOutService.class);

	private static final long TRUNCATE_SIZE = 0;

	private ScheduledExecutorService truncateLogService;

	public PeriodicTruncateNohupOutService(String executorName) {
		truncateLogService = Executors
				.newScheduledThreadPool(1, new SaturnThreadFactory(executorName + "-truncate-nohup-out-thread", false));
	}

	public void start() {
		log.info("start PeriodicTruncateNohupOutService");
		if (StringUtils.isBlank(SystemEnvProperties.VIP_SATURN_LOG_OUTFILE)) {
			log.warn("File path of saturn-nohup.out is not set, please check.");
			return;
		}

		truncateLogService.scheduleAtFixedRate(new TruncateLogRunnable(),
				new Random().nextInt(10), SystemEnvProperties.VIP_SATURN_CHECK_NOHUPOUT_SIZE_INTERVAL_IN_SEC,
				TimeUnit.SECONDS);
	}

	public void shutdown() {
		log.info("shutdown PeriodicTruncateNohupOutService");
		if (truncateLogService != null) {
			truncateLogService.shutdownNow();
		}
	}

	private class TruncateLogRunnable implements Runnable {

		@Override
		public void run() {
			try (RandomAccessFile file = new RandomAccessFile(SystemEnvProperties.VIP_SATURN_LOG_OUTFILE,
					"rw"); FileChannel fc = file.getChannel()) {
				if (fc.size() > SystemEnvProperties.VIP_SATURN_NOHUPOUT_SIZE_LIMIT_IN_BYTES) {
					log.info("truncate {} as size over {} bytes", SystemEnvProperties.VIP_SATURN_LOG_OUTFILE,
							SystemEnvProperties.VIP_SATURN_NOHUPOUT_SIZE_LIMIT_IN_BYTES);
					fc.truncate(TRUNCATE_SIZE);
				}
			} catch (FileNotFoundException e) {
				log.debug("saturn-nohup.out is not found:", SystemEnvProperties.VIP_SATURN_LOG_OUTFILE, e);
			} catch (Exception e) {
				log.debug("exception throws during handle saturn-nohup.out", e);
			}
		}
	}
}
