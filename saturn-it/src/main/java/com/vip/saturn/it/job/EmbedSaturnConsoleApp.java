package com.vip.saturn.it.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * 
 * @author timmy.hu
 */
@SpringBootApplication
@ComponentScan({ "com.vip.saturn.job.console" })
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class })
@ImportResource({ "classpath:context/*Context-test.xml", "classpath:context/webMvcContext.xml" })
public class EmbedSaturnConsoleApp {

	protected static Logger logger = LoggerFactory.getLogger(EmbedSaturnConsoleApp.class);

	private static ApplicationContext ac = null;

	public static void main(String[] args) throws Exception {
		ac = SpringApplication.run(EmbedSaturnConsoleApp.class, args);
		DispatcherServlet dispatcherServlet = (DispatcherServlet) ac.getBean("dispatcherServlet");
		dispatcherServlet.setThrowExceptionIfNoHandlerFound(true);
	}

	public static void stop() {
		try {
			if (ac != null) {
				SpringApplication.exit(ac);
			}
		} catch (Throwable t) {
			logger.error("关闭Saturn Console的时候出错", t);
		}
	}

	@Bean
	public ServerProperties getServerProperties() {
		return new ServerCustomization();
	}
}

class ServerCustomization extends ServerProperties {

	@Override
	public void customize(ConfigurableEmbeddedServletContainer container) {
		super.customize(container);
		container.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/404"));
		container.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/500"));
		container.addErrorPages(new ErrorPage("/500"));
	}
}