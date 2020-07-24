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
