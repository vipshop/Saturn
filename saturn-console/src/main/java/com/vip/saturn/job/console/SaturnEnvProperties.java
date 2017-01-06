package com.vip.saturn.job.console;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author xiaopeng.he
 *
 */
public final class SaturnEnvProperties {

	protected static Logger log = LoggerFactory.getLogger(SaturnEnvProperties.class);
	private SaturnEnvProperties() {}
	
	/** zk注册中心	 */
	public static String VIP_SATURN_ZK_CONNECTION;
	
	/** 指定注册中心地址*/
	public static String REG_CENTER_VALUE;
	
	/** 指定注册中心地址配置json文件*/
	public static String REG_CENTER_JSON_FILE;
	
	static {
		REG_CENTER_JSON_FILE = System.getProperty("REG_CENTER_JSON_PATH", System.getenv("REG_CENTER_JSON_PATH"));
		if (null != REG_CENTER_JSON_FILE) {
		}
		REG_CENTER_VALUE = System.getProperty("REG_CENTER_PROPERTY", System.getenv("REG_CENTER_PROPERTY"));
		if (null != REG_CENTER_VALUE) {
			REG_CENTER_VALUE = new String(REG_CENTER_VALUE.getBytes(), Charset.forName("UTF-8"));
		}
		VIP_SATURN_ZK_CONNECTION = System.getProperty("VIP_SATURN_ZK_CONNECTION", System.getenv("VIP_SATURN_ZK_CONNECTION"));
	}
}
