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
package com.vip.saturn.job.msg;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 支持20个延时等级的投递，默认情况按照重试次数依次使用不同延时来进行消息再投递；用户亦可修改每次重试的延时。
 */
public enum SaturnDelayedLevel {
	DELAYED_LEVEL_5S(1, 5, TimeUnit.SECONDS),

	DELAYED_LEVEL_10S(2, 10, TimeUnit.SECONDS),

	DELAYED_LEVEL_15S(3, 15, TimeUnit.SECONDS),

	DELAYED_LEVEL_30S(4, 30, TimeUnit.SECONDS),

	DELAYED_LEVEL_45S(5, 45, TimeUnit.SECONDS),

	DELAYED_LEVEL_1M(6, 1, TimeUnit.MINUTES),

	DELAYED_LEVEL_2M(7, 2, TimeUnit.MINUTES),

	DELAYED_LEVEL_3M(8, 3, TimeUnit.MINUTES),

	DELAYED_LEVEL_4M(9, 4, TimeUnit.MINUTES),

	DELAYED_LEVEL_5M(10, 5, TimeUnit.MINUTES),

	DELAYED_LEVEL_6M(11, 6, TimeUnit.MINUTES),

	DELAYED_LEVEL_7M(12, 7, TimeUnit.MINUTES),

	DELAYED_LEVEL_8M(13, 8, TimeUnit.MINUTES),

	DELAYED_LEVEL_9M(14, 9, TimeUnit.MINUTES),

	DELAYED_LEVEL_10M(15, 10, TimeUnit.MINUTES),

	DELAYED_LEVEL_20M(16, 20, TimeUnit.MINUTES),

	DELAYED_LEVEL_30M(17, 30, TimeUnit.MINUTES),

	DELAYED_LEVEL_45M(18, 45, TimeUnit.MINUTES),

	DELAYED_LEVEL_1H(19, 1, TimeUnit.HOURS),

	DELAYED_LEVEL_2H(20, 2, TimeUnit.HOURS),

	DELAYED_LEVEL_NULL(21, 0, TimeUnit.MILLISECONDS);

	private final int value;
	private final long durationMs;

	SaturnDelayedLevel(int value, long duration, TimeUnit unit) {
		this.value = value;
		this.durationMs = unit.toMillis(duration);
	}

	public static List<SaturnDelayedLevel> getAllDelayLevels() {
		SaturnDelayedLevel[] values = SaturnDelayedLevel.values();
		List<SaturnDelayedLevel> levels = new ArrayList<>(Arrays.asList(values));
		levels.remove(DELAYED_LEVEL_NULL);
		return levels;
	}

	public static SaturnDelayedLevel valueOf(int value) {
		SaturnDelayedLevel[] allLevels = values();
		if (value <= 0 || value > validLength()) {
			return DELAYED_LEVEL_NULL;
		}
		return allLevels[value - 1];
	}

	private static int validLength() {
		return values().length - 1;
	}

	public int getValue() {
		return value;
	}

	public long getDurationMs() {
		return durationMs;
	}

}
