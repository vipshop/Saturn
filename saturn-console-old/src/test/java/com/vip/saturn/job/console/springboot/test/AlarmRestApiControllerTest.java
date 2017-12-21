package com.vip.saturn.job.console.springboot.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import com.vip.saturn.job.console.service.ZkTreeService;
import org.assertj.core.util.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.console.AbstractSaturnConsoleTest;
import com.vip.saturn.job.console.controller.AlarmRestApiController;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.RestApiService;
import com.vip.saturn.job.integrate.entity.AlarmInfo;

/**
 * Created by kfchu on 24/05/2017.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(AlarmRestApiController.class)
public class AlarmRestApiControllerTest extends AbstractSaturnConsoleTest {
	@Autowired
	private MockMvc mvc;

	@MockBean
	private RestApiService restApiService;

	@MockBean
	private ZkTreeService zkTreeService;

	@Test
	public void testRaiseAlarmSuccessfully() throws Exception {
		final String jobName = "job1";
		final String executorName = "exec001";
		final String alarmName = "name";
		final String alarmTitle = "title";

		AlarmEntity alarmEntity = new AlarmEntity(jobName, executorName, alarmName, alarmTitle, "CRITICAL");
		alarmEntity.getAdditionalInfo().put("key1", "value1");
		alarmEntity.setShardItem(1);
		alarmEntity.setMessage("message");

		mvc.perform(post("/rest/v1/mydomain/alarms/raise").contentType(MediaType.APPLICATION_JSON)
				.content(alarmEntity.toJSON())).andExpect(status().isCreated());

		ArgumentCaptor<AlarmInfo> argument = ArgumentCaptor.forClass(AlarmInfo.class);

		verify(restApiService).raiseAlarm(eq("mydomain"), eq(jobName), eq(executorName), eq(1), argument.capture());
		compareAlarmInfo(alarmEntity, argument.getValue());
	}

	@Test
	public void testRaiseAlarmFailAsMissingMandatoryField() throws Exception {
		// missing jobName
		AlarmEntity alarmEntity = new AlarmEntity(null, "exec", "name", "title", "CRITICAL");

		MvcResult result = mvc.perform(post("/rest/v1/mydomain/alarms/raise").contentType(MediaType.APPLICATION_JSON)
				.content(alarmEntity.toJSON())).andExpect(status().isBadRequest()).andReturn();

		assertEquals("error message not equal", "Invalid request. Missing parameter: {jobName}",
				fetchErrorMessage(result));
		// missing executorname
		alarmEntity = new AlarmEntity("job1", null, "name", "title", "CRITICAL");

		result = mvc.perform(post("/rest/v1/mydomain/alarms/raise").contentType(MediaType.APPLICATION_JSON)
				.content(alarmEntity.toJSON())).andExpect(status().isBadRequest()).andReturn();

		assertEquals("error message not equal", "Invalid request. Missing parameter: {executorName}",
				fetchErrorMessage(result));

		// missing name
		alarmEntity = new AlarmEntity("job1", "exec", null, "title", "CRITICAL");

		result = mvc.perform(post("/rest/v1/mydomain/alarms/raise").contentType(MediaType.APPLICATION_JSON)
				.content(alarmEntity.toJSON())).andExpect(status().isBadRequest()).andReturn();

		assertEquals("error message not equal", "Invalid request. Missing parameter: {name}",
				fetchErrorMessage(result));

		// missing title
		alarmEntity = new AlarmEntity("job1", "exec", "name", null, "CRITICAL");

		result = mvc.perform(post("/rest/v1/mydomain/alarms/raise").contentType(MediaType.APPLICATION_JSON)
				.content(alarmEntity.toJSON())).andExpect(status().isBadRequest()).andReturn();

		assertEquals("error message not equal", "Invalid request. Missing parameter: {title}",
				fetchErrorMessage(result));

		// missing level
		alarmEntity = new AlarmEntity("job1", "exec", "name", "title", null);

		result = mvc.perform(post("/rest/v1/mydomain/alarms/raise").contentType(MediaType.APPLICATION_JSON)
				.content(alarmEntity.toJSON())).andExpect(status().isBadRequest()).andReturn();

		assertEquals("error message not equal", "Invalid request. Missing parameter: {level}",
				fetchErrorMessage(result));
	}

	@Test
	public void testRaiseAlarmFailWithExpectedException() throws Exception {
		String customErrMsg = "some exception throws";
		willThrow(new SaturnJobConsoleHttpException(400, customErrMsg)).given(restApiService).raiseAlarm(
				any(String.class), any(String.class), any(String.class), any(Integer.class), any(AlarmInfo.class));

		AlarmEntity alarmEntity = new AlarmEntity("job", "exec", "name", "title", "CRITICAL");
		MvcResult result = mvc.perform(post("/rest/v1/mydomain/alarms/raise").contentType(MediaType.APPLICATION_JSON)
				.content(alarmEntity.toJSON())).andExpect(status().isBadRequest()).andReturn();

		assertEquals("error message not equal", customErrMsg, fetchErrorMessage(result));
	}

	@Test
	public void testRaiseAlarmFailWithUnExpectedException() throws Exception {
		String customErrMsg = "some exception throws";
		willThrow(new RuntimeException(customErrMsg)).given(restApiService).raiseAlarm(any(String.class),
				any(String.class), any(String.class), any(Integer.class), any(AlarmInfo.class));

		AlarmEntity alarmEntity = new AlarmEntity("job", "exec", "name", "title", "CRITICAL");
		MvcResult result = mvc.perform(post("/rest/v1/mydomain/alarms/raise").contentType(MediaType.APPLICATION_JSON)
				.content(alarmEntity.toJSON())).andExpect(status().isInternalServerError()).andReturn();

		assertEquals("error message not equal", customErrMsg, fetchErrorMessage(result));
	}

	private void compareAlarmInfo(AlarmEntity expectAlarmInfo, AlarmInfo actualAlarmInfo) {
		assertEquals("name is not equal", expectAlarmInfo.getName(), actualAlarmInfo.getName());
		assertEquals("title is not equal", expectAlarmInfo.getTitle(), actualAlarmInfo.getTitle());
		assertEquals("level is not equal", expectAlarmInfo.getLevel(), actualAlarmInfo.getLevel());
		assertEquals("message is not equal", expectAlarmInfo.getMessage(), actualAlarmInfo.getMessage());
		assertEquals("additional info is not equal", expectAlarmInfo.getAdditionalInfo().get("key1"),
				actualAlarmInfo.getCustomFields().get("key1"));
	}

	public class AlarmEntity {

		private String jobName;

		private String executorName;

		private int shardItem;

		private String name;

		private String level;

		private String title;

		private String message;

		private Map<String, String> additionalInfo = Maps.newHashMap();

		public AlarmEntity(String jobName, String executorName, String name, String title, String level) {
			this.jobName = jobName;
			this.executorName = executorName;
			this.name = name;
			this.title = title;
			this.level = level;
		}

		public String toJSON() {
			return JSONObject.toJSONString(this);
		}

		public String getJobName() {
			return jobName;
		}

		public void setJobName(String jobName) {
			this.jobName = jobName;
		}

		public String getExecutorName() {
			return executorName;
		}

		public void setExecutorName(String executorName) {
			this.executorName = executorName;
		}

		public int getShardItem() {
			return shardItem;
		}

		public void setShardItem(int shardItem) {
			this.shardItem = shardItem;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getLevel() {
			return level;
		}

		public void setLevel(String level) {
			this.level = level;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public Map<String, String> getAdditionalInfo() {
			return additionalInfo;
		}

		public void setAdditionalInfo(Map<String, String> additionalInfo) {
			this.additionalInfo = additionalInfo;
		}
	}
}
