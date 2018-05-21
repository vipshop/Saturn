package com.vip.saturn.job.console.utils;

import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HttpUtils {

	private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

	public static void closeHttpClientQuietly(CloseableHttpClient httpClient) {
		if (httpClient != null) {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.error("Exception during httpclient closed.", e);
			}
		}
	}
}
