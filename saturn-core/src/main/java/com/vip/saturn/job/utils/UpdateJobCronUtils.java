package com.vip.saturn.job.utils;

import com.google.common.collect.Maps;
import com.vip.saturn.job.exception.SaturnJobException;
import java.net.ConnectException;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util for handling update job cron .
 *
 * @author timmy.hu
 */
public class UpdateJobCronUtils {

	private static final Logger log = LoggerFactory.getLogger(UpdateJobCronUtils.class);

	/**
	 * Send update job cron request to UpdateJobCron API in Console.
	 */
	public static void updateJobCron(String namespace, String jobName, String cron, Map<String, String> customContext)
			throws SaturnJobException {
		for (int i = 0, size = SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST.size(); i < size; i++) {

			String consoleUri = SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST.get(i);
			String targetUrl = consoleUri + "/rest/v1/" + namespace + "/jobs/" + jobName + "/cron";

			log.info("update job cron of domain {} to url {}: {}, retry count: {}", namespace, targetUrl, cron, i);

			CloseableHttpClient httpClient = null;
			try {
				checkParameters(cron);

				Map<String, String> bodyEntity = Maps.newHashMap();
				if (customContext != null) {
					bodyEntity.putAll(customContext);
				}
				bodyEntity.put("cron", cron);
				String json = JsonUtils.toJSON(bodyEntity);
				// prepare
				httpClient = HttpClientBuilder.create().build();
				HttpPut request = new HttpPut(targetUrl);
				final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000)
						.setSocketTimeout(10000).build();
				request.setConfig(requestConfig);
				StringEntity params = new StringEntity(json);
				request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
				request.setEntity(params);
				CloseableHttpResponse httpResponse = httpClient.execute(request);
				HttpUtils.handleResponse(httpResponse);
				return;
			} catch (SaturnJobException se) {
				log.error("SaturnJobException throws: {}", se);
				throw se;
			} catch (ConnectException e) {
				log.error("Fail to connect to url:{}, throws: {}", targetUrl, e);
				if (i == size - 1) {
					throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, "no available console server", e);
				}
			} catch (Exception e) {
				log.error("Other exception throws: {}", e);
				throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, e.getMessage(), e);
			} finally {
				HttpUtils.closeHttpClientQuietly(httpClient);
			}
		}
	}


	private static void checkParameters(String cron) throws SaturnJobException {
		if (StringUtils.isEmpty(cron)) {
			throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "cron cannot be null or empty.");
		}
	}

}
