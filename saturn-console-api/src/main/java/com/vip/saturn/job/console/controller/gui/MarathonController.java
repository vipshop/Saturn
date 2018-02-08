package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.SaturnEnvProperties;
import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.AddContainerModel;
import com.vip.saturn.job.console.domain.container.ContainerConfig;
import com.vip.saturn.job.console.domain.container.ContainerToken;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.MarathonService;
import com.vip.saturn.job.console.utils.CronExpression;
import com.vip.saturn.job.console.utils.SaturnConstants;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.HashMap;

/**
 * Marathon management
 *
 * @author hebelala
 */
@RequestMapping("/console/{namespace:.+}/marathon")
public class MarathonController extends AbstractGUIController {

	@Resource
	private MarathonService marathonService;

	@GetMapping(value = "/token")
	public SuccessResponseEntity getToken(@PathVariable String namespace) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(marathonService.getContainerToken(namespace));
	}

	@Audit
	@PostMapping(value = "/token")
	public SuccessResponseEntity saveToken(@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("userName") @RequestParam String userName,
			@AuditParam("password") @RequestParam String password) throws SaturnJobConsoleException {
		marathonService.saveContainerToken(namespace, new ContainerToken(userName, password));
		return new SuccessResponseEntity();
	}

	@GetMapping(value = "/containerVos")
	public SuccessResponseEntity getContainerVos(@PathVariable String namespace) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(marathonService.getContainerVos(namespace));
	}

	@PostMapping(value = "/container")
	@ResponseBody
	public SuccessResponseEntity addContainer(@AuditParam("namespace") @PathVariable String namespace,
			AddContainerModel addContainerModel, HttpServletRequest request) throws SaturnJobConsoleException {
		if (addContainerModel.getContainerToken() == null) {
			throw new SaturnJobConsoleException("Please input container token");
		}
		marathonService.checkContainerTokenNotNull(namespace, addContainerModel.getContainerToken());
		marathonService.saveOrUpdateContainerTokenIfNecessary(namespace, addContainerModel.getContainerToken());

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
			throw new SaturnJobConsoleException("VIP_SATURN_DCOS_REGISTRY_URI is not configured");
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

		marathonService.addContainer(namespace, containerConfig);
		return new SuccessResponseEntity();
	}

	@Audit
	@PostMapping(value = "/containerInstances")
	public SuccessResponseEntity updateContainerInstances(@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("taskId") @RequestParam String taskId,
			@AuditParam("instances") @RequestParam int instances) throws SaturnJobConsoleException {
		if (instances < 0) {
			throw new SaturnJobConsoleException("instances不能小于0");
		}
		marathonService.updateContainerInstances(namespace, taskId, instances);
		return new SuccessResponseEntity();
	}

	@Audit
	@DeleteMapping(value = "/container")
	public SuccessResponseEntity removeContainer(@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("taskId") @RequestParam String taskId) throws SaturnJobConsoleException {
		marathonService.removeContainer(namespace, taskId);
		return new SuccessResponseEntity();
	}

	@GetMapping(value = "/containerDetail")
	public SuccessResponseEntity getContainerDetail(@PathVariable String namespace, @RequestParam String taskId)
			throws SaturnJobConsoleException {
		return new SuccessResponseEntity(marathonService.getContainerDetail(namespace, taskId));
	}

	@GetMapping(value = "/registryCatalog")
	public SuccessResponseEntity getRegistryCatalog(@PathVariable String namespace) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(marathonService.getRegistryCatalog(namespace));
	}

	@GetMapping(value = "/registryRepositoryTags")
	public SuccessResponseEntity getRegistryRepositoryTags(@PathVariable String namespace,
			@RequestParam String repository) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(marathonService.getRegistryRepositoryTags(namespace, repository));
	}

	@GetMapping(value = "/timeZoneIds")
	public SuccessResponseEntity getTimeZoneIds(@PathVariable String namespace) {
		return new SuccessResponseEntity(SaturnConstants.TIME_ZONE_IDS);
	}

	@Audit
	@PostMapping(value = "/containerScaleJob")
	public SuccessResponseEntity addContainerScaleJob(@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("taskId") @RequestParam String taskId,
			@AuditParam("jobDesc") @RequestParam String jobDesc,
			@AuditParam("instances") @RequestParam int instances,
			@AuditParam("timeZone") @RequestParam String timeZone,
			@AuditParam("cron") @RequestParam String cron) throws SaturnJobConsoleException {
		if (instances < 0) {
			throw new SaturnJobConsoleException("instances不能小于0");
		}
		try {
			CronExpression.validateExpression(cron);
		} catch (ParseException e) {
			throw new SaturnJobConsoleException(String.format("cron(%s)无效:%s", cron, e.getMessage()));
		}
		marathonService.addContainerScaleJob(namespace, taskId, jobDesc, instances, timeZone, cron);
		return new SuccessResponseEntity();
	}

	@GetMapping(value = "/containerScaleJobVo")
	public SuccessResponseEntity getContainerScaleJob(@PathVariable String namespace,
			@RequestParam String taskId, @RequestParam String jobName) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(marathonService.getContainerScaleJobVo(namespace, taskId, jobName));
	}

	@Audit
	@PostMapping(value = "/enableContainerScaleJob")
	public SuccessResponseEntity enableContainerScaleJob(@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobName") @RequestParam String jobName) throws SaturnJobConsoleException {
		marathonService.enableContainerScaleJob(namespace, jobName, true);
		return new SuccessResponseEntity();
	}

	@Audit
	@PostMapping(value = "/disableContainerScaleJob")
	public SuccessResponseEntity disableContainerScaleJob(@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("jobName") @RequestParam String jobName) throws SaturnJobConsoleException {
		marathonService.enableContainerScaleJob(namespace, jobName, false);
		return new SuccessResponseEntity();
	}

	@Audit
	@DeleteMapping(value = "/containerScaleJob")
	public SuccessResponseEntity deleteContainerScaleJob(@AuditParam("namespace") @PathVariable String namespace,
			@AuditParam("taskId") @RequestParam String taskId,
			@AuditParam("jobName") @RequestParam String jobName) throws SaturnJobConsoleException {
		marathonService.deleteContainerScaleJob(namespace, taskId, jobName);
		return new SuccessResponseEntity();
	}

}
