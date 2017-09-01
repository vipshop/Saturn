package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.domain.AddContainerModel;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.container.ContainerConfig;
import com.vip.saturn.job.console.domain.container.ContainerToken;
import com.vip.saturn.job.console.domain.container.vo.ContainerScaleJobVo;
import com.vip.saturn.job.console.domain.container.vo.ContainerVo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.ContainerService;
import com.vip.saturn.job.console.utils.CronExpression;
import com.vip.saturn.job.console.utils.SaturnConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

/**
 * @author hebelala
 */
@RestController
@RequestMapping("/container")
public class ContainerController extends AbstractController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContainerController.class);

	@Resource
	private ContainerService containerService;

	@RequestMapping(value = "getContainerToken", method = RequestMethod.GET)
	public RequestResult getContainerToken(HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			Object containerToken = containerService.getContainerToken();
			requestResult.setSuccess(true);
			requestResult.setObj(containerToken);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "saveOrUpdateContainerToken", method = RequestMethod.POST)
	public RequestResult saveOrUpdateContainerToken(ContainerToken containerToken, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			if (containerToken == null) {
				throw new SaturnJobConsoleException("Please input container token");
			}
			containerService.checkContainerTokenNotNull(containerToken);
			containerService.saveOrUpdateContainerToken(containerToken);
			requestResult.setSuccess(true);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "/getContainerVos", method = RequestMethod.GET)
	public RequestResult getContainerVos(HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			List<ContainerVo> containerVos = containerService.getContainerVos();
			requestResult.setSuccess(true);
			requestResult.setObj(containerVos);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "/addContainer", method = RequestMethod.POST)
	@ResponseBody
	public RequestResult addContainer(AddContainerModel addContainerModel, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			if (addContainerModel.getContainerToken() == null) {
				throw new SaturnJobConsoleException("Please input container token");
			}
			containerService.checkContainerTokenNotNull(addContainerModel.getContainerToken());
			containerService.saveOrUpdateContainerTokenIfNecessary(addContainerModel.getContainerToken());

			ContainerConfig containerConfig = new ContainerConfig();
			containerConfig.setTaskId(addContainerModel.getTaskId());
			containerConfig.setCmd(addContainerModel.getCmd());
			containerConfig.setCpus(addContainerModel.getCpus());
			containerConfig.setMem(addContainerModel.getMem());
			containerConfig.setInstances(addContainerModel.getInstances());
			containerConfig.setConstraints(addContainerModel.getConstraints());
			containerConfig.setEnv(addContainerModel.getEnv());
			containerConfig.setPrivileged(
					addContainerModel.getPrivileged() == null ? false : addContainerModel.getPrivileged());
			containerConfig.setForcePullImage(
					addContainerModel.getForcePullImage() == null ? true : addContainerModel.getForcePullImage());
			containerConfig.setParameters(addContainerModel.getParameters());
			containerConfig.setVolumes(addContainerModel.getVolumes());
			containerConfig.setImage(addContainerModel.getImage());
			containerConfig.setCreateTime(System.currentTimeMillis());

			String imageNew = "";
			String vipSaturnDcosRegistryUri = SaturnEnvProperties.VIP_SATURN_DCOS_REGISTRY_URI;
			if (vipSaturnDcosRegistryUri == null || vipSaturnDcosRegistryUri.trim().length() == 0) {
				String errorMsg = "VIP_SATURN_DCOS_REGISTRY_URI is not configured";
				requestResult.setSuccess(false);
				requestResult.setMessage(errorMsg);
				return requestResult;
			} else {
				if (vipSaturnDcosRegistryUri.startsWith("http://")) {
					String tmp = vipSaturnDcosRegistryUri.substring("http://".length());
					while (tmp.endsWith("/")) {
						tmp = tmp.substring(0, tmp.length() - 1);
					}
					imageNew = tmp + "/" + addContainerModel.getImage();
				} else if (vipSaturnDcosRegistryUri.startsWith("https://")) {
					String tmp = vipSaturnDcosRegistryUri.substring("https://".length());
					while (tmp.endsWith("/")) {
						tmp = tmp.substring(0, tmp.length() - 1);
					}
					imageNew = tmp + "/" + addContainerModel.getImage();
				}
			}
			containerConfig.setImage(imageNew);

			if (containerConfig.getEnv() == null) {
				containerConfig.setEnv(new HashMap<String, String>());
			}
			if (!containerConfig.getEnv().containsKey(SaturnEnvProperties.NAME_VIP_SATURN_ZK_CONNECTION)) {
				containerConfig.getEnv().put(SaturnEnvProperties.NAME_VIP_SATURN_ZK_CONNECTION,
						getCurrentZkAddr(request.getSession()));
			}

			containerService.addContainer(containerConfig);
			requestResult.setSuccess(true);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}

		return requestResult;
	}

	@RequestMapping(value = "/updateContainerInstances", method = RequestMethod.POST)
	public RequestResult updateContainerInstances(String taskId, Integer instances, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			if (taskId == null) {
				throw new SaturnJobConsoleException("The taskId cannot be null");
			}
			if (instances == null || instances < 0) {
				throw new SaturnJobConsoleException("Please input the positive instances");
			}
			containerService.updateContainerInstances(taskId, instances);
			requestResult.setSuccess(true);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "/removeContainer", method = RequestMethod.POST)
	public RequestResult removeContainer(String taskId, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			if (taskId == null) {
				throw new SaturnJobConsoleException("The taskId cannot be null");
			}
			containerService.removeContainer(taskId);
			requestResult.setSuccess(true);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "/getContainerDetail", method = RequestMethod.GET)
	public RequestResult getContainerDetail(String taskId, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			if (taskId == null) {
				throw new SaturnJobConsoleException("The taskId cannot be null");
			}
			String containerDetail = containerService.getContainerDetail(taskId);
			requestResult.setSuccess(true);
			requestResult.setObj(containerDetail);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "/getRegistryCatalog", method = RequestMethod.GET)
	public RequestResult getRegistryCatalog(HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			String registryCatalog = containerService.getRegistryCatalog();
			requestResult.setSuccess(true);
			requestResult.setObj(registryCatalog);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "/getRegistryRepositoryTags", method = RequestMethod.GET)
	public RequestResult getRegistryRepositoryTags(String repository, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			if (repository == null) {
				throw new SaturnJobConsoleException("The repository cannot be null");
			}
			String constraints = containerService.getRegistryRepositoryTags(repository);
			requestResult.setSuccess(true);
			requestResult.setObj(constraints);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "/getTimeZoneIds", method = RequestMethod.GET)
	public RequestResult getTimeZoneIds(HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		requestResult.setSuccess(true);
		requestResult.setObj(SaturnConstants.TIME_ZONE_IDS);
		return requestResult;
	}

	@RequestMapping(value = "/addContainerScaleJob", method = RequestMethod.POST)
	public RequestResult addContainerScaleJob(String taskId, String jobDesc, Integer instances, String timeZone,
			String cron, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			if (taskId == null) {
				throw new SaturnJobConsoleException("The taskId cannot be null");
			}
			if (jobDesc == null) {
				throw new SaturnJobConsoleException("The jobDesc cannot be null");
			}
			if (instances == null || instances < 0) {
				throw new SaturnJobConsoleException("Please input the positive instances");
			}
			if (timeZone == null || timeZone.trim().length() == 0) {
				throw new SaturnJobConsoleException("The timeZone cannot be null or empty");
			}
			if (cron == null || cron.trim().length() == 0) {
				throw new SaturnJobConsoleException("The cron cannot be null or empty");
			}
			try {
				CronExpression.validateExpression(cron);
			} catch (ParseException e) {
				throw new SaturnJobConsoleException("The cron is not valid, " + e.toString());
			}
			containerService.addContainerScaleJob(taskId, jobDesc, instances, timeZone, cron);
			requestResult.setSuccess(true);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "/getContainerScaleJobVo", method = RequestMethod.GET)
	public RequestResult getContainerScaleJob(String taskId, String jobName, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			if (taskId == null) {
				throw new SaturnJobConsoleException("The taskId cannot be null");
			}
			if (jobName == null) {
				throw new SaturnJobConsoleException("The jobName cannot be null");
			}
			ContainerScaleJobVo containerScaleJob = containerService.getContainerScaleJobVo(taskId, jobName);
			requestResult.setSuccess(true);
			requestResult.setObj(containerScaleJob);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "/enableContainerScaleJob", method = RequestMethod.POST)
	public RequestResult enableContainerScaleJob(String jobName, Boolean enable, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			if (jobName == null) {
				throw new SaturnJobConsoleException("The jobName cannot be null");
			}
			if (enable == null) {
				throw new SaturnJobConsoleException("The enable cannot be null");
			}
			containerService.enableContainerScaleJob(jobName, enable);
			requestResult.setSuccess(true);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "/deleteContainerScaleJob", method = RequestMethod.POST)
	public RequestResult deleteContainerScaleJob(String taskId, String jobName, HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			if (taskId == null) {
				throw new SaturnJobConsoleException("The taskId cannot be null");
			}
			if (jobName == null) {
				throw new SaturnJobConsoleException("The jobName cannot be null");
			}
			containerService.deleteContainerScaleJob(taskId, jobName);
			requestResult.setSuccess(true);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
		} catch (Throwable t) {
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

}
