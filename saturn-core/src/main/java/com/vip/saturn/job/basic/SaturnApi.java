package com.vip.saturn.job.basic;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.alarm.AlarmInfo;
import com.vip.saturn.job.exception.SaturnJobException;
import com.vip.saturn.job.internal.config.ConfigurationService;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Provide the hook for client job callback.
 */
public class SaturnApi {

	private static Logger logger = LoggerFactory.getLogger(SaturnApi.class);

	private static final String SATURN_API_URI_PREFIX = SystemEnvProperties.VIP_SATURN_HOST_NAME + "/rest/v1/";

	private String namespace;

	private ConfigurationService configService;

	public SaturnApi(String namespace) {
		this.namespace = namespace;
	}

	// cannot be called
	private SaturnApi() {
	}

	public void updateJobCron(String jobName, String cron, Map<String, String> customContext) throws SaturnJobException {
		try {
			configService.updateJobCron(jobName, cron, customContext);
		} catch (Exception e) {
			throw constructSaturnJobException(e);
		}
	}

	/**
	 * The hook for client job raise alarm.
	 *
	 * @param jobName   The target job of alarm.
	 * @param shardItem The target shard ite id of alarm. If null, then will use 0 by default.
	 * @param alarmInfo The alarm information.
	 */
	public void raiseAlarm(String jobName, Integer shardItem, AlarmInfo alarmInfo) throws SaturnJobException {
		CloseableHttpClient httpClient = null;
		try {
			JSONObject json = constructRaiseAlarmRequestBody(jobName, shardItem, alarmInfo);
			// prepare
			httpClient = HttpClientBuilder.create().build();
			HttpPost request = new HttpPost(SATURN_API_URI_PREFIX + namespace + "/alarm/raise");
			StringEntity params = new StringEntity(json.toString());
			request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			request.setEntity(params);

			// send request
			CloseableHttpResponse httpResponse = httpClient.execute(request);
			// handle response
			handleResponse(httpResponse);
		} catch (Exception e) {
			throw constructSaturnJobException(e);
		} finally {
			if (httpClient != null) {
				try {
					httpClient.close();
				} catch (IOException e) {
					throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, e.getMessage(), e.getCause());
				}
			}
		}
	}

	private void handleResponse(CloseableHttpResponse httpResponse) throws IOException, SaturnJobException {
		int status = httpResponse.getStatusLine().getStatusCode();

		if (status == 201) {
			logger.info("raise alarm successfully.");
			return;
		}

		if (status >= 400 && status <= 500) {
			HttpEntity entity = httpResponse.getEntity();
			StringBuffer buffer = new StringBuffer();
			if (entity != null) {
				BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
				String temp = null;
				while ((temp = in.readLine()) != null) {
					buffer.append(temp);
				}
			}
			if (buffer.toString().length() > 0) {
				String errMsg = JSONObject.parseObject(buffer.toString()).getString("message");
				throw constructSaturnJobException(status, errMsg);
			}
		} else {
			// if have unexpected status, then throw RuntimeException directly.
			String errMsg = "unexpected status returned from Saturn Server.";
			throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, errMsg);
		}
	}

	private JSONObject constructRaiseAlarmRequestBody(String jobName, Integer shardItem, AlarmInfo alarmInfo) throws SaturnJobException {
		JSONObject jsonObject = new JSONObject();

		if (StringUtils.isBlank(jobName)) {
			throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "jobName cannot be blank.");
		}
		jsonObject.put("jobName", jobName);

		if (shardItem != null) {
			jsonObject.put("shardItem", shardItem);
		}

		if (alarmInfo == null) {
			throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "alarmInfo cannot be null.");
		}

		String level = alarmInfo.getLevel();
		if (StringUtils.isBlank(level)) {
			throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "alarmInfo.level cannot be blank.");
		}
		jsonObject.put("level", alarmInfo.getLevel());

		String name = alarmInfo.getName();
		if (StringUtils.isBlank(name)) {
			throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "alarmInfo.name cannot be blank.");
		}
		jsonObject.put("name", alarmInfo.getName());

		String title = alarmInfo.getTitle();
		if (StringUtils.isBlank(title)) {
			throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "alarmInfo.title cannot be blank.");
		}
		jsonObject.put("title", alarmInfo.getTitle());

		String message = alarmInfo.getMessage();
		if (StringUtils.isNotBlank(message)) {
			jsonObject.put("message", alarmInfo.getMessage());
		}

		if (alarmInfo.getCustomFields() != null) {
			jsonObject.put("additionalInfo", alarmInfo.getCustomFields());
		}

		return jsonObject;
	}

	private SaturnJobException constructSaturnJobException(Exception e) {
		logger.error("SaturnJobException throws: {%s}", e.getMessage());

		if (e instanceof SaturnJobException) {
			return (SaturnJobException) e;
		}
		// other exception will be marked as
		SaturnJobException saturnJobException = new SaturnJobException(SaturnJobException.SYSTEM_ERROR, e.getMessage(), e.getCause());
		saturnJobException.setStackTrace(e.getStackTrace());


		return saturnJobException;
	}

	private SaturnJobException constructSaturnJobException(int statusCode, String msg) {
		if (statusCode >= 400 && statusCode < 500) {
			return new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, msg);
		}
		return new SaturnJobException(SaturnJobException.SYSTEM_ERROR, msg);
	}

	public void setConfigService(ConfigurationService configService) {
		this.configService = configService;
	}
}
