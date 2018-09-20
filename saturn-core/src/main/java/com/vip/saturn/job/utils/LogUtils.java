package com.vip.saturn.job.utils;

import org.slf4j.Logger;

public class LogUtils {

	/**
	 * @param logger    logger
	 * @param eventName 每一类业务定义的类型
	 * @param format    自定义的输出格式
	 * @param msg       自定义的输出内容
	 */
	public static void warn(Logger logger, String eventName, String format, Object... msg) {
		if (logger.isWarnEnabled()) {
			logger.warn(appendFormatAndEventName(format, eventName), msg);
		}
	}

	/**
	 * @param logger    logger
	 * @param eventName 每一类业务定义的类型
	 * @param format    自定义的输出格式
	 * @param msg       自定义的输出内容
	 */
	public static void info(Logger logger, String eventName, String format, Object... msg) {
		if (logger.isInfoEnabled()) {
			logger.info(appendFormatAndEventName(format, eventName), msg);
		}
	}

	/**
	 * @param logger    logger
	 * @param eventName 每一类业务定义的类型
	 * @param format    自定义的输出格式
	 * @param msg       自定义的输出内容
	 */
	public static void error(Logger logger, String eventName, String format, Object... msg) {
		if (logger.isErrorEnabled()) {
			logger.error(appendFormatAndEventName(format, eventName), msg);
		}
	}

	public static void debug(Logger logger, String eventName, String format, Object... msg) {
		if (logger.isDebugEnabled()) {
			logger.debug(appendFormatAndEventName(format, eventName), msg);
		}
	}

	private static String appendFormatAndEventName(String format, String eventName) {
		return "[" + eventName + "] msg=" + format;
	}

}
