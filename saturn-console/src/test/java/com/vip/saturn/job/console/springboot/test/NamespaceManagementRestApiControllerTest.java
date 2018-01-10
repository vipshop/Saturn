package com.vip.saturn.job.console.springboot.test;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.vip.saturn.job.console.AbstractSaturnConsoleTest;
import com.vip.saturn.job.console.controller.rest.NamespaceManagementRestApiController;
import com.vip.saturn.job.console.domain.NamespaceDomainInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.RegistryCenterService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(NamespaceManagementRestApiController.class)
public class NamespaceManagementRestApiControllerTest extends AbstractSaturnConsoleTest {

	private final Gson gson = new Gson();

	@Autowired
	private MockMvc mvc;

	@MockBean
	private RegistryCenterService registryCenterService;


	@Test
	public void createNamespaceSuccessfully() throws Exception {
		NamespaceDomainInfo namespaceDomainInfo = new NamespaceDomainInfo();
		namespaceDomainInfo.setNamespace("testns");
		namespaceDomainInfo.setZkCluster("default");
		namespaceDomainInfo.setContent("");

		mvc.perform(post("/rest/v1/namespaces").contentType(MediaType.APPLICATION_JSON).content(gson.toJson(namespaceDomainInfo)))
				.andExpect(status().isCreated());

		ArgumentCaptor<NamespaceDomainInfo> argument = ArgumentCaptor.forClass(NamespaceDomainInfo.class);
		verify(registryCenterService).createNamespace(argument.capture());
		assertTrue("namespaceDomainInfo is not equal", namespaceDomainInfo.equals(argument.getValue()));
	}

	@Test
	public void createNamespaceFailAsMissingMandetoryParameter() throws Exception {
		NamespaceDomainInfo namespaceDomainInfo = new NamespaceDomainInfo();
		namespaceDomainInfo.setNamespace("testns");

		MvcResult result = mvc.perform(post("/rest/v1/namespaces").contentType(MediaType.APPLICATION_JSON).content(gson.toJson(namespaceDomainInfo)))
				.andExpect(status().isBadRequest()).andReturn();

		String message = fetchErrorMessage(result);
		assertEquals("error message not equal", "Invalid request. Missing parameter: {zkCluster}", message);

		namespaceDomainInfo.setZkCluster("default");
		namespaceDomainInfo.setNamespace(null);

		result = mvc.perform(post("/rest/v1/namespaces").contentType(MediaType.APPLICATION_JSON).content(gson.toJson(namespaceDomainInfo)))
				.andExpect(status().isBadRequest()).andReturn();
		message = fetchErrorMessage(result);
		assertEquals("error message not equal", "Invalid request. Missing parameter: {namespace}", message);
	}


	@Test
	public void querySuccessfully() throws Exception {
		String ns = "testns";
		String zkCluster = "default";

		NamespaceDomainInfo namespaceDomainInfo = new NamespaceDomainInfo();
		namespaceDomainInfo.setNamespace(ns);
		namespaceDomainInfo.setZkCluster(zkCluster);

		given(registryCenterService.getNamespace("testns")).willReturn(namespaceDomainInfo);

		MvcResult result = mvc.perform(get("/rest/v1/namespaces/" + ns)).andExpect(status().isOk()).andReturn();
		String body = result.getResponse().getContentAsString();
		Map<String, Object> resultMap = JSONObject.parseObject(body, Map.class);
		assertEquals("zkcluster not equal", zkCluster, resultMap.get("zkCluster"));
		assertEquals("namespace not equal", ns, resultMap.get("namespace"));
	}

	@Test
	public void queryFailAsNamespaceNotFound() throws Exception {
		String ns = "testns";

		given(registryCenterService.getNamespace(ns)).willThrow(
				new SaturnJobConsoleHttpException(HttpStatus.NOT_FOUND.value(), "The namespace does not exists."));

		MvcResult result = mvc.perform(get("/rest/v1/namespaces/" + ns)).andExpect(status().isNotFound()).andReturn();
		String message = fetchErrorMessage(result);
		assertEquals("error message not equal", "The namespace does not exists.", message);
	}
}