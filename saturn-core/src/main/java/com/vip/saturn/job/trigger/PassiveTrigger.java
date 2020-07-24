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

public class PassiveTrigger extends AbstractTrigger {

	@Override
	public org.quartz.Trigger createQuartzTrigger() {
		return null;
	}

	@Override
	public boolean isInitialTriggered() {
		return false;
	}

	@Override
	public void enableJob() {
		int countTime = job.getConfigService().getJobConfiguration().getProcessCountIntervalSeconds();
		if (job.getJobScheduler().getPreviousConf().getProcessCountIntervalSeconds() != countTime) {
			job.getJobScheduler().getPreviousConf().setProcessCountIntervalSeconds(countTime);
			job.getJobScheduler().rescheduleProcessCountJob();
		}
	}

	@Override
	public void disableJob() {

	}

	@Override
	public void onResharding() {

	}

	@Override
	public boolean isFailoverSupported() {
		return job.getConfigService().isEnabledReport();
	}

}
