package com.vip.saturn.job.internal.sharding;

/**
 * 
 * @author hebelala
 *
 */
public class GetDataStat {

	private String data;
	private int version;

	public GetDataStat() {
	}

	public GetDataStat(String data, int version) {
		this.data = data;
		this.version = version;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

}
