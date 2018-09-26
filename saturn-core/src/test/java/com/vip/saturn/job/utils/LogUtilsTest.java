package com.vip.saturn.job.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.common.collect.Lists;
import org.junit.*;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class LogUtilsTest {

	private static TestLogAppender testLogAppender = new TestLogAppender();

	private static ch.qos.logback.classic.Logger log;

	@BeforeClass
	public static void before() {
		log = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LogUtilsTest.class);
		if (testLogAppender != null) {
			testLogAppender.clear();
		}

		log.addAppender(testLogAppender);
	}

	@AfterClass
	public static void after() {
		if (testLogAppender != null) {
			log.detachAppender(testLogAppender);
		}
	}

	@After
	public void afterTest() {
		testLogAppender.clear();
	}

	@Test
	public void info() {
		LogUtils.info(log, "event", "this is info");
		assertEquals("[event] msg=this is info", testLogAppender.getLastMessage());

		LogUtils.info(log, "event", "this is info {}", "arg1");
		assertEquals("[event] msg=this is info arg1", testLogAppender.getLastMessage());

		LogUtils.info(log, "event", "this is info {} {}", "arg1", "arg2");
		assertEquals("[event] msg=this is info arg1 arg2", testLogAppender.getLastMessage());

		LogUtils.info(log, "event", "this is info {} {}", "arg1", "arg2", new ClassNotFoundException("com.abc"));
		assertEquals("[event] msg=this is info arg1 arg2", testLogAppender.getLastMessage());
		assertEquals("com.abc", testLogAppender.getLastEvent().getThrowableProxy().getMessage());

		LogUtils.info(log, "event", "this is info {}", "arg1", new Error("com.def"));
		assertEquals("[event] msg=this is info arg1", testLogAppender.getLastMessage());
		assertEquals("com.def", testLogAppender.getLastEvent().getThrowableProxy().getMessage());

		LogUtils.info(log, "event", "this is info", new Exception("com.def"));
		assertEquals("[event] msg=this is info", testLogAppender.getLastMessage());
		assertEquals("com.def", testLogAppender.getLastEvent().getThrowableProxy().getMessage());
	}

	@Test
	public void debug() {
		LogUtils.debug(log, "event", "this is debug");
		assertEquals("[event] msg=this is debug", testLogAppender.getLastMessage());

		LogUtils.debug(log, "event", "this is debug {}", "arg1");
		assertEquals("[event] msg=this is debug arg1", testLogAppender.getLastMessage());

		LogUtils.debug(log, "event", "this is debug {} {}", "arg1", "arg2");
		assertEquals("[event] msg=this is debug arg1 arg2", testLogAppender.getLastMessage());

		LogUtils.debug(log, "event", "this is debug {} {}", "arg1", "arg2", new ClassNotFoundException("com.abc"));
		assertEquals("[event] msg=this is debug arg1 arg2", testLogAppender.getLastMessage());
		assertEquals("com.abc", testLogAppender.getLastEvent().getThrowableProxy().getMessage());

		LogUtils.debug(log, "event", "this is debug {}", "arg1", new Error("com.def"));
		assertEquals("[event] msg=this is debug arg1", testLogAppender.getLastMessage());
		assertEquals("com.def", testLogAppender.getLastEvent().getThrowableProxy().getMessage());

		LogUtils.debug(log, "event", "this is debug", new Exception("com.def"));
		assertEquals("[event] msg=this is debug", testLogAppender.getLastMessage());
		assertEquals("com.def", testLogAppender.getLastEvent().getThrowableProxy().getMessage());
	}

	@Test
	public void error() {
		LogUtils.error(log, "event", "this is error");
		assertEquals("[event] msg=this is error", testLogAppender.getLastMessage());

		LogUtils.error(log, "event", "this is error {}", "arg1");
		assertEquals("[event] msg=this is error arg1", testLogAppender.getLastMessage());

		LogUtils.error(log, "event", "this is error {} {}", "arg1", "arg2");
		assertEquals("[event] msg=this is error arg1 arg2", testLogAppender.getLastMessage());

		LogUtils.error(log, "event", "this is error {} {}", "arg1", "arg2", new ClassNotFoundException("com.abc"));
		assertEquals("[event] msg=this is error arg1 arg2", testLogAppender.getLastMessage());
		assertEquals("com.abc", testLogAppender.getLastEvent().getThrowableProxy().getMessage());

		LogUtils.error(log, "event", "this is error {}", "arg1", new Error("com.def"));
		assertEquals("[event] msg=this is error arg1", testLogAppender.getLastMessage());
		assertEquals("com.def", testLogAppender.getLastEvent().getThrowableProxy().getMessage());

		LogUtils.error(log, "event", "this is error", new Exception("com.def"));
		assertEquals("[event] msg=this is error", testLogAppender.getLastMessage());
		assertEquals("com.def", testLogAppender.getLastEvent().getThrowableProxy().getMessage());
	}

	@Test
	public void warn() {
		LogUtils.warn(log, "event", "this is warn");
		assertEquals("[event] msg=this is warn", testLogAppender.getLastMessage());

		LogUtils.warn(log, "event", "this is warn {}", "arg1");
		assertEquals("[event] msg=this is warn arg1", testLogAppender.getLastMessage());

		LogUtils.warn(log, "event", "this is warn {} {}", "arg1", "arg2");
		assertEquals("[event] msg=this is warn arg1 arg2", testLogAppender.getLastMessage());

		LogUtils.warn(log, "event", "this is warn {} {}", "arg1", "arg2", new ClassNotFoundException("com.abc"));
		assertEquals("[event] msg=this is warn arg1 arg2", testLogAppender.getLastMessage());
		assertEquals("com.abc", testLogAppender.getLastEvent().getThrowableProxy().getMessage());

		LogUtils.warn(log, "event", "this is warn {}", "arg1", new Error("com.def"));
		assertEquals("[event] msg=this is warn arg1", testLogAppender.getLastMessage());
		assertEquals("com.def", testLogAppender.getLastEvent().getThrowableProxy().getMessage());

		LogUtils.warn(log, "event", "this is warn", new Exception("com.def"));
		assertEquals("[event] msg=this is warn", testLogAppender.getLastMessage());
		assertEquals("com.def", testLogAppender.getLastEvent().getThrowableProxy().getMessage());
	}


	public static final class TestLogAppender extends AppenderBase<ILoggingEvent> {
		private List<ILoggingEvent> events = Lists.newArrayList();
		private List<String> messages = Lists.newArrayList();

		@Override
		public synchronized void doAppend(ILoggingEvent eventObject) {
			super.doAppend(eventObject);
			events.add(eventObject);
			messages.add(eventObject.getFormattedMessage());
		}

		@Override
		protected void append(ILoggingEvent eventObject) {
			events.add(eventObject);
			messages.add(eventObject.getFormattedMessage());
		}

		public void clear() {
			events.clear();
			messages.clear();
		}

		public ILoggingEvent getLastEvent() {
			if (events.size() == 0) {
				return null;
			}

			return events.get(events.size() - 1);
		}

		public List<ILoggingEvent> getEvents() {
			return events;
		}

		public List<String> getMessages() {
			return messages;
		}

		public String getLastMessage() {
			if (messages.size() == 0) {
				return null;
			}

			return messages.get(messages.size() - 1);
		}
	}
}