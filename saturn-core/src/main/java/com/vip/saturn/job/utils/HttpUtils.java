package com.vip.saturn.job.utils;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.exception.SaturnJobException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HttpUtils {

	private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

	public static void handleResponse(CloseableHttpResponse httpResponse) throws IOException, SaturnJobException {
		int status = httpResponse.getStatusLine().getStatusCode();

		if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {
			return;
		}

		if (status >= HttpStatus.SC_BAD_REQUEST && status <= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
			String responseBody = EntityUtils.toString(httpResponse.getEntity());
			if (StringUtils.isNotBlank(responseBody)) {
				String errMsg = JSONObject.parseObject(responseBody).getString("message");
				throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, errMsg);
			} else {
				throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, "internal server error");
			}
		} else {
			// if have unexpected status, then throw RuntimeException directly.
			String errMsg = "unexpected status returned from Saturn Server.";
			throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, errMsg);
		}
	}


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
