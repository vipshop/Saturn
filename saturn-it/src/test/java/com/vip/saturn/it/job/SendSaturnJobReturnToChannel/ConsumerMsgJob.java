/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

package com.vip.saturn.it.job.SendSaturnJobReturnToChannel;

import com.vip.saturn.job.AbstractSaturnMsgJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.msg.MsgHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
