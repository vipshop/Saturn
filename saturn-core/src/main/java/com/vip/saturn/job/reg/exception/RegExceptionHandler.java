/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.reg.exception;

import org.apache.zookeeper.KeeperException.ConnectionLossException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 抛出RegException的异常处理类.
 * 
 * 
 */
public final class RegExceptionHandler {

	private static Logger log = LoggerFactory.getLogger(RegExceptionHandler.class);

	private RegExceptionHandler() {
	}

	/**
	 * 处理掉中断和连接失效异常并继续抛出RegException.
	 * 
	 * @param cause 待处理的异常.
	 */
	public static void handleException(final Exception cause) {
		if (cause == null) {
			throw new RegException(null);
		}

		if (isIgnoredException(cause) || isIgnoredException(cause.getCause())) {
			log.debug("Elastic job: ignored exception for: {}", cause.getMessage());
		} else if (cause instanceof InterruptedException) {
			Thread.currentThread().interrupt();
		} else {
			throw new RegException(cause);
		}
	}

	private static boolean isIgnoredException(final Throwable cause) {
		if (null == cause) {
			return false;
		}
		return cause instanceof ConnectionLossException || cause instanceof NoNodeException
				|| cause instanceof NodeExistsException;
	}
}
