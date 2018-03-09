package com.vip.saturn.job.console.springboot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@Profile("development")
public class SwaggerConfig {

	@Bean
	public Docket restAPIDocket() {
		return new Docket(DocumentationType.SWAGGER_2)
				.groupName("REST API")
				.select()
				.apis(RequestHandlerSelectors.basePackage("com.vip.saturn.job.console.controller.rest"))
				.paths(PathSelectors.any())
				.build();
	}

	@Bean
	public Docket guiAPIDocket() {
		return new Docket(DocumentationType.SWAGGER_2)
				.groupName("GUI API")
				.select()
				.apis(RequestHandlerSelectors.basePackage("com.vip.saturn.job.console.controller.gui"))
				.paths(PathSelectors.any())
				.build();
	}

}
