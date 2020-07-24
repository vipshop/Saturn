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

package com.vip.saturn.job.utils;

import org.slf4j.Logger;

public class LogUtils {

	private static final String FORMAT_FOR_LOG = "[{}] msg={}";

	private LogUtils() {
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param msg 日志内容
	 */
	public static void debug(Logger logger, String eventName, String msg) {
		logger.debug(FORMAT_FOR_LOG, eventName, msg);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param format 日志内容格式
	 * @param arg 日志内容参数
	 */
	public static void debug(Logger logger, String eventName, String format, Object arg) {
		// 因为debug日志很少打开，所以先判断，如果打开，然后才拼接format
		if (logger.isDebugEnabled()) {
			logger.debug(constructFormatOrMsg(eventName, format), arg);
		}
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param format 日志内容格式
	 * @param arg1 日志内容参数1
	 * @param arg2 日志内容参数2
	 */
	public static void debug(Logger logger, String eventName, String format, Object arg1, Object arg2) {
		// 因为debug日志很少打开，所以先判断，如果打开，然后才拼接format
		if (logger.isDebugEnabled()) {
			logger.debug(constructFormatOrMsg(eventName, format), arg1, arg2);
		}
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param format 日志内容格式
	 * @param arguments 日志内容参数
	 */
	public static void debug(Logger logger, String eventName, String format, Object... arguments) {
		// 因为debug日志很少打开，所以先判断，如果打开，然后才拼接format
		if (logger.isDebugEnabled()) {
			logger.debug(constructFormatOrMsg(eventName, format), arguments);
		}
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param msg 日志内容
	 * @param t 异常
	 */
	public static void debug(Logger logger, String eventName, String msg, Throwable t) {
		// 因为debug日志很少打开，所以先判断，如果打开，然后才拼接format
		if (logger.isDebugEnabled()) {
			logger.debug(constructFormatOrMsg(eventName, msg), t);
		}
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param msg 日志内容
	 */
	public static void info(Logger logger, String eventName, String msg) {
		logger.info(FORMAT_FOR_LOG, eventName, msg);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param format 日志内容格式
	 * @param arg 日志内容参数
	 */
	public static void info(Logger logger, String eventName, String format, Object arg) {
		logger.info(constructFormatOrMsg(eventName, format), arg);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param format 日志内容格式
	 * @param arg1 日志内容参数1
	 * @param arg2 日志内容参数2
	 */
	public static void info(Logger logger, String eventName, String format, Object arg1, Object arg2) {
		logger.info(constructFormatOrMsg(eventName, format), arg1, arg2);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param format 日志内容格式
	 * @param arguments 日志内容参数
	 */
	public static void info(Logger logger, String eventName, String format, Object... arguments) {
		logger.info(constructFormatOrMsg(eventName, format), arguments);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param msg 日志内容
	 * @param t 异常
	 */
	public static void info(Logger logger, String eventName, String msg, Throwable t) {
		logger.info(constructFormatOrMsg(eventName, msg), t);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param msg 日志内容
	 */
	public static void warn(Logger logger, String eventName, String msg) {
		logger.warn(FORMAT_FOR_LOG, eventName, msg);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param format 日志内容格式
	 * @param arg 日志内容参数
	 */
	public static void warn(Logger logger, String eventName, String format, Object arg) {
		logger.warn(constructFormatOrMsg(eventName, format), arg);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param format 日志内容格式
	 * @param arg1 日志内容参数1
	 * @param arg2 日志内容参数2
	 */
	public static void warn(Logger logger, String eventName, String format, Object arg1, Object arg2) {
		logger.warn(constructFormatOrMsg(eventName, format), arg1, arg2);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param format 日志内容格式
	 * @param arguments 日志内容参数
	 */
	public static void warn(Logger logger, String eventName, String format, Object... arguments) {
		logger.warn(constructFormatOrMsg(eventName, format), arguments);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param msg 日志内容
	 * @param t 异常
	 */
	public static void warn(Logger logger, String eventName, String msg, Throwable t) {
		logger.warn(constructFormatOrMsg(eventName, msg), t);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param msg 日志内容
	 */
	public static void error(Logger logger, String eventName, String msg) {
		logger.error(FORMAT_FOR_LOG, eventName, msg);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param format 日志内容格式
	 * @param arg 日志内容参数
	 */
	public static void error(Logger logger, String eventName, String format, Object arg) {
		logger.error(constructFormatOrMsg(eventName, format), arg);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param format 日志内容格式
	 * @param arg1 日志内容参数1
	 * @param arg2 日志内容参数2
	 */
	public static void error(Logger logger, String eventName, String format, Object arg1, Object arg2) {
		logger.error(constructFormatOrMsg(eventName, format), arg1, arg2);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param format 日志内容格式
	 * @param arguments 日志内容参数
	 */
	public static void error(Logger logger, String eventName, String format, Object... arguments) {
		logger.error(constructFormatOrMsg(eventName, format), arguments);
	}

	/**
	 * @param logger slf4j logger
	 * @param eventName 事件名
	 * @param msg 日志内容
	 * @param t 异常
	 */
	public static void error(Logger logger, String eventName, String msg, Throwable t) {
		logger.error(constructFormatOrMsg(eventName, msg), t);
	}

	private static String constructFormatOrMsg(String eventName, String formatOrMsg) {
		return new StringBuilder("[").append(eventName).append("] msg=").append(formatOrMsg).toString();
	}
}
