/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

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
