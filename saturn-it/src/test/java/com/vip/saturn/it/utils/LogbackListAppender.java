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

package com.vip.saturn.it.utils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * 在List中保存日志的Appender, 用于测试Logback的日志输出.
 *
 * 在测试开始前, 使用任意一种addToLogger()方法将此appender添加到需要侦听的logger中.
 *
 * @author calvin
 */
public class LogbackListAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

	private final List<ILoggingEvent> logs = new LinkedList<ILoggingEvent>();

	public LogbackListAppender() {
		start();
	}

	@Override
	protected void append(ILoggingEvent e) {
		logs.add(e);
	}

	/**
	 * 返回之前append的第一个log.
	 */
	public ILoggingEvent getFirstLog() {
		if (logs.isEmpty()) {
			return null;
		}
		return logs.get(0);
	}

	/**
	 * 返回之前append的第一个log的内容.
	 */
	public String getFirstMessage() {
		if (logs.isEmpty()) {
			return null;
		}
		return getFirstLog().getFormattedMessage();
	}

	/**
	 * 返回之前append的最后一个log.
	 */
	public ILoggingEvent getLastLog() {
		if (logs.isEmpty()) {
			return null;
		}
		return logs.get(logs.size() - 1);
	}

	/**
	 * 返回之前append的最后一个log的内容.
	 */
	public String getLastMessage() {
		if (logs.isEmpty()) {
			return null;
		}
		return getLastLog().getFormattedMessage();
	}

	/**
	 * 返回之前append的所有log.
	 */
	public List<ILoggingEvent> getAllLogs() {
		return logs;
	}

	/**
	 * 返回Log的数量。
	 */
	public int getLogsCount() {
		return logs.size();
	}

	/**
	 * 判断是否有log.
	 */
	public boolean isEmpty() {
		return logs.isEmpty();
	}

	/**
	 * 清除之前append的所有log.
	 */
	public void clearLogs() {
		logs.clear();
	}

	/**
	 * 将此appender添加到logger中.
	 */
	public void addToLogger(String loggerName) {
		Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
		logger.addAppender(this);
	}

	/**
	 * 将此appender添加到logger中.
	 */
	public void addToLogger(Class<?> loggerClass) {
		Logger logger = (Logger) LoggerFactory.getLogger(loggerClass);
		logger.addAppender(this);
	}

	/**
	 * 将此appender添加到root logger中.
	 */
	public void addToRootLogger() {
		Logger logger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		logger.addAppender(this);
	}

	/**
	 * 将此appender从logger中移除.
	 */
	public void removeFromLogger(String loggerName) {
		Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
		logger.detachAppender(this);
	}

	/**
	 * 将此appender从logger中移除.
	 */
	public void removeFromLogger(Class<?> loggerClass) {
		Logger logger = (Logger) LoggerFactory.getLogger(loggerClass);
		logger.detachAppender(this);
	}

	/**
	 * 将此appender从root logger中移除.
	 */
	public void removeFromRootLogger() {
		Logger logger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		logger.detachAppender(this);
	}

}