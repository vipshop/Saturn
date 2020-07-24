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

package com.vip.saturn.it.impl;

import com.vip.saturn.it.base.AbstractSaturnIT;
import com.vip.saturn.job.executor.Main;
import com.vip.saturn.job.executor.SaturnExecutorsNode;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.commons.exec.OS;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestartAndDumpIT extends AbstractSaturnIT {

	private static String PRG = "src/test/resources/script/restart/prg.sh";
	private static String OUTFILE = "src/test/resources/script/restart/outfile";

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopExecutorListGracefully();
		stopSaturnConsoleList();
	}

	@Test
	public void test() throws Exception {
		if (!OS.isFamilyUnix()) {
			return;
		}
		SystemEnvProperties.VIP_SATURN_ENABLE_EXEC_SCRIPT = true;
		SystemEnvProperties.VIP_SATURN_PRG = new File(PRG).getAbsolutePath();
		SystemEnvProperties.VIP_SATURN_LOG_OUTFILE = new File(OUTFILE).getAbsolutePath();
		try {
			File outfile = new File(OUTFILE);
			if (outfile.exists()) {
				assertThat(outfile.delete()).isTrue();
			}
			Main executor = startOneNewExecutorList();
			regCenter.persist(SaturnExecutorsNode.EXECUTORS_ROOT + "/" + executor.getExecutorName() + "/restart", "");
			Thread.sleep(1000L);
			String content = FileUtils.readFileToString(new File(OUTFILE));
			assertThat(content).isEqualTo("Hebe! Hebe! Hebe!");

			String dumpNodePath = SaturnExecutorsNode.EXECUTORS_ROOT + "/" + executor.getExecutorName() + "/dump";
			regCenter.persist(dumpNodePath, "");
			Thread.sleep(1000L);
			content = FileUtils.readFileToString(new File(OUTFILE));
			assertThat(content).isEqualTo("Hebe! Hebe! Hebe!Dump! Dump! Dump!");
			assertThat(regCenter.isExisted(dumpNodePath)).isFalse();
		} finally {
			SystemEnvProperties.VIP_SATURN_ENABLE_EXEC_SCRIPT = false;
			SystemEnvProperties.VIP_SATURN_PRG = null;
			SystemEnvProperties.VIP_SATURN_LOG_OUTFILE = null;
		}
	}

}
