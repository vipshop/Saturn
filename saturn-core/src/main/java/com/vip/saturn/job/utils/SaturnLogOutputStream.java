package com.vip.saturn.job.utils;

import org.apache.commons.exec.LogOutputStream;
import org.slf4j.Logger;

/**
 * 捕获Shell作业的输出流。
 *
 * @author hebelala
 */
public class SaturnLogOutputStream extends LogOutputStream {

	public static final int LEVEL_INFO = 1;
	public static final int LEVEL_ERROR = 2;

	private Logger log;

	public SaturnLogOutputStream(Logger log, int level) {
		super(level);
		this.log = log;
	}

	@Override
	protected void processLine(String line, int logLevel) {
		if (logLevel == LEVEL_INFO) {
			log.info(line);
		} else if (logLevel == LEVEL_ERROR) {
			log.error(line);
		}
	}
}
