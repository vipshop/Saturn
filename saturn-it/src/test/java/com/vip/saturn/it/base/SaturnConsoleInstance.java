package com.vip.saturn.it.base;

import com.vip.saturn.job.console.springboot.SaturnConsoleApp;
import org.springframework.context.ApplicationContext;

/**
 * @author hebelala
 */
public class SaturnConsoleInstance {

	public ApplicationContext applicationContext;
	public int port;
	public String url;

	public SaturnConsoleInstance() {
	}

	public SaturnConsoleInstance(ApplicationContext applicationContext, int port, String url) {
		this.applicationContext = applicationContext;
		this.port = port;
		this.url = url;
	}

	public void stop() {
		SaturnConsoleApp.stop(applicationContext);
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
