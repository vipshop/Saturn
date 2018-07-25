package com.vip.saturn.it.impl;

import com.google.gson.Gson;
import com.vip.saturn.it.AbstractSaturnIT;
import com.vip.saturn.it.utils.HttpClientUtils;
import com.vip.saturn.it.utils.HttpClientUtils.HttpResponseEntity;
import java.io.File;
import org.assertj.core.util.Maps;
import org.codehaus.jackson.map.Serializers.Base;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class RestApiIT extends AbstractSaturnIT {

	private final Gson gson = new Gson();
	private static String CONSOLE_HOST_URL;
	private static String BASE_URL;
	private static final String PATH_SEPARATOR = "/";

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@BeforeClass
	public static void setUp() throws Exception {
		System.setProperty("ALARM_RAISED_ON_EXECUTOR_RESTART", "true");
		startSaturnConsoleList(1);
		CONSOLE_HOST_URL = saturnConsoleInstanceList.get(0).url;
		BASE_URL = CONSOLE_HOST_URL + "/rest/v1/it-saturn/jobs";
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
		// create
		JobEntity jobEntity = constructJobEntity("job1");
		HttpClientUtils.HttpResponseEntity responseEntity = HttpClientUtils.sendPostRequestJson(BASE_URL,
				jobEntity.toJSON());
		assertEquals(201, responseEntity.getStatusCode());
		// query
		responseEntity = HttpClientUtils.sendGetRequestJson(BASE_URL + "/job1");
		assertEquals(200, responseEntity.getStatusCode());
		JobEntity responseJobEntity = gson.fromJson(responseEntity.getEntity(), JobEntity.class);
		// assert for details
		assertEquals("job1", responseJobEntity.getJobName());
		assertEquals("this is a description of job1", responseJobEntity.getDescription());
		assertEquals("0 */1 * * * ?", responseJobEntity.getJobConfig().get("cron"));
		assertEquals("SHELL_JOB", responseJobEntity.getJobConfig().get("jobType"));
		assertEquals(2.0, responseJobEntity.getJobConfig().get("shardingTotalCount"));
		assertEquals("0=echo 0;sleep $SLEEP_SECS,1=echo 1",
				responseJobEntity.getJobConfig().get("shardingItemParameters"));
	}

	@Test
	public void testCreateJobFailAsJobAlreadyExisted() throws Exception {
		JobEntity jobEntity = constructJobEntity("job2");

		HttpClientUtils.HttpResponseEntity responseEntity = HttpClientUtils.sendPostRequestJson(BASE_URL,
				jobEntity.toJSON());

		assertEquals(201, responseEntity.getStatusCode());

		responseEntity = HttpClientUtils.sendPostRequestJson(BASE_URL, jobEntity.toJSON());

		assertEquals(400, responseEntity.getStatusCode());

		Map<String, String> responseMap = gson.fromJson(responseEntity.getEntity(), Map.class);
		assertEquals("该作业(job2)已经存在", responseMap.get("message"));
	}

	@Test
	public void testCreateJobFailAsNamespaceNotExisted() throws Exception {
		JobEntity jobEntity = constructJobEntity("job3");

		HttpClientUtils.HttpResponseEntity responseEntity = HttpClientUtils
				.sendPostRequestJson(CONSOLE_HOST_URL + "/rest/v1/unknown/jobs", jobEntity.toJSON());

		assertEquals(404, responseEntity.getStatusCode());

		Map<String, String> responseMap = gson.fromJson(responseEntity.getEntity(), Map.class);
		assertEquals("The namespace {unknown} does not exists.", responseMap.get("message"));
	}

	@Test
	public void testCreateJobFailAsCronIsNotFilled() throws Exception {
		JobEntity jobEntity = constructJobEntity("job3");
		jobEntity.getJobConfig().remove("cron");

		HttpClientUtils.HttpResponseEntity responseEntity = HttpClientUtils.sendPostRequestJson(BASE_URL,
				jobEntity.toJSON());
		assertEquals(400, responseEntity.getStatusCode());
		Map<String, String> responseMap = gson.fromJson(responseEntity.getEntity(), Map.class);
		assertEquals("对于JAVA/SHELL作业，cron表达式必填", responseMap.get("message"));
	}

	@Test
	public void testQueryJobFailAsJobIsNotFound() throws IOException {
		HttpClientUtils.HttpResponseEntity responseEntity = HttpClientUtils
				.sendGetRequestJson(BASE_URL + "/not_existed");

		assertEquals(404, responseEntity.getStatusCode());
		Map<String, String> responseMap = gson.fromJson(responseEntity.getEntity(), Map.class);
		assertEquals("The job {not_existed} does not exists.", responseMap.get("message"));
	}

	@Test
	public void testDeleteJobSuccessful() throws Exception {
		String jobName = "testDeleteJobSuccessful";
		JobEntity jobEntity = constructJobEntity(jobName);
		createJob(jobEntity);
		Thread.sleep(2 * 60 * 1000L + 1);
		expectedEx.expect(IllegalArgumentException.class);
		expectedEx.expectMessage("Entity may not be null");
		HttpResponseEntity responseEntity = HttpClientUtils.sendDeleteResponseJson(BASE_URL + PATH_SEPARATOR + jobName);
	}

	@Test
	public void testDeleteJobFailAsJobIsCreatedIn2Minutes() throws IOException {
		String jobName = "jobTestDeleteJobSuccessfully";
		// create a job
		JobEntity jobEntity = constructJobEntity(jobName);

		HttpClientUtils.HttpResponseEntity responseEntity = HttpClientUtils.sendPostRequestJson(BASE_URL,
				jobEntity.toJSON());
		assertEquals(201, responseEntity.getStatusCode());

		// and delete it
		responseEntity = HttpClientUtils.sendDeleteResponseJson(BASE_URL + PATH_SEPARATOR + jobName);
		assertEquals(400, responseEntity.getStatusCode());
		Map<String, String> responseMap = gson.fromJson(responseEntity.getEntity(), Map.class);
		assertEquals("不能删除该作业(jobTestDeleteJobSuccessfully)，因为该作业创建时间距离现在不超过2分钟", responseMap.get("message"));
	}

	@Test
	public void testEnableAndDisableJobSuccessfully() throws IOException, InterruptedException {
		// create
		String jobName = "testEnableJobSuccessfully";
		JobEntity jobEntity = constructJobEntity(jobName);
		HttpClientUtils.HttpResponseEntity responseEntity = HttpClientUtils.sendPostRequestJson(BASE_URL,
				jobEntity.toJSON());
		assertEquals(201, responseEntity.getStatusCode());
		// sleep for 10 seconds
		Thread.sleep(10100L);
		// enable
		responseEntity = HttpClientUtils.sendPostRequestJson(
				BASE_URL + PATH_SEPARATOR + jobName + "/enable", jobEntity.toJSON());
		assertEquals(200, responseEntity.getStatusCode());
		// query for status
		responseEntity = HttpClientUtils.sendGetRequestJson(BASE_URL + PATH_SEPARATOR + jobName);
		assertEquals(200, responseEntity.getStatusCode());
		JobEntity responseJobEntity = gson.fromJson(responseEntity.getEntity(), JobEntity.class);
		assertEquals("READY", responseJobEntity.getRunningStatus());
		// enable again
		responseEntity = HttpClientUtils.sendPostRequestJson(
				BASE_URL + PATH_SEPARATOR + jobName + "/enable", jobEntity.toJSON());
		assertEquals(201, responseEntity.getStatusCode());
		// query for status
		responseEntity = HttpClientUtils.sendGetRequestJson(BASE_URL + PATH_SEPARATOR + jobName);
		assertEquals(200, responseEntity.getStatusCode());
		responseJobEntity = gson.fromJson(responseEntity.getEntity(), JobEntity.class);
		assertEquals("READY", responseJobEntity.getRunningStatus());
		// sleep for 3 seconds
		Thread.sleep(3010L);
		// disable
		responseEntity = HttpClientUtils.sendPostRequestJson(
				BASE_URL + PATH_SEPARATOR + jobName + "/disable", jobEntity.toJSON());
		assertEquals(200, responseEntity.getStatusCode());
		// query for status
		responseEntity = HttpClientUtils.sendGetRequestJson(BASE_URL + PATH_SEPARATOR + jobName);
		assertEquals(200, responseEntity.getStatusCode());
		responseJobEntity = gson.fromJson(responseEntity.getEntity(), JobEntity.class);
		assertEquals("STOPPED", responseJobEntity.getRunningStatus());
		// disable again
		responseEntity = HttpClientUtils.sendPostRequestJson(
				BASE_URL + PATH_SEPARATOR + jobName + "/disable", jobEntity.toJSON());
		assertEquals(201, responseEntity.getStatusCode());
		// query for status
		responseEntity = HttpClientUtils.sendGetRequestJson(BASE_URL + PATH_SEPARATOR + jobName);
		assertEquals(200, responseEntity.getStatusCode());
		responseJobEntity = gson.fromJson(responseEntity.getEntity(), JobEntity.class);
		assertEquals("STOPPED", responseJobEntity.getRunningStatus());
	}

	@Test
	public void testEnabledJobFailAsCreationLessThan10() throws Exception {
		// create
		String jobName = "testEnabledJobFailAsCreationLessThan10";
		JobEntity jobEntity = constructJobEntity(jobName);
		createJob(jobEntity);
		Thread.sleep(6001L);
		// enabled job
		HttpResponseEntity responseEntity = HttpClientUtils.sendPostRequestJson(
				BASE_URL + PATH_SEPARATOR + jobName + "/enable", jobEntity.toJSON());
		assertEquals(403, responseEntity.getStatusCode());
		assertEquals("Cannot enable the job until 10 seconds after job creation!",
				gson.fromJson(responseEntity.getEntity(), Map.class).get("message"));
	}

	@Test
	public void testEnabledAndDisabledJobFailAsIntervalLessThan3() throws Exception {
		// create
		String jobName = "testEnabledAndDisabledJobFailAsIntervalLessThan3";
		JobEntity jobEntity = constructJobEntity(jobName);
		createJob(jobEntity);
		Thread.sleep(10001L);
		// enabled job
		HttpResponseEntity responseEntity = HttpClientUtils.sendPostRequestJson(
				BASE_URL + PATH_SEPARATOR + jobName + "/enable", jobEntity.toJSON());
		assertEquals(200, responseEntity.getStatusCode());
		// disabled job less than 3 seconds
		responseEntity = HttpClientUtils.sendPostRequestJson(
				BASE_URL + PATH_SEPARATOR + jobName + "/disable", jobEntity.toJSON());
		assertEquals(403, responseEntity.getStatusCode());
		assertEquals("The update interval time cannot less than 3 seconds",
				gson.fromJson(responseEntity.getEntity(), Map.class).get("message"));
	}

	@Test
	public void testDisableAndEnabledJobFailAsIntervalLessThan3() throws Exception {
		// create
		String jobName = "testEnabledJobFail";
		JobEntity jobEntity = constructJobEntity(jobName);
		createJob(jobEntity);
		Thread.sleep(10001L);
		// enabled job
		HttpResponseEntity responseEntity = HttpClientUtils.sendPostRequestJson(
				BASE_URL + PATH_SEPARATOR + jobName + "/enable", jobEntity.toJSON());
		assertEquals(200, responseEntity.getStatusCode());
		Thread.sleep(3001L);
		// disabled job after 3 seconds
		responseEntity = HttpClientUtils.sendPostRequestJson(
				BASE_URL + PATH_SEPARATOR + jobName + "/disable", jobEntity.toJSON());
		assertEquals(200, responseEntity.getStatusCode());
		// enabled less than 3 seconds
		responseEntity = HttpClientUtils.sendPostRequestJson(
				BASE_URL + PATH_SEPARATOR + jobName + "/enable", jobEntity.toJSON());
		assertEquals(403, responseEntity.getStatusCode());
		assertEquals("The update interval time cannot less than 3 seconds",
				gson.fromJson(responseEntity.getEntity(), Map.class).get("message"));
	}

	@Test
	public void testRunJobSuccessful() throws Exception {
		startExecutorList(1);
		String jobName = "testRunJobSuccessFul";
		JobEntity jobEntity = constructJobEntity(jobName);
		createJob(jobEntity);
		Thread.sleep(10001L);
		HttpResponseEntity responseEntity = HttpClientUtils.sendPostRequestJson(
				BASE_URL + PATH_SEPARATOR + jobName + "/enable", jobEntity.toJSON());
		assertEquals(200, responseEntity.getStatusCode());
		expectedEx.expect(IllegalArgumentException.class);
		expectedEx.expectMessage("Entity may not be null");
		responseEntity = HttpClientUtils.sendPostRequestJson(BASE_URL + PATH_SEPARATOR + jobName + "/run", "");
		assertEquals(204, responseEntity.getStatusCode());
	}

	@Test
	public void testRunJobFailAsStatusNotReady() throws Exception {
		String jobName = "testRunJobFail";
		JobEntity jobEntity = constructJobEntity(jobName);
		createJob(jobEntity);
		HttpResponseEntity responseEntity = HttpClientUtils.sendPostRequestJson(BASE_URL + PATH_SEPARATOR + jobName + "/run", "");
		assertEquals(400, responseEntity.getStatusCode());
		assertEquals("job's status is not {READY}",
				gson.fromJson(responseEntity.getEntity(), Map.class).get("message"));
	}

	@Test
	public void testRunJobFailAsNoExecutor() throws Exception {
		String jobName = "testRunJobFailAsNoExecutor";
		JobEntity jobEntity = constructJobEntity(jobName);
		createJob(jobEntity);
		Thread.sleep(10001L);
		HttpResponseEntity responseEntity = HttpClientUtils.sendPostRequestJson(
				BASE_URL + PATH_SEPARATOR + jobName + "/enable", jobEntity.toJSON());
		assertEquals(200, responseEntity.getStatusCode());
		responseEntity = HttpClientUtils.sendPostRequestJson(BASE_URL + PATH_SEPARATOR + jobName + "/run", "");
		assertEquals(400, responseEntity.getStatusCode());
		assertEquals("no executor found for this job",
				gson.fromJson(responseEntity.getEntity(), Map.class).get("message"));
	}

	@Test
	public void testStopJobFailAsStatusIsReady() throws Exception {
		String jobName = "testStopJobFail";
		JobEntity jobEntity = constructJobEntity(jobName);
		createJob(jobEntity);
		Thread.sleep(10001L);
		HttpResponseEntity responseEntity = HttpClientUtils.sendPostRequestJson(BASE_URL + PATH_SEPARATOR + jobName + "/enable",
				jobEntity.toJSON());
		assertEquals(200, responseEntity.getStatusCode());
		responseEntity = HttpClientUtils.sendPostRequestJson(BASE_URL + PATH_SEPARATOR + jobName + "/stop", "");
		assertEquals(400, responseEntity.getStatusCode());
		assertEquals("job cannot be stopped while its status is READY or RUNNING",
				gson.fromJson(responseEntity.getEntity(), Map.class).get("message"));
	}

	@Test
	public void testRaiseExecutorRestartAlarmSuccessfully() throws IOException {
		Map<String, Object> requestBody = Maps.newHashMap();
		requestBody.put("executorName", "exec_1");
		requestBody.put("level", "Critical");
		requestBody.put("title", "Executor_Restart");
		requestBody.put("name", "Saturn Event");

		HttpClientUtils.HttpResponseEntity responseEntity = HttpClientUtils
				.sendPostRequestJson(CONSOLE_HOST_URL + "/rest/v1/it-saturn/alarms/raise", gson.toJson(requestBody));
		assertEquals(201, responseEntity.getStatusCode());
	}

	@Test
	public void testUpdateCronSuccessfully() throws IOException, InterruptedException {
		String jobName = "testUpdateCronSuccessfully";
		// create
		JobEntity jobEntity = constructJobEntity(jobName);
		HttpClientUtils.HttpResponseEntity responseEntity = HttpClientUtils
				.sendPostRequestJson(BASE_URL, jobEntity.toJSON());
		assertEquals(201, responseEntity.getStatusCode());
		// sleep for a while ...
		Thread.sleep(3010L);
		// update cron
		Map<String, Object> requestBody = Maps.newHashMap();
		requestBody.put("cron", "0 0/11 * * * ?");

		responseEntity = HttpClientUtils.sendPutRequestJson(
				BASE_URL + PATH_SEPARATOR + jobName + "/cron", gson.toJson(requestBody));
		System.out.println(responseEntity.getEntity());
		assertEquals(200, responseEntity.getStatusCode());
		// query again
		responseEntity = HttpClientUtils.sendGetRequestJson(BASE_URL + PATH_SEPARATOR + jobName);
		assertEquals(200, responseEntity.getStatusCode());
		JobEntity responseJobEntity = gson.fromJson(responseEntity.getEntity(), JobEntity.class);
		assertEquals("0 0/11 * * * ?", responseJobEntity.getJobConfig().get("cron"));
	}

	@Test
    public void testUpdateCronFailAsCronInvalid() throws Exception{
        String jobName = "testUpdateCronFailAsCronInvalid";
        // create
        JobEntity jobEntity = constructJobEntity(jobName);
        HttpClientUtils.HttpResponseEntity responseEntity = HttpClientUtils
                .sendPostRequestJson(BASE_URL, jobEntity.toJSON());
        assertEquals(201, responseEntity.getStatusCode());
        // sleep for a while ...
        Thread.sleep(3010L);
        // update cron
        Map<String, Object> requestBody = Maps.newHashMap();
        requestBody.put("cron", "abc");

        responseEntity = HttpClientUtils.sendPutRequestJson(
                BASE_URL + PATH_SEPARATOR + jobName + "/cron", gson.toJson(requestBody));
        System.out.println(responseEntity.getEntity());
		assertEquals(400, responseEntity.getStatusCode());
		assertEquals("The cron expression is invalid: abc",
				gson.fromJson(responseEntity.getEntity(), Map.class).get("message"));
    }

    @Test
    public void testUpdateCronFailAsJobNotExists() throws Exception{
        String jobName = "testUpdateCronFailAsJobNotExists";
        // update cron
        Map<String, Object> requestBody = Maps.newHashMap();
        requestBody.put("cron", "abc");
        HttpResponseEntity responseEntity = HttpClientUtils.sendPutRequestJson(
                BASE_URL + PATH_SEPARATOR + "unknown" + "/cron", gson.toJson(requestBody));
        System.out.println(responseEntity.getEntity());
        assertEquals(404, responseEntity.getStatusCode());
        assertEquals("The job {unknown} does not exists.",
                gson.fromJson(responseEntity.getEntity(), Map.class).get("message"));
    }

	@Test
	public void testUpdateJobSuccessfully() throws Exception {
		String jobName = "testUpdateJobSuccessful";
		JobEntity jobEntity = constructJobEntity(jobName);
		createJob(jobEntity);
		// 执行 update
		HttpResponseEntity responseEntity = HttpClientUtils.sendPostRequestJson(BASE_URL + PATH_SEPARATOR + jobName,
				jobEntity.toJSON());
		assertEquals(200, responseEntity.getStatusCode());
	}

	@Test
	public void testUpdateJobFailAsStatusNotDisabled() throws Exception {
		String jobName = "testUpdateJobFailAsStatusNotDisabled";
		JobEntity jobEntity = constructJobEntity(jobName);
		createJob(jobEntity);
		Thread.sleep(10001L);
		HttpResponseEntity responseEntity = HttpClientUtils.sendPostRequestJson(
				BASE_URL + PATH_SEPARATOR + jobName + "/enable", jobEntity.toJSON());
		assertEquals(200, responseEntity.getStatusCode());
		responseEntity = HttpClientUtils.sendPostRequestJson(BASE_URL + PATH_SEPARATOR + jobName, jobEntity.toJSON());
		assertEquals(400, responseEntity.getStatusCode());
		assertEquals("job's status is not {STOPPED}",
				gson.fromJson(responseEntity.getEntity(), Map.class).get("message"));
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

	private void createJob(JobEntity jobEntity) throws Exception {
		HttpClientUtils.HttpResponseEntity responseEntity = HttpClientUtils
				.sendPostRequestJson(BASE_URL, jobEntity.toJSON());
		assertEquals(201, responseEntity.getStatusCode());
		// sleep for 10 seconds
	}

	private void assertEqualsJob(String jobName, JobEntity jobEntity) {
		assertEquals(jobName, jobEntity.getJobName());
		assertEquals("this is a description of job1", jobEntity.getDescription());
		assertEquals("0 */1 * * * ?", jobEntity.getJobConfig().get("cron"));
		assertEquals("SHELL_JOB", jobEntity.getJobConfig().get("jobType"));
		assertEquals(2.0, jobEntity.getJobConfig().get("shardingTotalCount"));
		assertEquals("0=echo 0;sleep $SLEEP_SECS,1=echo 1", jobEntity.getJobConfig().get("shardingItemParameters"));
	}

	private void invokeApiFailAsTokenMissing(String uri) {

	}

	public class JobEntity {
		private final Gson gson = new Gson();

		private String jobName;

		private String description;

		private String runningStatus;

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

		public String getRunningStatus() {
			return runningStatus;
		}

		public void setRunningStatus(String runningStatus) {
			this.runningStatus = runningStatus;
		}
	}

}
