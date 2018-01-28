/**
 *
 */
package com.vip.saturn.job.executor;

import com.vip.saturn.job.internal.statistics.ProcessCountResetTask;
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
		log.info("msg=start the task of resetting statistics data");
	}

	public void shutdownRestCountTimer() {
		countResetTimer.cancel();
	}
}
