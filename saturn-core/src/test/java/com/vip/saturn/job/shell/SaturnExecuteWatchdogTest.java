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
