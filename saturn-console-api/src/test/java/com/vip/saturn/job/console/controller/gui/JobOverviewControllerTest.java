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

package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.utils.PageableUtil;
import org.assertj.core.util.Lists;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class JobOverviewControllerTest {

	private TestJobOverviewController controller = new TestJobOverviewController();

	@Test
	public void getJobSubListByPage() {
		List<JobConfig> configs = Lists
				.newArrayList(buildJobConfig("job1"), buildJobConfig("job2"), buildJobConfig("job3"));

		List<JobConfig> result = controller.getJobSubListByPage(configs, PageableUtil.generatePageble(1, 2));
		assertEquals(2, result.size());
		assertEquals("job1", result.get(0).getJobName());
		assertEquals("job2", result.get(1).getJobName());

		result = controller.getJobSubListByPage(configs, PageableUtil.generatePageble(2, 2));
		assertEquals(1, result.size());
		assertEquals("job3", result.get(0).getJobName());

		result = controller.getJobSubListByPage(configs, PageableUtil.generatePageble(3, 2));
		assertEquals(0, result.size());

		result = controller.getJobSubListByPage(configs, PageableUtil.generatePageble(0, 2));
		assertEquals(2, result.size());
		assertEquals("job1", result.get(0).getJobName());
		assertEquals("job2", result.get(1).getJobName());

		result = controller.getJobSubListByPage(configs, PageableUtil.generatePageble(-1, 2));
		assertEquals(2, result.size());
		assertEquals("job1", result.get(0).getJobName());
		assertEquals("job2", result.get(1).getJobName());

		result = controller.getJobSubListByPage(configs, PageableUtil.generatePageble(1, 5));
		assertEquals(3, result.size());
		assertEquals("job1", result.get(0).getJobName());
		assertEquals("job2", result.get(1).getJobName());
		assertEquals("job3", result.get(2).getJobName());

		result = controller.getJobSubListByPage(configs, PageableUtil.generatePageble(1, 1));
		assertEquals(1, result.size());
		assertEquals("job1", result.get(0).getJobName());

		result = controller.getJobSubListByPage(configs, PageableUtil.generatePageble(2, 1));
		assertEquals(1, result.size());
		assertEquals("job2", result.get(0).getJobName());

		result = controller.getJobSubListByPage(configs, PageableUtil.generatePageble(1, -1));
		assertEquals(3, result.size());
	}

	private JobConfig buildJobConfig(String name) {
		JobConfig jobConfig = new JobConfig();
		jobConfig.setJobName(name);
		return jobConfig;
	}

	static final class TestJobOverviewController extends JobOverviewController {

	}

}