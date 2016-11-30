package com.vip.saturn.job.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.Test;

public class ResourceUtilsTest {

	@Test
	public void test() {
		final Properties props = ResourceUtils
				.getResource("properties/saturn-core-test.properties");
		assertThat( props.getProperty("build.version.test")).isEqualTo("saturn-dev-test");
		
	}

}
