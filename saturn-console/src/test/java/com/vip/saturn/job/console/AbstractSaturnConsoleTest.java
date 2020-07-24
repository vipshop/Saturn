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

package com.vip.saturn.job.console;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.console.springboot.SaturnConsoleApp;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.test.web.servlet.MvcResult;

public abstract class AbstractSaturnConsoleTest {

	@BeforeClass
	public static void beforeClass() throws Exception {
		System.setProperty("db.profiles.active", "h2");
		System.setProperty("authentication.enabled", "false");
		SaturnConsoleApp.startEmbeddedDb();
	}

	@AfterClass
	public static void afterClass() throws IOException, InterruptedException {
		SaturnConsoleApp.stopEmbeddedDb();
	}

	protected String fetchErrorMessage(MvcResult result) throws UnsupportedEncodingException {
		return JSONObject.parseObject(result.getResponse().getContentAsString()).getString("message");
	}
}
