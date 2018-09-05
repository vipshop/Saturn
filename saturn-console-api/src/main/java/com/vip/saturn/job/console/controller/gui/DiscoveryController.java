package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.controller.rest.DiscoveryRestApiController;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * System config related operations.
 *
 * @author ray.leung
 */
@RequestMapping("/console/discovery")
public class DiscoveryController extends AbstractGUIController {

	@Resource
	private DiscoveryRestApiController discoveryRestApiController;

	/**
	 * 获取所有系统配置信息。
	 */
	@Audit(type = AuditType.REST)
	@RequestMapping(method = {RequestMethod.POST, RequestMethod.GET}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity getConfigs(HttpServletRequest request) throws SaturnJobConsoleException {
		return new SuccessResponseEntity(discoveryRestApiController.getConfigs(request).getBody());
	}
}
