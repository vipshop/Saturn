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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @author chembo.huang
 *
 */
@SpringBootApplication
@ComponentScan({ "com.vip.saturn.job.console" })
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
		JpaRepositoriesAutoConfiguration.class })
@ImportResource("classpath*:context/*Context.xml")
public class SaturnConsoleApp {

	private static TestingServer embeddedZookeeper;
	private static EmbeddedDatabase embeddedDatabase;

	public static void main(String[] args) throws Exception {
		if (Boolean.getBoolean("saturn.embeddedZk")) {
			startEmbeddedZk();
		}
		if (Boolean.getBoolean("saturn.embeddedDb")) {
			startEmbeddedDb();
		}

		SpringApplication.run(SaturnConsoleApp.class, args);
	}

	public static ApplicationContext start() {
		return SpringApplication.run(SaturnConsoleApp.class);
	}

	public static void stop(ApplicationContext applicationContext) {
		SpringApplication.exit(applicationContext);
	}

	public static void startEmbeddedZk() throws Exception {
		embeddedZookeeper = new TestingServer(2181);
		embeddedZookeeper.start();
	}

	public static void stopEmbeddedZk() throws IOException {
		if (embeddedZookeeper != null) {
			embeddedZookeeper.close();
		}
	}

	public static void startEmbeddedDb() throws SQLException {
		EmbeddedDatabaseBuilder embeddedDatabaseBuilder = new EmbeddedDatabaseBuilder();
		embeddedDatabaseBuilder.setType(EmbeddedDatabaseType.H2).addScript("classpath:db/h2/global.sql")
				.addScript("classpath:db/h2/schema.sql").addScript("classpath:db/h2/data.sql");
		String customSql = "classpath:db/h2/custom.sql";
		Resource resource = new DefaultResourceLoader().getResource(customSql);
		if (resource.exists()) {
			embeddedDatabaseBuilder.addScript(customSql);
		}
		embeddedDatabase = embeddedDatabaseBuilder.build();
		System.setProperty("db.profiles.active", "h2");
	}

	public static void stopEmbeddedDb() {
		if (embeddedDatabase != null) {
			embeddedDatabase.shutdown();
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