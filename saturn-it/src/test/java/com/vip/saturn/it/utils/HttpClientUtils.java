package com.vip.saturn.it.utils;

import com.google.gson.Gson;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.utils.HttpUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author hebelala
 */
public class HttpClientUtils {

	public static ResponseEntity sendDeleteRequest(String url) throws IOException {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			HttpDelete httpDelete = new HttpDelete(url);
			final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(10000)
					.build();
			httpDelete.setConfig(requestConfig);
			CloseableHttpResponse response = httpClient.execute(httpDelete);
			HttpEntity entity = response.getEntity();
			return new ResponseEntity(response.getStatusLine().getStatusCode(),
					entity != null ? EntityUtils.toString(entity) : null);
		} finally {
			HttpUtils.closeHttpClientQuietly(httpClient);
		}
	}

	public static ResponseEntity sendGetRequest(String url) throws IOException {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(url);
			final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(10000)
					.build();
			request.setConfig(requestConfig);
			CloseableHttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			return new ResponseEntity(response.getStatusLine().getStatusCode(),
					entity != null ? EntityUtils.toString(entity) : null);
		} finally {
			HttpUtils.closeHttpClientQuietly(httpClient);
		}
	}

	public static ResponseEntity sendPostRequest(String url, Map<String, Object> params) throws IOException {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			HttpPost request = new HttpPost(url);
			if (params != null && params.size() > 0) {
				List<NameValuePair> nameValuePairList = new ArrayList<>();
				Iterator<Map.Entry<String, Object>> iterator = params.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, Object> next = iterator.next();
					nameValuePairList.add(new BasicNameValuePair(next.getKey(), String.valueOf(next.getValue())));
				}
				request.setEntity(new UrlEncodedFormEntity(nameValuePairList));
			}
			final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(10000)
					.build();
			request.setConfig(requestConfig);
			CloseableHttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			return new ResponseEntity(response.getStatusLine().getStatusCode(),
					entity != null ? EntityUtils.toString(entity) : null);
		} finally {
			HttpUtils.closeHttpClientQuietly(httpClient);
		}
	}

	public static ResponseEntity sendPostRequestJson(String url, String jsonBody) throws IOException {
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
			CloseableHttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			return new ResponseEntity(response.getStatusLine().getStatusCode(),
					entity != null ? EntityUtils.toString(entity) : null);
		} finally {
			HttpUtils.closeHttpClientQuietly(httpClient);
		}
	}

	public static ResponseEntity sendPutRequestJson(String url, String jsonBody) throws IOException {
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
			CloseableHttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			return new ResponseEntity(response.getStatusLine().getStatusCode(),
					entity != null ? EntityUtils.toString(entity) : null);

		} finally {
			HttpUtils.closeHttpClientQuietly(httpClient);
		}
	}

	public static ResponseEntity sendGetRequestJson(String url) throws IOException {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(url);
			final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(10000)
					.build();
			request.setConfig(requestConfig);
			request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			CloseableHttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			return new ResponseEntity(response.getStatusLine().getStatusCode(),
					entity != null ? EntityUtils.toString(entity) : null);
		} finally {
			HttpUtils.closeHttpClientQuietly(httpClient);
		}
	}

	public static ResponseEntity sendDeleteResponseJson(String url) throws IOException {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			HttpDelete request = new HttpDelete(url);
			final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(10000)
					.build();
			request.setConfig(requestConfig);
			request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			CloseableHttpResponse response = httpClient.execute(request);
			HttpEntity entity = response.getEntity();
			return new ResponseEntity(response.getStatusLine().getStatusCode(),
					entity != null ? EntityUtils.toString(entity) : null);
		} finally {
			HttpUtils.closeHttpClientQuietly(httpClient);
		}
	}

	public static GuiResponseEntity toGuiResponseEntity(ResponseEntity responseEntity) {
		RequestResult requestResult = new Gson().fromJson(responseEntity.getEntity(), RequestResult.class);
		GuiResponseEntity guiResponseEntity = new GuiResponseEntity();
		guiResponseEntity.setStatusCode(responseEntity.getStatusCode());
		guiResponseEntity.setRequestResult(requestResult);
		return guiResponseEntity;
	}

	public static class ResponseEntity {

		private int statusCode;
		private String entity;

		public ResponseEntity(int statusCode, String entity) {
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

	public static class GuiResponseEntity {

		private int statusCode;
		private RequestResult requestResult;

		public int getStatusCode() {
			return statusCode;
		}

		public void setStatusCode(int statusCode) {
			this.statusCode = statusCode;
		}

		public RequestResult getRequestResult() {
			return requestResult;
		}

		public void setRequestResult(RequestResult requestResult) {
			this.requestResult = requestResult;
		}
	}

}
