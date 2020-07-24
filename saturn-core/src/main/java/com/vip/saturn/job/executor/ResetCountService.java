/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

/**
 *
 */
package com.vip.saturn.job.executor;

import com.vip.saturn.job.internal.statistics.ProcessCountResetTask;
import com.vip.saturn.job.utils.LogEvents;
import com.vip.saturn.job.utils.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;

/**
 * @author chembo.huang
 */
public class ResetCountService {
	static Logger log = LoggerFactory.getLogger(ResetCountService.class);

	private Timer countResetTimer;
	private ProcessCountResetTask countResetTask;

	public ResetCountService(String executorName) {
		countResetTimer = new Timer(executorName + "-reset-count-at-midnight");
		countResetTask = new ProcessCountResetTask(executorName);
	}

	// 每天临晨零点清理统计信息
	public void startRestCountTimer() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0); // 凌晨0点
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		Date date = calendar.getTime();

		// 避免任务立即执行
		if (date.before(new Date())) {
			Calendar startDT = Calendar.getInstance();
			startDT.setTime(date);
			startDT.add(Calendar.DAY_OF_MONTH, 1);
			date = startDT.getTime();
		}
		countResetTimer.schedule(countResetTask, date, 24 * 60 * 60 * 1000L); // 时间间隔(一天)
		LogUtils.info(log, LogEvents.ExecutorEvent.INIT, "start the task of resetting statistics data");
	}

	public void shutdownRestCountTimer() {
		countResetTimer.cancel();
	}
}
