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

package com.vip.saturn.job.shell;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.exec.OS;
import org.junit.Test;

public class SaturnExecuteWatchdogTest {
	@Test
	public void assertGetPidByProcess() throws Exception {
		if (OS.isFamilyUnix()) {
			Process process = Runtime.getRuntime().exec("echo 123");
			long pid = SaturnExecuteWatchdog.getPidByProcess(process);
			assertThat(pid).isGreaterThan(0);
		}
	}
}
