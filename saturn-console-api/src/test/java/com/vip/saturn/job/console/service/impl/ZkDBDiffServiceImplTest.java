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

import com.vip.saturn.job.console.domain.JobDiffInfo;
import org.assertj.core.util.Lists;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ZkDBDiffServiceImplTest {

	private ZkDBDiffServiceImpl zkDBDiffService = new ZkDBDiffServiceImpl();

	@Test
	public void diff() {
		List<JobDiffInfo.ConfigDiffInfo> diffInfoList = Lists.newArrayList();
		// case #1: empty string
		zkDBDiffService.diff("ns", "", "", diffInfoList);
		assertEquals(0, diffInfoList.size());
		diffInfoList.clear();
		// case #2: db is not empty but zk is empty
		zkDBDiffService.diff("ns", "123", "", diffInfoList);
		assertEquals(1, diffInfoList.size());
		diffInfoList.clear();
		// case #3: db is empty but zk is not empty
		zkDBDiffService.diff("ns", "", "123", diffInfoList);
		assertEquals(1, diffInfoList.size());
		diffInfoList.clear();
		// case #4: trim
		zkDBDiffService.diff("ns", "123", "123  ", diffInfoList);
		assertEquals(0, diffInfoList.size());
		diffInfoList.clear();
		// case #5: db and zk not equal string
		zkDBDiffService.diff("ns", "123", "456", diffInfoList);
		assertEquals(1, diffInfoList.size());
		diffInfoList.clear();
	}
}