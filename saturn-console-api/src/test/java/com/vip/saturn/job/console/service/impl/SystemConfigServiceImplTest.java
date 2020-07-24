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

package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import com.vip.saturn.job.console.mybatis.service.SystemConfig4SqlService;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SystemConfigServiceImplTest {

	@Mock
	private SystemConfig4SqlService systemConfig4SqlService;

	@InjectMocks
	private SystemConfigServiceImpl systemConfigService;

	@Test
	public void testGetValueByPrefix() {
		List<SystemConfig> configs = Lists.newArrayList();
		configs.add(initSystemConfig("test_1", "value_1"));
		configs.add(initSystemConfig("test_2", "value_2"));


		when(systemConfig4SqlService.selectByLastly()).thenReturn(configs);

		systemConfigService.reload();

		List<String> result = systemConfigService.getValuesByPrefix("test");

		assertEquals(2, result.size());

		int count = 0;
		for (String value : result) {
			if (value.equals("value_1") || value.equals("value_2")) {
				count++;
			} else {
				fail("should not come here");
			}
		}

		assertEquals(2, count);
	}

	private SystemConfig initSystemConfig(String property, String value) {
		SystemConfig systemConfig = new SystemConfig();
		systemConfig.setProperty(property);
		systemConfig.setValue(value);
		return systemConfig;
	}

}