package com.vip.saturn.job.trigger;

import com.vip.saturn.job.basic.AbstractElasticJob;
import com.vip.saturn.job.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hebelala
 */
public abstract class AbstractTrigger implements Trigger {

	protected Logger log = LoggerFactory.getLogger(getClass());

	protected AbstractElasticJob job;

	public void init(AbstractElasticJob job) {
		this.job = job;
	}

	@Override
	public Triggered createTriggered(boolean yes, String upStreamDataStr) {
		Triggered triggered = new Triggered();
		triggered.setYes(yes);
		triggered.setUpStreamData(JsonUtils.fromJson(upStreamDataStr, TriggeredData.class));
		triggered.setDownStreamData(new TriggeredData());
		return triggered;
	}

	@Override
	public String serializeDownStreamData(Triggered triggered) {
		return JsonUtils.toJson(triggered.getDownStreamData());
	}
}
