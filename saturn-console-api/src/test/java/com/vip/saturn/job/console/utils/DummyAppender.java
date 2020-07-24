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
