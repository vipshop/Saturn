package com.vip.saturn.job.exception;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 
 * @author xiaopeng.he
 *
 */
public class SaturnJobExceptionTest {

	@Test
	public void testConstant() {
		assertThat(SaturnJobException.ILLEGAL_ARGUMENT).isEqualTo(0);
		assertThat(SaturnJobException.JOB_NOT_FOUND).isEqualTo(1);
		assertThat(SaturnJobException.OUT_OF_ZK_LIMIT_MEMORY).isEqualTo(3);
		assertThat(SaturnJobException.JOB_NAME_INVALID).isEqualTo(4);
	}

	@Test
	public void testGet() {
		SaturnJobException saturnJobException = new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT,
				"cron valid");
		assertThat(saturnJobException.getType()).isEqualTo(SaturnJobException.ILLEGAL_ARGUMENT);
		assertThat(saturnJobException.getMessage()).isEqualTo("cron valid");
	}

}
