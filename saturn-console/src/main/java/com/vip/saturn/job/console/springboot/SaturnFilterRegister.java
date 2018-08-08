package com.vip.saturn.job.console.springboot;

import com.vip.saturn.job.console.filter.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class SaturnFilterRegister {

	@Value("${authentication.enabled:false}")
	private boolean authenticationEnabled;

	@Bean
	public FilterRegistrationBean registerAuthenticationFilter() {
		AuthenticationFilter filter = new AuthenticationFilter();
		filter.setEnabled(authenticationEnabled);
		FilterRegistrationBean registration = new FilterRegistrationBean(filter);
		registration.addUrlPatterns("/console/*");
		registration.setOrder(0);
		return registration;
	}

}
