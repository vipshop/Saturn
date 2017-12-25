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

	private int count;
	private int error;

	public ZkStatistics() {
	}

	public ZkStatistics(int count, int error) {
		this.count = count;
		this.error = error;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getError() {
		return error;
	}

	public void setError(int error) {
		this.error = error;
	}
}
