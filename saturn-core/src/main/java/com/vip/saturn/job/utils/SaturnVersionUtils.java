/**
 * 
 */
package com.vip.saturn.job.utils;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @author timmy.hu
 *
 */
public class SaturnVersionUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(SaturnVersionUtils.class);

	private SaturnVersionUtils() {

	}

	public static String getVersion() {
		try {
			Properties props = ResourceUtils.getResource("properties/saturn-core.properties");
			if (props != null) {
				String version = props.getProperty("build.version");
				if (!Strings.isNullOrEmpty(version)) {
					return version;
				} else {
					LOGGER.error("the build.version property is not exists");
				}
			} else {
				LOGGER.error("the saturn-core.properties file is not exists");
			}
			return null;
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			return null;
		}
	}
}
