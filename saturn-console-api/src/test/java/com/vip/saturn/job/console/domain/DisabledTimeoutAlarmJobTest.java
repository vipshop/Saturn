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
package com.vip.saturn.job.console.domain;

import org.junit.Assert;
import org.junit.Test;

public class DisabledTimeoutAlarmJobTest {

	/**
	 * 测试equals方法
	 */
	@Test
	public void testEquals() {
		DisabledTimeoutAlarmJob job1 = new DisabledTimeoutAlarmJob("jobName1", "domain1", "nns1", "1");
		DisabledTimeoutAlarmJob job2 = new DisabledTimeoutAlarmJob("jobName2", "domain1", "nns2", "2");
		Assert.assertFalse(job1.equals(job2));
		job2.setJobName("jobName1");
		Assert.assertTrue(job1.equals(job2));
	}

}
