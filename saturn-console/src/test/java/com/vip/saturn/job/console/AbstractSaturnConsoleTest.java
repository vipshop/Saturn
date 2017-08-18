package com.vip.saturn.job.console;

import java.io.IOException;

import com.vip.saturn.job.console.springboot.SaturnConsoleApp;
import org.junit.AfterClass;
import org.junit.BeforeClass;

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

}
