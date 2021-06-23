package com.vip.saturn.demo;

import com.vip.saturn.embed.spring.EmbeddedSpringSaturnApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public EmbeddedSpringSaturnApplication embeddedSpringSaturnApplication() {
		EmbeddedSpringSaturnApplication embeddedSpringSaturnApplication = new EmbeddedSpringSaturnApplication();
		embeddedSpringSaturnApplication.setIgnoreExceptions(false);
		return embeddedSpringSaturnApplication;
	}

}
