package com.vip.saturn.job.console.controller;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.DashboardService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.ZkClusterMappingUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author timmy.hu
 */
@Controller
@RequestMapping("/")
public class DashboardRefreshController extends AbstractController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DashboardRefreshController.class);

	private static final int CONNECT_TIMEOUT_MS = 10000;

	private static final int SO_TIMEOUT_MS = 180000;

	@Autowired
	private SystemConfigService systemConfigService;

	@Autowired
	private DashboardService dashboardService;

	@RequestMapping(value = "dashboard_refresh_page", method = RequestMethod.GET)
	public String dashboardRefreshPage(final ModelMap model, HttpServletRequest request) {
		return "dashboard_refresh";
	}

	@RequestMapping(value = "dashboardRefresh", method = RequestMethod.POST)
	@ResponseBody
	public RequestResult dashboardRefresh(String zkClusterKey, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			long beforeRefresh = System.currentTimeMillis();
			doDashboardRefresh(zkClusterKey);
			long afterRefresh = System.currentTimeMillis();
			long takeTime = afterRefresh - beforeRefresh;
			requestResult.setSuccess(true);
			requestResult.setObj(takeTime);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			requestResult.setSuccess(false);
			requestResult.setMessage(e.toString());
		}
		return requestResult;
	}

	private void doDashboardRefresh(String zkClusterKey) throws SaturnJobConsoleException {
		boolean isSameIdc = ZkClusterMappingUtils.isCurrentConsoleInTheSameIdc(systemConfigService, zkClusterKey);
		if (isSameIdc) {
			LOGGER.info("the zk and the console are in the same IDC,refreshStatistics2DB in the current Console");
			dashboardService.refreshStatistics2DB(zkClusterKey);
		} else {
			LOGGER.info("the zk and the console are in different IDC,forward the refresh request to remote console");
			try {
				forwardDashboardRefreshToRemote(zkClusterKey);
			} catch (SaturnJobConsoleException e) {
				LOGGER.error(e.getMessage(), e);
				LOGGER.info("remote refresh request error,so refreshStatistics2DB in the current Console");
				dashboardService.refreshStatistics2DB(zkClusterKey);
			}
		}
	}

	private void forwardDashboardRefreshToRemote(String zkClusterKey) throws SaturnJobConsoleException {
		CloseableHttpClient httpClient = null;
		String url = null;
		try {
			String domain = ZkClusterMappingUtils.getConsoleDomainByZkClusterKey(systemConfigService, zkClusterKey);
			if (StringUtils.isBlank(domain)) {
				throw new SaturnJobConsoleException(
						String.format("The console domain is not found by zkClusterKey(%s)", zkClusterKey));
			}
			url = domain + "/rest/v1/dashboard/refresh?zkClusterKey=" + zkClusterKey;
			httpClient = HttpClientBuilder.create().build();
			HttpPost httpPost = createHttpRequest(url);
			CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
			handleResponse(url, httpResponse);
		} catch (SaturnJobConsoleException se) {
			throw se;
		} catch (Exception e) {
			throw new SaturnJobConsoleException("Fail to execute forwardDashboardRefreshToRemote, Url: " + url, e);
		} finally {
			IOUtils.closeQuietly(httpClient);
		}
	}

	private void handleResponse(String url, CloseableHttpResponse httpResponse)
			throws IOException, SaturnJobConsoleException {
		StatusLine statusLine = httpResponse.getStatusLine();
		Integer statusCode = statusLine != null ? statusLine.getStatusCode() : null;
		LOGGER.info("the statusCode of remote request is:" + statusCode);
		if (statusLine != null && statusCode == HttpStatus.SC_OK) {
			String takeTime = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");
			LOGGER.info("forwardDashboardRefreshToRemote Url " + url + ", spend time:" + takeTime);
			return;
		}
		if (statusCode >= HttpStatus.SC_BAD_REQUEST && statusCode <= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
			String responseBody = EntityUtils.toString(httpResponse.getEntity());
			if (StringUtils.isNotBlank(responseBody)) {
				String errMsg = JSONObject.parseObject(responseBody).getString("message");
				throw new SaturnJobConsoleException(errMsg);
			} else {
				throw new SaturnJobConsoleException("internal server error");
			}
		} else {
			throw new SaturnJobConsoleException("unexpected status returned from Saturn Server.");
		}
	}

	private HttpPost createHttpRequest(String url) {
		HttpPost httpPost = new HttpPost(url);
		RequestConfig cfg = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT_MS).setSocketTimeout(SO_TIMEOUT_MS)
				.build();
		httpPost.setConfig(cfg);
		return httpPost;
	}

}
