package com.vip.saturn.job.console.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Ignore;

@Ignore
public class DummyAppender extends AppenderBase<ILoggingEvent> {

	private List<ILoggingEvent> events = Lists.newArrayList();

	@Override
	public synchronized void doAppend(ILoggingEvent eventObject) {
		super.doAppend(eventObject);
		events.add(eventObject);
	}

	@Override
	protected void append(ILoggingEvent eventObject) {
		events.add(eventObject);
	}

	public void clear() {
		events.clear();
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
}
