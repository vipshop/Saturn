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

public class JobTypeImpl implements JobType {

	private String name;
	private Class<? extends Trigger> triggerClass;
	private Class<? extends AbstractElasticJob> handlerClass;

	private boolean isCron;
	private boolean isPassive;
	private boolean isJava;
	private boolean isShell;
	private boolean isAllowedShutdownGracefully;

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setTriggerClass(Class<? extends Trigger> triggerClass) {
		this.triggerClass = triggerClass;
	}

	@Override
	public Class<? extends Trigger> getTriggerClass() {
		return triggerClass;
	}

	public void setHandlerClass(Class<? extends AbstractElasticJob> handlerClass) {
		this.handlerClass = handlerClass;
	}

	@Override
	public Class<? extends AbstractElasticJob> getHandlerClass() {
		return handlerClass;
	}

	public void setCron(boolean cron) {
		isCron = cron;
	}

	@Override
	public boolean isCron() {
		return isCron;
	}

	public void setPassive(boolean passive) {
		isPassive = passive;
	}

	@Override
	public boolean isPassive() {
		return isPassive;
	}

	public void setJava(boolean java) {
		isJava = java;
	}

	@Override
	public boolean isJava() {
		return isJava;
	}

	public void setShell(boolean shell) {
		isShell = shell;
	}

	@Override
	public boolean isShell() {
		return isShell;
	}

	public void setAllowedShutdownGracefully(boolean allowedShutdownGracefully) {
		isAllowedShutdownGracefully = allowedShutdownGracefully;
	}

	@Override
	public boolean isAllowedShutdownGracefully() {
		return isAllowedShutdownGracefully;
	}
}
