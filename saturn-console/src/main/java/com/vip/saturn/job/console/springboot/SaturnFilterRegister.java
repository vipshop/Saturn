package com.vip.saturn.job.console.springboot;

import com.vip.saturn.job.console.filter.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.CharacterEncodingFilter;

@Component
public class SaturnFilterRegister {

	@Value("${authentication.enabled:false}")
	private boolean authenticationEnabled;

	@Bean
	public FilterRegistrationBean registerEncodingFilter() {
		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding("UTF-8");
		filter.setForceEncoding(true);
		FilterRegistrationBean registration = new FilterRegistrationBean(filter);
		registration.addUrlPatterns("/*");
		registration.setOrder(0);
		return registration;
	}

	@Bean
	public FilterRegistrationBean registerAuthenticationFilter() {
		AuthenticationFilter filter = new AuthenticationFilter();
		filter.setEnabled(authenticationEnabled);
		FilterRegistrationBean registration = new FilterRegistrationBean(filter);
		registration.addUrlPatterns("/console/*");
		registration.setOrder(1);
		return registration;
	}


}
