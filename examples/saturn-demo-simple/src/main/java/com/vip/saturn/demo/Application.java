package com.vip.saturn.demo;

import com.vip.saturn.job.application.SaturnApplication;

public class Application implements SaturnApplication {

	public void init() {
		System.out.println("init...");
	}

	public void destroy() {
		System.out.println("destroy...");
	}

	public <J> J getJobInstance(Class<J> jobClass) {
		return null;
	}
}
