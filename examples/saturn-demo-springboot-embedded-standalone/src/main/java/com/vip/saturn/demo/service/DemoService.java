package com.vip.saturn.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DemoService {

	private static final Logger log = LoggerFactory.getLogger(DemoService.class);

	public void doing() {
		log.info("DemoService is doing...");
	}

}
