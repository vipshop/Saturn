package com.vip.saturn.job.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import com.vip.saturn.job.utils.ScriptPidUtils;

public class ScriptPidUtilsTest {
	private static final String executorName = "executor_test_01";
	private static final String jobName = "jobName_test_01";
	private String executorFilePath = ScriptPidUtils.EXECUTINGPATH + ScriptPidUtils.FILESEPARATOR + executorName;
	private String jobFilePath = executorFilePath + ScriptPidUtils.FILESEPARATOR + jobName;

	@Before
	public void setup() {
		ScriptPidUtils.writePidToFile(executorName, jobName, 1, 101);
		ScriptPidUtils.writePidToFile(executorName, jobName, 2, 102);
	}

	@After
	public void tearDown() {
		ScriptPidUtils.removeAllPidFile(executorName, jobName, 1);
		ScriptPidUtils.removeAllPidFile(executorName, jobName, 2);

		File file = new File(executorFilePath);
		FileSystemUtils.deleteRecursively(file);
	}

	@Test
	public void assertGetSaturnExecutingHome() {
		File file = ScriptPidUtils.getSaturnExecutingHome();
		assertThat(file).exists();
		assertThat(file).canRead();
		assertThat(file).canWrite();
		assertThat(file).isDirectory();
	}

	@Test
	public void assertWritePidToFile() {
		ScriptPidUtils.writePidToFile(executorName, jobName, 3, 103);
		long pid = ScriptPidUtils.getFirstPidFromFile(executorName, jobName, "" + 3);
		assertThat(pid).isEqualTo(103);
	}

	@Test
	public void assertgetPidFromFile() {
		long pid = ScriptPidUtils.getFirstPidFromFile(executorName, jobName, "" + 1);
		assertThat(pid).isEqualTo(101);
	}

	@Test
	public void assertgetPidFromFile2() {
		String[] files = ScriptPidUtils.getItemsPaths(executorName, jobName);
		assertThat(files.length).isEqualTo(2);
		Arrays.asList(files).contains(jobFilePath + ScriptPidUtils.FILESEPARATOR + "1");
		Arrays.asList(files).contains(jobFilePath + ScriptPidUtils.FILESEPARATOR + "2");
	}

	@Test
	public void assertremovePidFile() {
		long pid = ScriptPidUtils.getFirstPidFromFile(executorName, jobName, "" + 1);
		assertThat(pid).isEqualTo(101);
		ScriptPidUtils.removeAllPidFile(executorName, jobName, 1);
		long pid2 = ScriptPidUtils.getFirstPidFromFile(executorName, jobName, "" + 1);
		assertThat(pid2).isEqualTo(-1);
	}

	@Test
	public void testFilterEnvInCmdStr() {
		Map<String, String> env = new HashMap<>();
		env.put("fool", "duff");
		env.put("ass", "david");
		env.put("LS_COLORS",
				"r=01;31:*.ace=01;31:*.zoo=01;31:*.cpio=01;31:*.7z=01;31:*.rz=01;31:*.jpg=01;35:*.jpeg=01;35:*.gif=01;35:*.bmp=01");
		String cmd = "In front of me is ${fool}, beside me is $ass. $fool likes ${ass}. this is ${nobody} i don't know.";
		String expected = "In front of me is duff, beside me is david. duff likes david. this is ${nobody} i don't know.";
		String result = ScriptPidUtils.filterEnvInCmdStr(env, cmd);
		assertThat(result).isEqualTo(expected);
	}

}
