/**
 * vips Inc.
 * Copyright (c) 2016 All Rights Reserved.
 */   
package com.vip.saturn.job.console.exception;   


/**
 * 项目名称：saturn-job-console 
 * 创建时间：2016年5月24日 下午4:51:35   
 * @author yangjuanying  
 * @version 1.0   
 * @since JDK 1.7.0_05  
 * 文件名称：SaturnJobConsoleException.java  
 * 类说明：  通用异常类
 */
public class SaturnJobConsoleException extends Exception {

	/**  */
	private static final long serialVersionUID = -911821039567556368L;

	public SaturnJobConsoleException() {
	}

	public SaturnJobConsoleException(String message) {
		super(message);
	}

	public SaturnJobConsoleException(String message, Throwable cause) {
		super(message, cause);
	}

	public SaturnJobConsoleException(Throwable cause) {
		super(cause);
	}

	public SaturnJobConsoleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
  