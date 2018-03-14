package com.vip.saturn.job.console.service.impl.marathon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.domain.container.ContainerConfig;
import com.vip.saturn.job.console.domain.container.ContainerStatus;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.impl.marathon.entity.Tasks;
import com.vip.saturn.job.console.service.impl.marathon.entity.WrapperApp;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author hebelala
 */
public class MarathonRestClient {
 	private static final String AUTHORIZATION_DES = "Authorization";
	private static final String BASIC_DES = "Basic ";
	private static final String UTF8_DES = "UTF-8";
	private static final String API_VERSION_DES = "/v2/apps/";

	private static Logger log = LoggerFactory.getLogger(MarathonRestClient.class);

	private static String getEntityContent(HttpEntity entity) throws IOException {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()))) {
			StringBuilder sb = new StringBuilder();
			int maxLen = 1024;
			char[] cBuf = new char[maxLen];
			int readLen = -1;
			while ((readLen = in.read(cBuf, 0, maxLen)) != -1) {
				sb.append(cBuf, 0, readLen);
			}
			return sb.toString();
		}
	}

	public static void deploy(String userName, String password, ContainerConfig containerConfig)
			throws SaturnJobConsoleException {
		JSONObject deployFormModel = new JSONObject();
		deployFormModel.put("id", containerConfig.getTaskId());
		deployFormModel.put("instances", containerConfig.getInstances());
		deployFormModel.put("cmd", containerConfig.getCmd());
		deployFormModel.put("cpus", containerConfig.getCpus());
		deployFormModel.put("mem", containerConfig.getMem());
		deployFormModel.put("constraints", containerConfig.getConstraints());
		JSONObject container = new JSONObject();
		JSONObject docker = new JSONObject();
		docker.put("forcePullImage", containerConfig.getForcePullImage());
		docker.put("image", containerConfig.getImage());
		docker.put("network", "BRIDGE");
		docker.put("parameters", containerConfig.getParameters());
		docker.put("privileged", containerConfig.getPrivileged());
		JSONArray portMappings = new JSONArray();
		JSONObject portMapping = new JSONObject();
		portMapping.put("containerPort", 24500); // equal to jmx port
		portMapping.put("hostPort", 0);
		portMapping.put("servicePort", 0);
		portMapping.put("protocol", "tcp");
		portMappings.add(portMapping);
		docker.put("portMappings", portMappings);
		container.put("docker", docker);
		container.put("type", "DOCKER");
		container.put("volumes", containerConfig.getVolumes());
		deployFormModel.put("container", container);
		deployFormModel.put("env", containerConfig.getEnv());
		JSONArray healthChecks = new JSONArray();
		JSONObject healthCheck = new JSONObject();
		healthCheck.put("protocol", "TCP");
		healthChecks.add(healthCheck);
		deployFormModel.put("healthChecks", healthChecks);

		String urlStr = SaturnEnvProperties.VIP_SATURN_DCOS_REST_URI + "/v2/apps";
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpPost httpPost = new HttpPost(urlStr);
			httpPost.setHeader(AUTHORIZATION_DES,
					BASIC_DES + Base64.encodeBase64String((userName + ":" + password).getBytes(UTF8_DES)));
			httpPost.setHeader("Content-type", "application/json; charset=utf-8");
			httpPost.setEntity(new StringEntity(deployFormModel.toJSONString()));

			CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
			StatusLine statusLine = httpResponse.getStatusLine();
			if (statusLine != null) {
				int statusCode = statusLine.getStatusCode();
				String reasonPhrase = statusLine.getReasonPhrase();
				if ((statusCode != 200) && (statusCode != 201)) {
					HttpEntity entity = httpResponse.getEntity();
					if (entity != null) {
						String entityContent = getEntityContent(entity);
						throw new SaturnJobConsoleException(entityContent);
					} else {
						throw new SaturnJobConsoleException(
								"statusCode is " + statusCode + ", reasonPhrase is " + reasonPhrase);
					}
				}
			} else {
				throw new SaturnJobConsoleException("Not status returned, url is " + urlStr);
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public static void destroy(String userName, String password, String appId) throws SaturnJobConsoleException {
		String urlStr = SaturnEnvProperties.VIP_SATURN_DCOS_REST_URI + API_VERSION_DES + appId;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpDelete httpDelete = new HttpDelete(urlStr);
			httpDelete.setHeader(AUTHORIZATION_DES,
					BASIC_DES + Base64.encodeBase64String((userName + ":" + password).getBytes(UTF8_DES)));
			CloseableHttpResponse httpResponse = httpClient.execute(httpDelete);
			StatusLine statusLine = httpResponse.getStatusLine();
			if (statusLine != null) {
				int statusCode = statusLine.getStatusCode();
				String reasonPhrase = statusLine.getReasonPhrase();
				if (statusCode != 200) {
					HttpEntity entity = httpResponse.getEntity();
					if (entity != null) {
						String entityContent = getEntityContent(entity);
						throw new SaturnJobConsoleException(entityContent);
					} else {
						throw new SaturnJobConsoleException(
								"statusCode is " + statusCode + ", reasonPhrase is " + reasonPhrase);
					}
				}
			} else {
				throw new SaturnJobConsoleException("Not status returned, url is " + urlStr);
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public static int count(String userName, String password, String appId) throws SaturnJobConsoleException {
		String urlStr = SaturnEnvProperties.VIP_SATURN_DCOS_REST_URI + API_VERSION_DES + appId + "/tasks";
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpGet httpGet = new HttpGet(urlStr);
			httpGet.setHeader(AUTHORIZATION_DES,
					BASIC_DES + Base64.encodeBase64String((userName + ":" + password).getBytes(UTF8_DES)));
			CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity entity = httpResponse.getEntity();
			if (entity != null) {
				String entityContent = getEntityContent(entity);
				StatusLine statusLine = httpResponse.getStatusLine();
				if (statusLine != null && statusLine.getStatusCode() == 200) {
					Tasks tasks = JSON.parseObject(entityContent, Tasks.class);
					return tasks != null && tasks.getTaskList() != null ? tasks.getTaskList().size() : 0;
				} else {
					throw new SaturnJobConsoleException(entityContent);
				}
			} else {
				throw new SaturnJobConsoleException("Not status returned, url is " + urlStr);
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public static void scale(String userName, String password, String appId, Integer instances)
			throws SaturnJobConsoleException {
		JSONObject params = new JSONObject();
		params.put("instances", instances);
		String urlStr = SaturnEnvProperties.VIP_SATURN_DCOS_REST_URI + API_VERSION_DES + appId + "?force=true";
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpPut httpPut = new HttpPut(urlStr);
			httpPut.setHeader(AUTHORIZATION_DES,
					BASIC_DES + Base64.encodeBase64String((userName + ":" + password).getBytes(UTF8_DES)));
			httpPut.setHeader("Content-type", "application/json; charset=utf-8");
			httpPut.setEntity(new StringEntity(params.toJSONString()));
			CloseableHttpResponse httpResponse = httpClient.execute(httpPut);
			StatusLine statusLine = httpResponse.getStatusLine();
			if (statusLine != null) {
				int statusCode = statusLine.getStatusCode();
				String reasonPhrase = statusLine.getReasonPhrase();
				if ((statusCode != 200) && (statusCode != 201)) {
					HttpEntity entity = httpResponse.getEntity();
					if (entity != null) {
						String entityContent = getEntityContent(entity);
						throw new SaturnJobConsoleException(entityContent);
					} else {
						throw new SaturnJobConsoleException(
								"statusCode is " + statusCode + ", reasonPhrase is " + reasonPhrase);
					}
				}
			} else {
				throw new SaturnJobConsoleException("Not status returned, url is " + urlStr);
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public static ContainerStatus getContainerStatus(String userName, String password, String appId)
			throws SaturnJobConsoleException {
		String urlStr = SaturnEnvProperties.VIP_SATURN_DCOS_REST_URI + API_VERSION_DES + appId;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpGet httpGet = new HttpGet(urlStr);
			httpGet.setHeader(AUTHORIZATION_DES,
					BASIC_DES + Base64.encodeBase64String((userName + ":" + password).getBytes(UTF8_DES)));
			CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity entity = httpResponse.getEntity();
			if (entity != null) {
				String entityContent = getEntityContent(entity);
				StatusLine statusLine = httpResponse.getStatusLine();
				if (statusLine != null && statusLine.getStatusCode() == 200) {
					WrapperApp app = JSON.parseObject(entityContent, WrapperApp.class);
					ContainerStatus containerStatus = new ContainerStatus();
					containerStatus.setHealthyCount(app.getApp().getTasksHealthy());
					containerStatus.setUnhealthyCount(app.getApp().getTasksUnhealthy());
					containerStatus.setRunningCount(app.getApp().getTasksRunning());
					containerStatus.setStagedCount(app.getApp().getTasksStaged());
					containerStatus.setTotalCount(app.getApp().getInstances());
					return containerStatus;
				} else {
					throw new SaturnJobConsoleException(entityContent);
				}
			} else {
				throw new SaturnJobConsoleException("Not data returned, url is " + urlStr);
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public static String info(String userName, String password, String appId) throws SaturnJobConsoleException {
		String urlStr = SaturnEnvProperties.VIP_SATURN_DCOS_REST_URI + API_VERSION_DES + appId;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpGet httpGet = new HttpGet(urlStr);
			httpGet.setHeader(AUTHORIZATION_DES,
					BASIC_DES + Base64.encodeBase64String((userName + ":" + password).getBytes(UTF8_DES)));
			CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity entity = httpResponse.getEntity();
			if (entity != null) {
				String entityContent = getEntityContent(entity);
				StatusLine statusLine = httpResponse.getStatusLine();
				if (statusLine != null && statusLine.getStatusCode() == 200) {
					return entityContent;
				} else {
					throw new SaturnJobConsoleException(entityContent);
				}
			} else {
				throw new SaturnJobConsoleException("Not data returned, url is " + urlStr);
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public static String getRegistryCatalog() throws SaturnJobConsoleException {
		String urlStr = SaturnEnvProperties.VIP_SATURN_DCOS_REGISTRY_URI + "/v2/_catalog";
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpGet httpGet = new HttpGet(urlStr);
			CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity entity = httpResponse.getEntity();
			if (entity != null) {
				String entityContent = getEntityContent(entity);
				StatusLine statusLine = httpResponse.getStatusLine();
				if (statusLine != null && statusLine.getStatusCode() == 200) {
					return entityContent;
				} else {
					throw new SaturnJobConsoleException(entityContent);
				}
			} else {
				throw new SaturnJobConsoleException("Not data returned, url is " + urlStr);
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public static String getRegistryRepositoriesTagsList(String repository) throws SaturnJobConsoleException {
		String urlStr = SaturnEnvProperties.VIP_SATURN_DCOS_REGISTRY_URI + "/v2/" + repository + "/tags/list";
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			HttpGet httpGet = new HttpGet(urlStr);
			CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity entity = httpResponse.getEntity();
			if (entity != null) {
				String entityContent = getEntityContent(entity);
				StatusLine statusLine = httpResponse.getStatusLine();
				if (statusLine != null && statusLine.getStatusCode() == 200) {
					return entityContent;
				} else {
					throw new SaturnJobConsoleException(entityContent);
				}
			} else {
				throw new SaturnJobConsoleException("Not data returned, url is " + urlStr);
			}
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new SaturnJobConsoleException(e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

}
