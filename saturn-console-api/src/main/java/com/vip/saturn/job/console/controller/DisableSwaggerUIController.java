package com.vip.saturn.job.console.controller;

import javax.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Display swagger-ui.html in production.
 * -Dspring.profiles.active=development
 * 放开 swagger-ui 地址：http://localhost:9998/swagger-ui.html
 *
 * @author kfchu
 */
//@Profile 标签用于开发、测试、 上线环境的切换
@Profile("!development")
@Controller
public class DisableSwaggerUIController {

	@GetMapping(value = "swagger-ui.html")
	public void getSwagger(HttpServletResponse httpResponse) {
		httpResponse.setStatus(HttpStatus.NOT_FOUND.value());
	}
}
