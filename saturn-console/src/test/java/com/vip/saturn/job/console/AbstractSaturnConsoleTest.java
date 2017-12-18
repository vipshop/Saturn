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
