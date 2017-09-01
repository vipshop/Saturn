/**
 * 
 */
package com.vip.saturn.job.console.domain;

import java.io.Serializable;

/**
 * @author chembo.huang
 *
 */
public class ZkStatistics implements Serializable {

	private static final long serialVersionUID = 1L;

	private final int count;
	private final int error;

	public ZkStatistics(int count, int error) {
		this.count = count;
		this.error = error;
	}

	public int getCount() {
		return count;
	}

	public int getError() {
		return error;
	}

	@Override
	public String toString() {
		return "ZkStatistics [count=" + count + ", error=" + error + "]";
	}
}
