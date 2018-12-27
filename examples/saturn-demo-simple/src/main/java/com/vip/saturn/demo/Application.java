package com.vip.saturn.demo;

import com.vip.saturn.job.application.AbstractSaturnApplication;

public class Application extends AbstractSaturnApplication {

	public void init() {
		System.out.println("init...");
	}

	public void destroy() {
		System.out.println("destroy...");
	}

}
