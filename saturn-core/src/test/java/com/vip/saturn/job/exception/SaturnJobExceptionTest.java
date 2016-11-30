package com.vip.saturn.job.exception;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

/**
 * 
 * @author xiaopeng.he
 *
 */
public class SaturnJobExceptionTest {

	@Test
	public void testConstant() {
		assertThat(SaturnJobException.CRON_VALID).isEqualTo(0);
		assertThat(SaturnJobException.JOB_NOT_FOUND).isEqualTo(1);
		assertThat(SaturnJobException.OUT_OF_ZK_LIMIT_MEMORY).isEqualTo(3);
		assertThat(SaturnJobException.JOBNAME_VALID).isEqualTo(4);
	}
	
	@Test
	public void testGet() {
		SaturnJobException saturnJobException = new SaturnJobException(SaturnJobException.CRON_VALID, "cron valid");
		assertThat(saturnJobException.getType()).isEqualTo(SaturnJobException.CRON_VALID);
		assertThat(saturnJobException.getMessage()).isEqualTo("cron valid");
	}
	
}
