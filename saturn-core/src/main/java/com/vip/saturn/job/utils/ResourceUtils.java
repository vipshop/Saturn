/**
 * 
 */
package com.vip.saturn.job.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author chembo.huang
 *
 */
public class ResourceUtils {

	public static Properties getResource(String resource) throws IOException {
		Properties props = new Properties();
		InputStream is = null;
		try {
			is = ResourceUtils.class.getClassLoader().getResourceAsStream(resource);
			if (is != null) {
				props.load(is);
			}
		} finally {
			if (is != null) {
				is.close();
			}
		}
		return props;
	}
}
