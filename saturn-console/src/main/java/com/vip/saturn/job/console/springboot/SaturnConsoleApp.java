package com.vip.saturn.job.console.springboot;

import org.apache.curator.test.TestingServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.HttpStatus;

/**
 * @author chembo.huang
 *
 */
@SpringBootApplication
@ComponentScan({ "com.vip.saturn.job.console" })
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
		JpaRepositoriesAutoConfiguration.class })
@ImportResource("classpath:context/*Context.xml")
public class SaturnConsoleApp {

	private static TestingServer embeddedZookeeper;

	public static void main(String[] args) throws Exception {
		startEmbeddedZkIfNeeded();

		SpringApplication.run(SaturnConsoleApp.class, args);
	}

	private static void startEmbeddedZkIfNeeded() throws Exception {
		if (Boolean.getBoolean("saturn.embeddedzk")) {
			embeddedZookeeper = new TestingServer(2182);
			embeddedZookeeper.start();
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