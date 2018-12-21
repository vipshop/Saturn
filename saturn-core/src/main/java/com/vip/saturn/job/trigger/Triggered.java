package com.vip.saturn.job.trigger;

/**
 * @author hebelala
 */
public class Triggered {

	private boolean yes;
	private TriggeredData upStreamData;
	private TriggeredData downStreamData;

	public boolean isYes() {
		return yes;
	}

	public void setYes(boolean yes) {
		this.yes = yes;
	}

	public TriggeredData getUpStreamData() {
		return upStreamData;
	}

	public void setUpStreamData(TriggeredData upStreamData) {
		this.upStreamData = upStreamData;
	}

	public TriggeredData getDownStreamData() {
		return downStreamData;
	}

	public void setDownStreamData(TriggeredData downStreamData) {
		this.downStreamData = downStreamData;
	}

}
