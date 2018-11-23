package com.vip.saturn.job.console.service.impl.statistics.analyzer;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author timmy.hu
 */
public class ZkClusterDailyCountAnalyzer {

	private AtomicLong totalCount = new AtomicLong(0);

	private AtomicLong errorCount = new AtomicLong(0);

	public void incrTotalCount(long totalCount) {
		this.totalCount.addAndGet(totalCount);
	}

	public void incrErrorCount(long errorCount) {
		this.errorCount.addAndGet(errorCount);
	}

	public long getTotalCount() {
		return totalCount.get();
	}

	public long getErrorCount() {
		return errorCount.get();
	}
}
