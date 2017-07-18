package com.vip.saturn.it;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.vip.saturn.it.job.EmbedSaturnConsoleApp;

/**
 * 启动控制台
 * 
 * 需要用到控制台的测试IT类，需要继承本类
 * 
 * @author timmy.hu
 */
public class AbstractSaturnConsoleIT extends SaturnAutoBasic {

	@BeforeClass
	public static void beforeClass() throws Exception {
		initSysEnv();
		initZK();
		startConsole();
	}

	@AfterClass
	public static void afterClass() throws IOException, InterruptedException {
		stopConsole();
		regCenter.close();
		nestedZkUtils.stopServer();
	}

}
