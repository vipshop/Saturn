package com.vip.saturn.it.job.SendSaturnJobReturnToChannel;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.AbstractSaturnMsgJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.msg.MsgHolder;

/**
 * @author dylan.xue
 */
public class ConsumerMsgJob extends AbstractSaturnMsgJob {
	static Logger log = LoggerFactory.getLogger(ConsumerMsgJob.class);

	public static List<String> messageList = new ArrayList<>();

	@Override
	public SaturnJobReturn handleMsgJob(String jobName, Integer shardItem, String shardParam, MsgHolder msgHolder,
			SaturnJobExecutionContext shardingContext) throws InterruptedException {
		messageList.add(msgHolder.getPayload());
		log.info("messageList:" + messageList);
		return new SaturnJobReturn();
	}

}
