package com.vip.saturn.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;


@SpringBootApplication
public class DemoMain {

    public static ConfigurableApplicationContext ac;

	public static void main(String[] args) {
        ac = SpringApplication.run(DemoMain.class, args);
	}
}
