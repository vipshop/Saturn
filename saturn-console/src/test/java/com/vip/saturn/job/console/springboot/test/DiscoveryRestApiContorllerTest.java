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

package com.vip.saturn.job.console.springboot.test;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.console.AbstractSaturnConsoleTest;
import com.vip.saturn.job.console.controller.rest.DiscoveryRestApiController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(DiscoveryRestApiController.class)
public class DiscoveryRestApiContorllerTest extends AbstractSaturnConsoleTest {

	@Autowired
	private MockMvc mvc;

	@Test
	public void disoverSuccessfully() throws Exception {
		MvcResult result = mvc.perform(get("/rest/v1/discovery?namespace=mydomain")).andExpect(status().isOk())
				.andReturn();
		String body = result.getResponse().getContentAsString();
		Map<String, Object> resultMap = JSONObject.parseObject(body, Map.class);
		assertEquals("localhost:2181", resultMap.get("zkConnStr"));
		assertEquals("dev", resultMap.get("env"));
	}
}
