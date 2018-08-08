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