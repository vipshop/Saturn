package com.vip.saturn.job.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

public class ResourceUtilsTest {

	@Test
	public void test() throws IOException {
		Properties props = ResourceUtils.getResource("properties/saturn-core-test.properties");
		assertThat(props.getProperty("build.version.test")).isEqualTo("saturn-dev-test");
	}

}
