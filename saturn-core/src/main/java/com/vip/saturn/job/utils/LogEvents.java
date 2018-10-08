package com.vip.saturn.job.utils;

public class LogEvents {

	public static class ExecutorEvent {

		public static final String VERSION_UPGRADE = "VERSION_UPGRADE";

		public static final String INIT = "EXECUTOR_INIT";

		public static final String GRACEFUL_SHUTDOWN = "EXECUTOR_SHUTDOWN_GRACEFULLY";

		public static final String SHUTDOWN = "EXECUTOR_SHUTDOWN";

		public static final String INIT_OR_SHUTDOWN = "EXECUTOR_INIT_OR_SHUTDOWN";

		public static final String REINIT = "EXECUTOR_REINIT";

		public static final String RESTART = "EXECUTOR_RESTART";

		public static final String DUMP = "EXECUTOR_DUMP";

		public static final String COMMON = "COMMON";

		public static final String FORCE_STOP = "FORCE_STOP";

	}
}
