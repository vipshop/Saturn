package com.vip.saturn.it.impl;

import com.google.gson.Gson;
import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.job.utils.HttpUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.assertj.core.util.Maps;
import org.junit.*;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JobOperationRestApiIT extends AbstractSaturnIT {

	private static final String CONSOLE_HOST_URL = "http://localhost:9089";

	private final Gson gson = new Gson();

	@BeforeClass
	public static void setUp() throws Exception {
		startSaturnConsoleList(1);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopSaturnConsoleList();
	}

	@After
	public void after() throws Exception {
	}

	@Test
	public void testCreateAndQueryJobSuccessfully() throws Exception {
		JobEntity jobEntity = constructJobEntity("job1");

		HttpResponseEntity responseEntity = sendPostReqest(CONSOLE_HOST_URL + "/rest/v1/it-saturn/jobs", jobEntity.toJSON());

		assertEquals(201, responseEntity.getStatusCode());

		responseEntity = sendGetRequest(CONSOLE_HOST_URL + "/rest/v1/it-saturn/jobs/job1");
		assertEquals(200, responseEntity.getStatusCode());
		JobEntity responseJobEntity = gson.fromJson(responseEntity.getEntity(), JobEntity.class);

		assertEquals("job1", responseJobEntity.getJobName());
		assertEquals("this is a description of job1", responseJobEntity.getDescription());
		assertEquals("0 */1 * * * ?", responseJobEntity.getJobConfig().get("cron"));
		assertEquals("SHELL_JOB", responseJobEntity.getJobConfig().get("jobType"));
		assertEquals(2.0, responseJobEntity.getJobConfig().get("shardingTotalCount"));
		assertEquals("0=echo 0;sleep $SLEEP_SECS,1=echo 1", responseJobEntity.getJobConfig().get("shardingItemParameters"));
	}

	@Test
	public void testCreateJobFailAsJobAlreadyExisted() throws Exception {
		JobEntity jobEntity = constructJobEntity("job2");

		HttpResponseEntity responseEntity = sendPostReqest(CONSOLE_HOST_URL + "/rest/v1/it-saturn/jobs", jobEntity.toJSON());

		assertEquals(201, responseEntity.getStatusCode());

		responseEntity = sendPostReqest(CONSOLE_HOST_URL + "/rest/v1/it-saturn/jobs", jobEntity.toJSON());

		assertEquals(400, responseEntity.getStatusCode());

		Map<String, String> responseMap = gson.fromJson(responseEntity.getEntity(), Map.class);
		assertEquals("该作业(job2)已经存在", responseMap.get("message"));
	}

	@Test
	public void testCreateJobFailAsNamespaceNotExisted() throws Exception {
		JobEntity jobEntity = constructJobEntity("job3");

		HttpResponseEntity responseEntity = sendPostReqest(CONSOLE_HOST_URL + "/rest/v1/unknown/jobs", jobEntity.toJSON());

		assertEquals(404, responseEntity.getStatusCode());

		Map<String, String> responseMap = gson.fromJson(responseEntity.getEntity(), Map.class);
		assertEquals("The namespace {unknown} does not exists.", responseMap.get("message"));
	}

	@Test
	public void testCreateJobFailAsCronIsNotFilled() throws Exception {
		JobEntity jobEntity = constructJobEntity("job3");
		jobEntity.getJobConfig().remove("cron");

		HttpResponseEntity responseEntity = sendPostReqest(CONSOLE_HOST_URL + "/rest/v1/it-saturn/jobs", jobEntity.toJSON());
		assertEquals(400, responseEntity.getStatusCode());
		Map<String, String> responseMap = gson.fromJson(responseEntity.getEntity(), Map.class);
		assertEquals("对于JAVA/SHELL作业，cron表达式必填", responseMap.get("message"));
	}

	@Test
	public void testQueryJobFailAsJobIsNotFound() throws IOException {
		HttpResponseEntity responseEntity = sendGetRequest(CONSOLE_HOST_URL + "/rest/v1/it-saturn/jobs/not_existed");

		assertEquals(404, responseEntity.getStatusCode());
		Map<String, String> responseMap = gson.fromJson(responseEntity.getEntity(), Map.class);
		assertEquals("The job {not_existed} does not exists.", responseMap.get("message"));
	}


	@Test
	public void testDeleteJobFailAsJobIsCreatedIn2Minutes() throws IOException {
		String jobName = "jobTestDeleteJobSuccessfully";
		// create a job
		JobEntity jobEntity = constructJobEntity(jobName);

		HttpResponseEntity responseEntity = sendPostReqest(CONSOLE_HOST_URL + "/rest/v1/it-saturn/jobs", jobEntity.toJSON());
		assertEquals(201, responseEntity.getStatusCode());

		// and delete it
		responseEntity = sendDeleteResponse(CONSOLE_HOST_URL + "/rest/v1/it-saturn/jobs/" + jobName);
		assertEquals(400, responseEntity.getStatusCode());
		Map<String, String> responseMap = gson.fromJson(responseEntity.getEntity(), Map.class);
		assertEquals("不能删除该作业(jobTestDeleteJobSuccessfully)，因为该作业创建时间距离现在不超过2分钟", responseMap.get("message"));
	}


	private HttpResponseEntity sendPostReqest(String url, String jsonBody) throws IOException {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			HttpPost request = new HttpPost(url);
			final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(10000).build();
			request.setConfig(requestConfig);
			StringEntity params = new StringEntity(jsonBody);
			request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			request.setEntity(params);
			// send request
			CloseableHttpResponse response = httpClient.execute(request);

			return new HttpResponseEntity(response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity()));

		} finally {
			HttpUtils.closeHttpClientQuitetly(httpClient);
		}
	}

	private HttpResponseEntity sendGetRequest(String url) throws IOException {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(url);
			final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(10000).build();
			request.setConfig(requestConfig);
			request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

			CloseableHttpResponse response = httpClient.execute(request);

			return new HttpResponseEntity(response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity()));
		} finally {
			HttpUtils.closeHttpClientQuitetly(httpClient);
		}
	}

	private HttpResponseEntity sendDeleteResponse(String url) throws IOException {
		CloseableHttpClient httpClient = null;
		try {
			httpClient = HttpClientBuilder.create().build();
			HttpDelete request = new HttpDelete(url);
			final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(10000).build();
			request.setConfig(requestConfig);
			request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			// send request
			CloseableHttpResponse response = httpClient.execute(request);

			return new HttpResponseEntity(response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity()));

		} finally {
			HttpUtils.closeHttpClientQuitetly(httpClient);
		}
	}

	private JobEntity constructJobEntity(String job) {
		JobEntity jobEntity = new JobEntity(job);

		jobEntity.setDescription("this is a description of " + job);
		jobEntity.setConfig("cron", "0 */1 * * * ?");
		jobEntity.setConfig("jobType", "SHELL_JOB");
		jobEntity.setConfig("shardingTotalCount", 2);
		jobEntity.setConfig("shardingItemParameters", "0=echo 0;sleep $SLEEP_SECS,1=echo 1");

		return jobEntity;
	}

	public class HttpResponseEntity {
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

	public class JobEntity {
		private final Gson gson = new Gson();

		private String jobName;

		private String description;

		private Map<String, Object> jobConfig = Maps.newHashMap();

		public JobEntity(String jobName) {
			this.jobName = jobName;
		}

		public void setConfig(String key, Object value) {
			jobConfig.put(key, value);
		}

		public Object getConfig(String key) {
			return jobConfig.get(key);
		}

		public String toJSON() {
			return gson.toJson(this);
		}

		public Gson getGson() {
			return gson;
		}

		public String getJobName() {
			return jobName;
		}

		public void setJobName(String jobName) {
			this.jobName = jobName;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Map<String, Object> getJobConfig() {
			return jobConfig;
		}

		public void setJobConfig(Map<String, Object> jobConfig) {
			this.jobConfig = jobConfig;
		}
	}


}
