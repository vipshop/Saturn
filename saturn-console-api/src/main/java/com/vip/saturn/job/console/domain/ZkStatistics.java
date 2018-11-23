/**
 *
 */
package com.vip.saturn.job.console.domain;

import java.io.Serializable;

/**
 * @author chembo.huang
 */
public class ZkStatistics implements Serializable {

	private static final long serialVersionUID = 1L;

	private long count;
	private long error;

	public ZkStatistics() {
	}

	public ZkStatistics(long count, long error) {
		this.count = count;
		this.error = error;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public long getError() {
		return error;
	}

	public void setError(long error) {
		this.error = error;
	}
}
