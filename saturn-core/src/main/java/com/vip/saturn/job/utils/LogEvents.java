package com.vip.saturn.job.utils;

public class LogEvents {

	public static class ExecutorEvent {
		public static final String INIT = "EXECUTOR_INIT";

		public static final String GRACEFUL_SHUTDOWN = "EXECUTOR_SHUTDOWN_GRACEFULLY";

		public static final String SHUTDOWN = "EXECUTOR_SHUTDOWN";

		public static final String REINIT = "EXECUTOR_REINIT";
	}
}
