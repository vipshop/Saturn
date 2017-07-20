package com.vip.saturn.job.console;

import org.junit.BeforeClass;

public abstract class AbstractSaturnConsoleTest {
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		System.setProperty("db.profiles.active", "h2");
	}

}
