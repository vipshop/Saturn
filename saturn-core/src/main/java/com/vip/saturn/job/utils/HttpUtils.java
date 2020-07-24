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

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
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
				JsonElement message = JsonUtils.getJsonParser().parse(responseBody).getAsJsonObject().get("message");
				String errMsg = message == JsonNull.INSTANCE || message == null ? "" : message.getAsString();
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
				LogUtils.error(log, LogEvents.ExecutorEvent.COMMON, "Exception during httpclient closed.", e);
			}
		}
	}
}
