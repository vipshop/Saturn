package com.vip.saturn.it.utils;

import com.vip.saturn.job.utils.HttpUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * @author hebelala
 */
public class HttpClientUtils {

	public static HttpResponseEntity sendPostRequest(String url, Map<String, Object> params) throws IOException {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			StringBuilder lastUrl = new StringBuilder(url);
			if (params != null && params.size() > 0) {
				lastUrl.append('?');
				Iterator<Map.Entry<String, Object>> iterator = params.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, Object> next = iterator.next();
					lastUrl.append(next.getKey()).append('=').append(next.getValue()).append('&');
				}
				lastUrl.deleteCharAt(lastUrl.length() - 1);
			}
			HttpPost request = new HttpPost(lastUrl.toString());
			final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(10000)
					.build();
			request.setConfig(requestConfig);
			// send request
			CloseableHttpResponse response = httpClient.execute(request);

			return new HttpResponseEntity(response.getStatusLine().getStatusCode(),
					EntityUtils.toString(response.getEntity()));

		} finally {
			HttpUtils.closeHttpClientQuietly(httpClient);
		}
	}

	public static HttpResponseEntity sendPostRequestJson(String url, String jsonBody) throws IOException {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			HttpPost request = new HttpPost(url);
			final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(10000)
					.build();
			request.setConfig(requestConfig);
			StringEntity params = new StringEntity(jsonBody);
			request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			request.setEntity(params);
			// send request
			CloseableHttpResponse response = httpClient.execute(request);

			return new HttpResponseEntity(response.getStatusLine().getStatusCode(),
					EntityUtils.toString(response.getEntity()));

		} finally {
			HttpUtils.closeHttpClientQuietly(httpClient);
		}
	}

	public static HttpResponseEntity sendPutRequestJson(String url, String jsonBody) throws IOException {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			HttpPut request = new HttpPut(url);
			final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(10000)
					.build();
			request.setConfig(requestConfig);
			StringEntity params = new StringEntity(jsonBody);
			request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			request.setEntity(params);
			// send request
			CloseableHttpResponse response = httpClient.execute(request);

			return new HttpResponseEntity(response.getStatusLine().getStatusCode(),
					EntityUtils.toString(response.getEntity()));

		} finally {
			HttpUtils.closeHttpClientQuietly(httpClient);
		}
	}

	public static HttpResponseEntity sendGetRequestJson(String url) throws IOException {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(url);
			final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(10000)
					.build();
			request.setConfig(requestConfig);
			request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

			CloseableHttpResponse response = httpClient.execute(request);

			return new HttpResponseEntity(response.getStatusLine().getStatusCode(),
					EntityUtils.toString(response.getEntity()));
		} finally {
			HttpUtils.closeHttpClientQuietly(httpClient);
		}
	}

	public static HttpResponseEntity sendDeleteResponseJson(String url) throws IOException {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			HttpDelete request = new HttpDelete(url);
			final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(10000)
					.build();
			request.setConfig(requestConfig);
			request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			// send request
			CloseableHttpResponse response = httpClient.execute(request);

			return new HttpResponseEntity(response.getStatusLine().getStatusCode(),
					EntityUtils.toString(response.getEntity()));

		} finally {
			HttpUtils.closeHttpClientQuietly(httpClient);
		}
	}

	public static class HttpResponseEntity {
		private int statusCode;

		private String entity;

		public HttpResponseEntity(int statusCode, String entity) {
			this.statusCode = statusCode;
			this.entity = entity;
		}

		public int getStatusCode() {
			return statusCode;
		}

		public String getEntity() {
			return entity;
		}

	}
}
