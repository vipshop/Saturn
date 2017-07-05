package com.vip.saturn.job.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.exception.SaturnJobException;

/**
 * Util for handling update job cron .
 * 
 * @author timmy.hu
 */
public class UpdateJobCronUtils {

	private final static Logger logger = LoggerFactory.getLogger(UpdateJobCronUtils.class);

	/**
	 * Send update job cron request to UpdateJobCron API in Console.
	 */
	public static void updateJobCron(String namespace, String jobName, String cron, Map<String, String> customContext) throws SaturnJobException {
		for (int i = 0, size = SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST.size(); i < size; i++) {

			String consoleUri = SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST.get(i);
			String targetUrl = consoleUri + "/rest/v1/" + namespace + "/jobs/" + jobName + "/cron";

			logger.info("update job cron of domain {} to url {}: {}", namespace, targetUrl, cron);

			CloseableHttpClient httpClient = null;
			try {
				checkParameters(cron);
				if (customContext == null) {
					customContext = new HashMap<String, String>();
				}
				customContext.put("cron", cron);
				String json = JsonUtils.toJSON(customContext);
				// prepare
				httpClient = HttpClientBuilder.create().build();
				HttpPut request = new HttpPut(targetUrl);
				final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(10000).build();
				request.setConfig(requestConfig);
				StringEntity params = new StringEntity(json);
				request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
				request.setEntity(params);
				CloseableHttpResponse httpResponse = httpClient.execute(request);
				handleResponse(httpResponse);
				return;
			} catch (SaturnJobException se) {
				logger.error("SaturnJobException throws: {}", se);
				if (i == size - 1) {
					throw se;
				}
			} catch (Exception e) {
				logger.error("Other exception throws: {}", e);
				if (i == size - 1) {
					throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, e.getMessage(), e);
				}
			} finally {
				if (httpClient != null) {
					try {
						httpClient.close();
					} catch (IOException e) {
						logger.error("Exception during httpclient closed.", e);
					}
				}
			}
		}
	}

	private static void handleResponse(CloseableHttpResponse httpResponse) throws IOException, SaturnJobException {
		int status = httpResponse.getStatusLine().getStatusCode();

		if (status == HttpStatus.SC_OK) {
			logger.info("update job cron successfully.");
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
			String errMsg = "unexpected status returned from Saturn Server.";
			throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, errMsg);
		}
	}

	private static void checkParameters(String cron) throws SaturnJobException {
		if (StringUtils.isEmpty(cron)) {
			throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "cron cannot be null or empty.");
		}
	}

	public static void main(String[] args) throws Exception {
		String namespace = "vip-monitor.vip.vip.com";
		String jobName = "aaaa";
		String cron = "0/15 * * * * ?";
		UpdateJobCronUtils.updateJobCron(namespace, jobName, cron, null);
	}
}
