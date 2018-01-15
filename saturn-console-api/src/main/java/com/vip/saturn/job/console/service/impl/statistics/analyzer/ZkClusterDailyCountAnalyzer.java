package com.vip.saturn.job.console.service.impl.statistics.analyzer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author timmy.hu
 */
public class ZkClusterDailyCountAnalyzer {

	private AtomicInteger totalCount = new AtomicInteger(0);

	private AtomicInteger errorCount = new AtomicInteger(0);

	public void incrTotalCount(int totalCount) {
		this.totalCount.addAndGet(totalCount);
	}

	public void incrErrorCount(int errorCount) {
		this.errorCount.addAndGet(errorCount);
	}

	public int getTotalCount() {
		return totalCount.get();
	}

	public int getErrorCount() {
		return errorCount.get();
	}
}
