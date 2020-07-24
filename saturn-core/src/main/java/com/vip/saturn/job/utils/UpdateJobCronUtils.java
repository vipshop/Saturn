/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

package com.vip.saturn.job.utils;

import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;
import com.vip.saturn.job.exception.SaturnJobException;
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

import java.net.ConnectException;
import java.util.Map;

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

			LogUtils.info(log, jobName, "update job cron of domain {} to url {}: {}, retry count: {}", namespace,
					targetUrl, cron, i);

			CloseableHttpClient httpClient = null;
			try {
				checkParameters(cron);

				Map<String, String> bodyEntity = Maps.newHashMap();
				if (customContext != null) {
					bodyEntity.putAll(customContext);
				}
				bodyEntity.put("cron", cron);
				String json = JsonUtils.toJson(bodyEntity, new TypeToken<Map<String, String>>() {
				}.getType());
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
				LogUtils.error(log, jobName, "SaturnJobException throws: {}", se.getMessage(), se);
				throw se;
			} catch (ConnectException e) {
				LogUtils.error(log, jobName, "Fail to connect to url:{}, throws: {}", targetUrl, e.getMessage(), e);
				if (i == size - 1) {
					throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, "no available console server", e);
				}
			} catch (Exception e) {
				LogUtils.error(log, jobName, "Other exception throws: {}", e.getMessage(), e);
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
