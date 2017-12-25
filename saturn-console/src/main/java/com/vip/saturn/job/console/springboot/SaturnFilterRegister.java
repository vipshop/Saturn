package com.vip.saturn.job.console.springboot;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.vip.saturn.job.console.filter.RecordLastVisit;
import com.vip.saturn.job.console.filter.SaturnXssFilter;

@Component
public class SaturnFilterRegister {

	@Bean
	public FilterRegistrationBean registerEncodingFilter() {
		CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
		encodingFilter.setEncoding("UTF-8");
		encodingFilter.setForceEncoding(true);
		FilterRegistrationBean registration = new FilterRegistrationBean(encodingFilter);
		registration.addUrlPatterns("/*");
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}

	// @Bean
	// public FilterRegistrationBean registerSaturnXssFilter() {
	// SaturnXssFilter xssFilter = new SaturnXssFilter();
	// FilterRegistrationBean registration = new FilterRegistrationBean(xssFilter);
	// registration.addUrlPatterns("/*");
	// registration.setOrder(2);
	// return registration;
	// }

	@Bean
	public FilterRegistrationBean registerLastVisitFilter() {
		RecordLastVisit lastVisit = new RecordLastVisit();
		FilterRegistrationBean registration = new FilterRegistrationBean(lastVisit);
		registration.addUrlPatterns("/*");
		registration.setOrder(3);
		return registration;
	}

}
