package com.vip.saturn.job.console.springboot.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.vip.saturn.job.console.service.ZkTreeService;
import org.assertj.core.util.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.vip.saturn.job.console.AbstractSaturnConsoleTest;
import com.vip.saturn.job.console.controller.rest.JobOperationRestApiController;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RestApiJobConfig;
import com.vip.saturn.job.console.domain.RestApiJobInfo;
import com.vip.saturn.job.console.domain.RestApiJobStatistics;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.RestApiService;

/**
 * Created by kfchu on 24/05/2017.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(JobOperationRestApiController.class)
public class JobOperationRestApiControllerTest extends AbstractSaturnConsoleTest {

	@Autowired
	private MockMvc mvc;

	@MockBean
	private RestApiService restApiService;

	@MockBean
	private ZkTreeService zkTreeService;

	@Test
	public void testCreateSuccessfully() throws Exception {
		JobEntity jobEntity = constructJobEntity("job1");
		mvc.perform(post("/rest/v1/domain/jobs").contentType(MediaType.APPLICATION_JSON).content(jobEntity.toJSON()))
				.andExpect(status().isCreated());

		JobConfig jobConfig = convert2JobConfig("domain", jobEntity);
		ArgumentCaptor<JobConfig> argument = ArgumentCaptor.forClass(JobConfig.class);

		verify(restApiService).createJob(eq("domain"), argument.capture());
		assertTrue("jobconfig is not equal", jobConfig.equals(argument.getValue()));
	}

	@Test
	public void testCreateFailAsMissingMandatoryField() throws Exception {
		JobEntity jobEntity = constructJobEntity("job1");
		// jobType should be mandatory
		jobEntity.setConfig("jobType", null);

		MvcResult result = mvc.perform(
				post("/rest/v1/domain/jobs").contentType(MediaType.APPLICATION_JSON).content(jobEntity.toJSON()))
				.andExpect(status().isBadRequest()).andReturn();

		String message = fetchErrorMessage(result);
		assertEquals("error message not equal", "Invalid request. Missing parameter: {jobType}", message);
	}

	@Test
	public void testCreateFailAsInvalidJobType() throws Exception {
		JobEntity jobEntity = constructJobEntity("job1");
		// jobType should be mandatory
		jobEntity.setConfig("jobType", "abc");

		MvcResult result = mvc.perform(
				post("/rest/v1/domain/jobs").contentType(MediaType.APPLICATION_JSON).content(jobEntity.toJSON()))
				.andExpect(status().isBadRequest()).andReturn();

		String message = fetchErrorMessage(result);
		assertEquals("error message not equal", "Invalid request. Parameter: {jobType} is malformed", message);
	}

	@Test
	public void testCreateFailAsSaturnJobExceptionThrows() throws Exception {
		String customErrMsg = "some exception throws";
		willThrow(new SaturnJobConsoleException(SaturnJobConsoleException.ERROR_CODE_INTERNAL_ERROR, customErrMsg)).given(restApiService)
				.createJob(any(String.class),
				any(JobConfig.class));

		JobEntity jobEntity = constructJobEntity("job12345");
		MvcResult result = mvc.perform(
				post("/rest/v1/domain/jobs").contentType(MediaType.APPLICATION_JSON).content(jobEntity.toJSON()))
				.andExpect(status().isInternalServerError()).andReturn();

		String message = fetchErrorMessage(result);
		assertEquals("error message not equal", customErrMsg, message);

		// Created
		customErrMsg = "jobname does not exists";
		willThrow(new SaturnJobConsoleException(SaturnJobConsoleException.ERROR_CODE_NOT_EXISTED, customErrMsg)).given(restApiService)
				.createJob(any(String.class),
				any(JobConfig.class));

		result = mvc.perform(
				post("/rest/v1/domain/jobs").contentType(MediaType.APPLICATION_JSON).content(jobEntity.toJSON()))
				.andExpect(status().isNotFound()).andReturn();

		message = fetchErrorMessage(result);
		assertEquals("error message not equal", customErrMsg, message);
	}

	@Test
	public void testCreateFailAsSaturnJobHttpExceptionThrows() throws Exception {
		String customErrMsg = "some exception throws";
		willThrow(new SaturnJobConsoleHttpException(400, customErrMsg)).given(restApiService)
				.createJob(any(String.class), any(JobConfig.class));

		JobEntity jobEntity = constructJobEntity("job1");
		MvcResult result = mvc.perform(
				post("/rest/v1/domain/jobs").contentType(MediaType.APPLICATION_JSON).content(jobEntity.toJSON()))
				.andExpect(status().isBadRequest()).andReturn();

		String message = fetchErrorMessage(result);
		assertEquals("error message not equal", customErrMsg, message);
	}

	@Test
	public void testCreateFailAsSaturnHttpJobExceptionThrows() throws Exception {
		String customErrMsg = "some exception throws";
		willThrow(new SaturnJobConsoleHttpException(400, customErrMsg)).given(restApiService)
				.createJob(any(String.class), any(JobConfig.class));

		JobEntity jobEntity = constructJobEntity("job1");
		MvcResult result = mvc.perform(
				post("/rest/v1/domain/jobs").contentType(MediaType.APPLICATION_JSON).content(jobEntity.toJSON()))
				.andExpect(status().isBadRequest()).andReturn();

		String message = fetchErrorMessage(result);
		assertEquals("error message not equal", customErrMsg, message);
	}

	@Test
	public void testCreateFailAsUnExpectedExceptionThrows() throws Exception {
		String customErrMsg = "unexpected exception";
		willThrow(new RuntimeException(customErrMsg)).given(restApiService).createJob(any(String.class),
				any(JobConfig.class));

		JobEntity jobEntity = constructJobEntity("job1");
		MvcResult result = mvc.perform(
				post("/rest/v1/domain/jobs").contentType(MediaType.APPLICATION_JSON).content(jobEntity.toJSON()))
				.andExpect(status().isInternalServerError()).andReturn();

		String message = fetchErrorMessage(result);
		assertEquals("error message not equal", customErrMsg, message);
	}

	@Test
	public void testCreateFailAsJobAlreadyExisted() throws Exception {
		JobEntity jobEntity = constructJobEntity("job2");
		String errMsg = "该作业(job2)已经存在";
		willThrow(new SaturnJobConsoleException(SaturnJobConsoleException.ERROR_CODE_BAD_REQUEST, errMsg)).given(restApiService)
				.createJob(any(String.class), any(JobConfig.class));

		MvcResult result = mvc.perform(post("/rest/v1/domain/jobs").contentType(MediaType.APPLICATION_JSON).content(jobEntity.toJSON()))
				.andExpect(status().isBadRequest()).andReturn();

		String message = fetchErrorMessage(result);
		assertEquals("error message not equal", "该作业(job2)已经存在", message);
	}

	@Test
	public void testQuerySuccessfully() throws Exception {
		String jobName = "job1";

		RestApiJobInfo jobInfo = constructJobInfo("domain", jobName);

		given(restApiService.getRestAPIJobInfo("domain", jobName)).willReturn(jobInfo);

		MvcResult result = mvc.perform(get("/rest/v1/domain/jobs/" + jobName)).andExpect(status().isOk()).andReturn();

		String body = result.getResponse().getContentAsString();
		Map<String, Object> resultMap = JSONObject.parseObject(body, Map.class);
		assertEquals("jobname not equal", jobName, resultMap.get("jobName"));
		assertEquals("description not equal", jobInfo.getDescription(), resultMap.get("description"));

		Map<String, Object> jobConfigMap = (Map<String, Object>) resultMap.get("jobConfig");
		assertEquals("cron not equal", jobInfo.getJobConfig().getCron(), jobConfigMap.get("cron"));

		Map<String, Object> statisticsMap = (Map<String, Object>) resultMap.get("statistics");
		assertEquals("nextFireTime not equal", jobInfo.getStatistics().getNextFireTime(),
				new Long((Integer) statisticsMap.get("nextFireTime")));
	}

	@Test
	public void testQueryFailAsSaturnJobExceptionThrows() throws Exception {
		String customErrMsg = "some exception throws";
		willThrow(new SaturnJobConsoleHttpException(400, customErrMsg)).given(restApiService)
				.getRestAPIJobInfo("domain", "job1");

		MvcResult result = mvc.perform(get("/rest/v1/domain/jobs/job1")).andExpect(status().isBadRequest()).andReturn();

		assertEquals("error msg is not equal", customErrMsg, fetchErrorMessage(result));
	}

	@Test
	public void testRunAtOnceSuccessfully() throws Exception {
		mvc.perform(post("/rest/v1/domain/jobs/abc/run").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent()).andReturn();
	}

	@Test
	public void testStopAtOnceSuccessfully() throws Exception {
		mvc.perform(post("/rest/v1/domain/jobs/abc/stop").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent()).andReturn();
	}

	@Test
	public void testDeleteJobSuccessfully() throws Exception {
		mvc.perform(delete("/rest/v1/domain/jobs/abc").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNoContent()).andReturn();
	}

	private RestApiJobInfo constructJobInfo(String domain, String jobName) {
		RestApiJobInfo jobInfo = new RestApiJobInfo();
		jobInfo.setJobName(jobName);
		jobInfo.setEnabled(true);
		jobInfo.setDescription("this is a decription of " + jobName);

		RestApiJobConfig jobConfig = new RestApiJobConfig();
		jobConfig.setCron("0 */1 * * * ?");

		jobInfo.setJobConfig(jobConfig);

		RestApiJobStatistics statistics = new RestApiJobStatistics();
		statistics.setNextFireTime(1234567L);
		jobInfo.setStatistics(statistics);

		return jobInfo;
	}

	private JobConfig convert2JobConfig(String namespace, JobEntity jobEntity) {
		JobConfig jobConfig = new JobConfig();

		jobConfig.setJobName(jobEntity.getJobName());
		jobConfig.setCron((String) jobEntity.getConfig("cron"));
		jobConfig.setJobType((String) jobEntity.getConfig("jobType"));
		jobConfig.setShardingTotalCount((Integer) jobEntity.getConfig("shardingTotalCount"));
		jobConfig.setShardingItemParameters((String) jobEntity.getConfig("shardingItemParameters"));
		jobConfig.setDescription(jobEntity.getDescription());

		jobConfig.setLocalMode(null);
		jobConfig.setUseSerial(null);

		return jobConfig;
	}

	private JobEntity constructJobEntity(String job) {
		JobEntity jobEntity = new JobEntity("job");

		jobEntity.setDescription("this is a description of " + job);
		jobEntity.setConfig("cron", "0 */1 * * * ?");
		jobEntity.setConfig("jobType", "SHELL_JOB");
		jobEntity.setConfig("shardingTotalCount", 2);
		jobEntity.setConfig("shardingItemParameters", "0=echo 0;sleep $SLEEP_SECS,1=echo 1");

		return jobEntity;
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
