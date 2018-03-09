package com.vip.saturn.job.console.controller;

import javax.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Display swagger-ui.html in production.
 *
 * @author kfchu
 */
@Profile("!development")
@Controller
public class DisableSwaggerUIController {

	@GetMapping(value = "swagger-ui.html")
	public void getSwagger(HttpServletResponse httpResponse) {
		httpResponse.setStatus(HttpStatus.NOT_FOUND.value());
	}
}
