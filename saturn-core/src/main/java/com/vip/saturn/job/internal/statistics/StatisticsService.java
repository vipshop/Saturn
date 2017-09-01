/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.internal.statistics;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.internal.config.ConfigurationService;

/**
 * 作业统计信息服务.
 * 
 * 
 */
public class StatisticsService extends AbstractSaturnService {
	static Logger log = LoggerFactory.getLogger(StatisticsService.class);

	private ConfigurationService configService;

	private ScheduledExecutorService processCountExecutor;

	private ScheduledFuture<?> processCountJobFuture;

	private boolean isdown = false;

	public StatisticsService(final JobScheduler jobScheduler) {
		super(jobScheduler);
	}

	@Override
	public synchronized void start() {
		configService = jobScheduler.getConfigService();
		processCountExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			private AtomicInteger number = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable r) {
				StringBuilder name = new StringBuilder(jobConfiguration.getJobName()).append("-ProcessCount-Thread-")
						.append(number.incrementAndGet());
				Thread t = new Thread(r, name.toString());
				if (t.isDaemon()) {
					t.setDaemon(false);
				}
				if (t.getPriority() != Thread.NORM_PRIORITY) {
					t.setPriority(Thread.NORM_PRIORITY);
				}
				return t;
			}
		});
	}

	/**
	 * 开启或重启统计处理数据数量的作业.
	 */
	public synchronized void startProcessCountJob() {
		int processCountIntervalSeconds = configService.getProcessCountIntervalSeconds();
		if (processCountIntervalSeconds > 0) {

			if (processCountJobFuture != null) {
				processCountJobFuture.cancel(true);
				log.info("[{}] msg=Reschedule ProcessCountJob of the {} job, the processCountIntervalSeconds is {}",
						jobName, jobConfiguration.getJobName(), processCountIntervalSeconds);
			}
			processCountJobFuture = processCountExecutor.scheduleAtFixedRate(new ProcessCountJob(jobScheduler),
					new Random().nextInt(10), processCountIntervalSeconds, TimeUnit.SECONDS);

		} else { // don't count, reset to zero.
			if (processCountJobFuture != null) {
				log.info("[{}] msg=shutdown the task of reporting statistics data");
				processCountJobFuture.cancel(true);
				processCountJobFuture = null;
			}
		}
	}

	/**
	 * 停止统计处理数据数量的作业.
	 */
	public synchronized void stopProcessCountJob() {
		if (processCountJobFuture != null) {
			processCountJobFuture.cancel(true);
		}
		if (processCountExecutor != null) {
			processCountExecutor.shutdown();
		}
	}

	@Override
	public void shutdown() {
		if (isdown) {
			return;
		}
		isdown = true;
		stopProcessCountJob();
		ProcessCountStatistics.resetSuccessFailureCount(executorName, jobName);
	}

}
