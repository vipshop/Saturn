package com.vip.saturn.job.utils;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.exception.SaturnJobException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.Map;

/**
 * Util for handling alarm.
 * <p>
 * Created by jeff.zhu on 17/05/2017.
 */
public class AlarmUtils {

	private static final Logger log = LoggerFactory.getLogger(AlarmUtils.class);

	/**
	 * Send alarm request to Alarm API in Console.
	 */
	public static void raiseAlarm(Map<String, Object> alarmInfo, String namespace) throws SaturnJobException {
		int size = SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST.size();
		for (int i = 0; i < size; i++) {

			String consoleUri = SystemEnvProperties.VIP_SATURN_CONSOLE_URI_LIST.get(i);
			String targetUrl = consoleUri + "/rest/v1/" + namespace + "/alarms/raise";

			log.info("raise alarm of domain {} to url {}: {}, retry count: {}", namespace, targetUrl, alarmInfo.toString(), i);

			CloseableHttpClient httpClient = null;
			try {
				checkParameters(alarmInfo);
				JSONObject json = new JSONObject(alarmInfo);
				// prepare
				httpClient = HttpClientBuilder.create().build();
				HttpPost request = new HttpPost(targetUrl);
				final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000)
						.setSocketTimeout(10000).build();
				request.setConfig(requestConfig);
				StringEntity params = new StringEntity(json.toString());
				request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
				request.setEntity(params);

				// send request
				CloseableHttpResponse httpResponse = httpClient.execute(request);
				// handle response
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

	private static void checkParameters(Map<String, Object> alarmInfo) throws SaturnJobException {
		if (alarmInfo == null) {
			throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "alarmInfo cannot be null.");
		}

		String level = (String) alarmInfo.get("level");
		if (StringUtils.isBlank(level)) {
			throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "level cannot be blank.");
		}

		String name = (String) alarmInfo.get("name");
		if (StringUtils.isBlank(name)) {
			throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "name cannot be blank.");
		}

		String title = (String) alarmInfo.get("title");
		if (StringUtils.isBlank(title)) {
			throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "title cannot be blank.");
		}

	}

}
