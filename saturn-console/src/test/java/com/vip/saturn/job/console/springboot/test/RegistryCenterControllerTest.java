package com.vip.saturn.job.console.springboot.test;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.console.AbstractSaturnConsoleTest;
import com.vip.saturn.job.console.controller.rest.AlarmRestApiController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(AlarmRestApiController.class)
public class RegistryCenterControllerTest extends AbstractSaturnConsoleTest {

	@Autowired
	private MockMvc mvc;

	@Test
	public void testCreateAndUpdateZkClusterInfo() throws Exception {
		String clusterName = "/clusterx";
		// craete a new zkCluster
		ZkClusterInfoForTest zkClusterInfo = new ZkClusterInfoForTest(clusterName, "alias1", "127.0.0.1:12345", "A机房");
		MvcResult result = mvc.perform(post("/console/zkClusters").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.content(zkClusterInfo.toContent())).andExpect(status().isOk()).andReturn();
		String responseBody = result.getResponse().getContentAsString();
		Map<String, Object> resultMap = JSONObject.parseObject(responseBody, Map.class);
		assertEquals(0, resultMap.get("status"));

		// refresh
		mvc.perform(get("/console/registryCenter/refresh")).andExpect(status().isOk()).andReturn();
		// get and compare
		int size;
		int count = 0;
		List<Map<String, String>> objValue;
		do {
			Thread.sleep(3000L);
			result = mvc.perform(get("/console/zkClusters")).andExpect(status().isOk()).andReturn();
			responseBody = result.getResponse().getContentAsString();
			resultMap = JSONObject.parseObject(responseBody, Map.class);
			objValue = (List<Map<String, String>>) resultMap.get("obj");
			size = objValue.size();
		} while (size == 1 && count++ < 10);

		assertEquals(2, size);

		String connectionString = "";
		String description = "";
		for (Map<String, String> clusterInfo : objValue) {
			String clusterKey = clusterInfo.get("zkClusterKey");
			if (clusterKey.equals(clusterName)) {
				connectionString = clusterInfo.get("zkAddr");
				description = clusterInfo.get("description");
				break;
			}
		}

		assertEquals("127.0.0.1:12345", connectionString);
		assertEquals("A机房", description);

		// get 单个zkcluster
		result = mvc.perform(get("/console/zkClusters?zkClusterKey=" + clusterName)).andExpect(status().isOk())
				.andReturn();
		responseBody = result.getResponse().getContentAsString();
		resultMap = JSONObject.parseObject(responseBody, Map.class);
		Map<String, Object> zkClusterMap = (Map<String, Object>) resultMap.get("obj");
		assertEquals(clusterName, zkClusterMap.get("zkClusterKey"));
		assertEquals("127.0.0.1:12345", zkClusterMap.get("zkAddr"));
		assertEquals("A机房", zkClusterMap.get("description"));
		assertTrue((Boolean) zkClusterMap.get("offline"));
	}

	private static class ZkClusterInfoForTest {
		private String zkClusterKey;

		private String alias;

		private String connectString;

		private String description;

		public ZkClusterInfoForTest(String zkClusterKey, String alias, String connectString, String description) {
			this.zkClusterKey = zkClusterKey;
			this.alias = alias;
			this.connectString = connectString;
			this.description = description;
		}

		public String toContent() {
			return String.format("zkClusterKey=%s&alias=%s&connectString=%s&description=%s", zkClusterKey, alias,
					connectString, description);
		}

		public String getZkClusterKey() {
			return zkClusterKey;
		}

		public void setZkClusterKey(String zkClusterKey) {
			this.zkClusterKey = zkClusterKey;
		}

		public String getAlias() {
			return alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}

		public String getConnectString() {
			return connectString;
		}

		public void setConnectString(String connectString) {
			this.connectString = connectString;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}