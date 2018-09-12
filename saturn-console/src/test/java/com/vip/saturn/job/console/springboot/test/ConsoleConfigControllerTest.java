package com.vip.saturn.job.console.springboot.test;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.console.AbstractSaturnConsoleTest;
import com.vip.saturn.job.console.controller.gui.ConsoleConfigController;
import com.vip.saturn.job.console.domain.JobConfigMeta;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(ConsoleConfigController.class)
public class ConsoleConfigControllerTest extends AbstractSaturnConsoleTest {

	@Autowired
	private MockMvc mvc;

	@Test
	public void testGetConfigMeta() throws Exception {
		MvcResult result = mvc.perform(get("/console/configs/console")).andExpect(status().isOk()).andReturn();
		String body = result.getResponse().getContentAsString();
		Map<String, Object> resultMap = JSONObject.parseObject(body, Map.class);
		Map<String, Object> objValue = (Map<String, Object>) resultMap.get("obj");
		assertEquals(5, objValue.size());
		List<JobConfigMeta> metas = (List<JobConfigMeta>) objValue.get("job_configs");
		assertTrue(metas.size() > 0);
		metas = (List<JobConfigMeta>) objValue.get("executor_configs");
		assertTrue(metas.size() > 0);
		metas = (List<JobConfigMeta>) objValue.get("cluster_configs");
		assertTrue(metas.size() > 0);
		metas = (List<JobConfigMeta>) objValue.get("console_configs");
		assertTrue(metas.size() > 0);
		metas = (List<JobConfigMeta>) objValue.get("other_configs");
		assertTrue(metas.size() > 0);
	}

	@Test
	public void testGetEnvConfig() throws Exception {
		MvcResult result = mvc.perform(get("/console/configs/console/env")).andExpect(status().isOk()).andReturn();
		String body = result.getResponse().getContentAsString();
		Map<String, Object> resultMap = JSONObject.parseObject(body, Map.class);
		String objValue = (String) resultMap.get("obj");
		assertEquals("dev", objValue);
	}
}