package com.vip.saturn.demo.job;

import com.vip.saturn.demo.service.DemoService;
import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class DemoJob extends AbstractSaturnJavaJob {

	private static final Logger log = LoggerFactory.getLogger(DemoJob.class);

	@Resource
	private DemoService demoService;

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) throws InterruptedException {
		log.info("{} is running, item is {}", jobName, shardItem);
		demoService.doing();
		return new SaturnJobReturn();
	}

}
