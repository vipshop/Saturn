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

package com.vip.saturn.job.basic;

import com.vip.saturn.job.trigger.Trigger;

public class JobTypeBuilder {

	public static JobTypeBuilder newBuilder() {
		return new JobTypeBuilder();
	}

	private JobTypeImpl jobType;

	public JobTypeBuilder() {
		this.jobType = new JobTypeImpl();
	}

	public JobTypeBuilder name(String name) {
		jobType.setName(name);
		return this;
	}

	public JobTypeBuilder triggerClass(Class<? extends Trigger> triggerClass) {
		jobType.setTriggerClass(triggerClass);
		return this;
	}

	public JobTypeBuilder handlerClass(Class<? extends AbstractElasticJob> handlerClass) {
		jobType.setHandlerClass(handlerClass);
		return this;
	}

	public JobTypeBuilder cron() {
		jobType.setCron(true);
		return this;
	}

	public JobTypeBuilder passive() {
		jobType.setPassive(true);
		return this;
	}

	public JobTypeBuilder java() {
		jobType.setJava(true);
		return this;
	}

	public JobTypeBuilder shell() {
		jobType.setShell(true);
		return this;
	}

	public JobTypeBuilder allowedShutdownGracefully() {
		jobType.setAllowedShutdownGracefully(true);
		return this;
	}

	public JobType build() {
		return jobType;
	}

}
