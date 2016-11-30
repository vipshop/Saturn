/**
 * 
 */
package com.vip.saturn.job.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author chembo.huang
 *
 */
public class ResourceUtils {
	static Logger log = LoggerFactory.getLogger(ResourceUtils.class);

	public static Properties getResource(String resource) {
		Properties props = new Properties();
		try (InputStream is = ResourceUtils.class.getClassLoader().getResourceAsStream(resource)){
			if (is != null) {
				props.load(is);
			}
		} catch (IOException e) {
			log.error("msg=" + e.getMessage(),e);
		} 
		return props;
	}
}
