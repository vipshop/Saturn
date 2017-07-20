package com.vip.saturn.job.console;

import java.util.Random;

import org.junit.BeforeClass;

public abstract class AbstractSaturnConsoleTest {
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		System.setProperty("db.profiles.active", "h2");
		System.setProperty("db.h2.dbname", "dbname" + new Random().nextInt(10000));
	}

}
