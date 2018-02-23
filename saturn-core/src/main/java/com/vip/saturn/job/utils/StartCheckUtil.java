package com.vip.saturn.job.utils;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class StartCheckUtil {
	private static final String OUT_FILE = System.getProperty("start.check.outfile");

	public enum StartCheckItem {
		ZK, PORT, UNIQUE, JOBKILL;
	}

	public enum CheckStatus {
		UNKNOWN, SUCCESS, ERROR;
	}

	private static Map<StartCheckItem, CheckStatus> checkResultMap = new EnumMap<StartCheckItem, CheckStatus>(
			StartCheckItem.class);

	public static void add2CheckList(StartCheckItem... items) {
		for (StartCheckItem item : items) {
			checkResultMap.put(item, CheckStatus.UNKNOWN);
		}
	}

	public static void setError(StartCheckItem item) {
		checkResultMap.put(item, CheckStatus.ERROR);
		doCheck();
	}

	public static void setOk(StartCheckItem item) {
		checkResultMap.put(item, CheckStatus.SUCCESS);
		doCheck();
	}

	private static void writeCheckResult(CheckStatus status) {
		System.out.println("start check result: [" + status + "]");// NOSONAR
		if (OUT_FILE == null) {
			return;
		}
		try (OutputStream fs = Files.newOutputStream(Paths.get(OUT_FILE))) {
			byte[] res = status.toString().getBytes("UTF-8");
			fs.write(res);
		} catch (Exception e) {// NOSONAR
			e.printStackTrace();// NOSONAR
		}
		checkResultMap.clear();
	}

	/**
	 * 进行启动项检查 如检测到一项失败，则表示启动失败；
	 */
	private static synchronized void doCheck() {
		Collection<CheckStatus> checkLists = checkResultMap.values();

		for (CheckStatus status : checkLists) {
			if (status == CheckStatus.ERROR) {
				writeCheckResult(CheckStatus.ERROR);
				return;
			}

			if (status == CheckStatus.UNKNOWN) {
				return;
			}
		}

		writeCheckResult(CheckStatus.SUCCESS);
	}
}
