/**
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.vip.saturn.job.trigger;

import com.vip.saturn.job.basic.AbstractElasticJob;
import com.vip.saturn.job.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
