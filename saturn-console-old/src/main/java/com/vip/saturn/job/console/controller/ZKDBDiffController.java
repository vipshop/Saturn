/**
 * Copyright 1999-2015 dangdang.com. <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at <p>
 * http://www.apache.org/licenses/LICENSE-2.0 <p> Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License. </p>
 */

package com.vip.saturn.job.console.controller;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vip.saturn.job.console.domain.JobDiffInfo;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.NamespaceZkClusterMappingService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.ZkDBDiffService;
import com.vip.saturn.job.console.service.helper.ZkClusterMappingUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/")
public class ZKDBDiffController extends AbstractController {

	private static final int HTTP_CONNECTION_TIMEOUT_IN_MILL_SEC = 20000;

	private static final int HTTP_SESSION_TIMEOUT_IN_MILL_SEC = 500000;

	private static Logger LOGGER = LoggerFactory.getLogger(ServerController.class);

	@Resource
	private ZkDBDiffService zkDBDiffService;

	@Resource
	private NamespaceZkClusterMappingService namespaceZkClusterMappingService;

	@Resource
	private SystemConfigService systemConfigService;

	private Gson gson = new Gson();

	/**
	 * 返回对比页面
	 *
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "zk_db_diff", method = RequestMethod.GET)
	public String zkDbDiff(final ModelMap model, HttpServletRequest request) {
		try {
			model.put("zkClusters", namespaceZkClusterMappingService.getZkClusterListWithOnline());
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return "zk_db_diff";
	}

	/**
	 * 按照zk集群，对比zk和db的数据差异。<br> 如果console和zk集群不是在同一机房，则会将调用zk所属机房的console rest api去进行对比，如果对比失败，则继续在本机房进行对比；
	 *
	 * @param request
	 * @param zkCluster
	 * @return
	 */
	@RequestMapping(value = "zk_db_diff/diffByCluster", method = RequestMethod.GET)
	@ResponseBody
	public RequestResult diffByCluster(HttpServletRequest request, String zkCluster) {
		RequestResult requestResult = new RequestResult();
		List<JobDiffInfo> resultList = null;
		try {
			if (!ZkClusterMappingUtils.isCurrentConsoleInTheSameIdc(systemConfigService, zkCluster)) {
				resultList = relayDiffIfPossible(zkCluster);
			} else {
				resultList = zkDBDiffService.diffByCluster(zkCluster);
			}
			requestResult.setSuccess(true);
			requestResult.setObj(resultList);
		} catch (Exception e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.toString());
		}

		return requestResult;
	}

	private List<JobDiffInfo> relayDiffIfPossible(String zkCluster)
			throws SaturnJobConsoleException, InterruptedException {
		try {
			return relayDiff(zkCluster);
		} catch (SaturnJobConsoleException e) {
			LOGGER.warn("fail to relay diff: {}", e.getMessage(), e);
			return zkDBDiffService.diffByCluster(zkCluster);
		}
	}

	/**
	 * 远程对比db和zk的数据差异
	 *
	 * @param zkCluster
	 * @return
	 * @throws SaturnJobConsoleException
	 */
	private List<JobDiffInfo> relayDiff(String zkCluster) throws SaturnJobConsoleException {
		String consoleHost = ZkClusterMappingUtils.getConsoleDomainByZkClusterKey(systemConfigService, zkCluster);
		if (StringUtils.isBlank(consoleHost)) {
			throw new SaturnJobConsoleException(
					String.format("The console domain is not found by zkClusterKey(%s)", zkCluster));
		}
		return relayDiff(zkCluster, consoleHost);
	}

	/**
	 * Relay diff 请求到指定console的REST API.
	 */
	public List<JobDiffInfo> relayDiff(String zkCluster, String consoleUri) throws SaturnJobConsoleException {
		String targetUrl = buildTargetUrl(zkCluster, consoleUri);
		LOGGER.info("send diff request to url {}", targetUrl);

		CloseableHttpClient httpClient = null;
		try {
			// prepare
			httpClient = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(targetUrl);

			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(HTTP_CONNECTION_TIMEOUT_IN_MILL_SEC)
					.setSocketTimeout(HTTP_SESSION_TIMEOUT_IN_MILL_SEC).build();

			request.setConfig(requestConfig);
			request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			// send request
			CloseableHttpResponse httpResponse = httpClient.execute(request);
			// handle response
			return handleResponse(httpResponse);
		} catch (SaturnJobConsoleException se) {
			throw se;
		} catch (Exception e) {
			throw new SaturnJobConsoleException("Exception throws during relay diff", e);
		} finally {
			if (httpClient != null) {
				try {
					httpClient.close();
				} catch (IOException e) {
					LOGGER.error("Exception during httpclient closed.", e);
				}
			}
		}
	}

	private String buildTargetUrl(String zkCluster, String consoleDomain) throws SaturnJobConsoleException {
		StringBuilder sb = new StringBuilder();
		if (!consoleDomain.startsWith("http")) {
			sb.append("http://");
		}
		return sb.append(consoleDomain + "/rest/v1/diff?zkcluster=" + zkCluster).toString();
	}

	private List<JobDiffInfo> handleResponse(CloseableHttpResponse httpResponse)
			throws IOException, SaturnJobConsoleException {
		int status = httpResponse.getStatusLine().getStatusCode();

		if (status == HttpStatus.SC_OK) {
			String responseBody = EntityUtils.toString(httpResponse.getEntity());
			List<JobDiffInfo> result = gson.fromJson(responseBody, new TypeToken<List<JobDiffInfo>>() {
			}.getType());

			LOGGER.info("Get diff info from relay site successfully, size={}", result.size());
			return result;
		}

		if (status >= HttpStatus.SC_BAD_REQUEST && status <= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
			String responseBody = EntityUtils.toString(httpResponse.getEntity());
			if (StringUtils.isNotBlank(responseBody)) {
				String errMsg = JSONObject.parseObject(responseBody).getString("message");
				throw new SaturnJobConsoleException(errMsg);
			} else {
				throw new SaturnJobConsoleException("internal server error");
			}
		} else {
			// if have unexpected status, then throw RuntimeException directly.
			throw new SaturnJobConsoleException("unexpected status returned from Saturn Server.");
		}
	}

	/**
	 * 对比zk和db的job config数据差异。所有对比都在当前console进行。
	 *
	 * @param request
	 * @param jobName   作业名
	 * @param namespace
	 * @return
	 */
	@RequestMapping(value = "zk_db_diff/diffByJob", method = RequestMethod.GET)
	@ResponseBody
	public RequestResult diffByJob(HttpServletRequest request, String jobName, String namespace) {
		RequestResult requestResult = new RequestResult();
		try {
			JobDiffInfo jobDiffInfo = zkDBDiffService.diffByJob(namespace, jobName);
			requestResult.setSuccess(true);
			requestResult.setObj(jobDiffInfo);
		} catch (Exception e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.toString());
		}

		return requestResult;
	}

}
