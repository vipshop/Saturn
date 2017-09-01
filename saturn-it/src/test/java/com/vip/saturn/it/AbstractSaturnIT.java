package com.vip.saturn.it;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;

/**
 * IT flow
 * @author hebelala
 */
public class AbstractSaturnIT extends SaturnAutoBasic {

	@BeforeClass
	public static void beforeClass() throws Exception {
		initSysEnv();
		initZK();
		prepare4Console();
	}

	@AfterClass
	public static void afterClass() throws IOException, InterruptedException {
		regCenter.close();
		nestedZkUtils.stopServer();
		stopConsoleDb();
	}

}
