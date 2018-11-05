package com.vip.saturn.job.trigger;

import com.vip.saturn.job.basic.AbstractElasticJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTrigger implements Trigger {

	protected Logger log = LoggerFactory.getLogger(getClass());

	protected AbstractElasticJob job;

	public void init(AbstractElasticJob job) {
		this.job = job;
	}
}
