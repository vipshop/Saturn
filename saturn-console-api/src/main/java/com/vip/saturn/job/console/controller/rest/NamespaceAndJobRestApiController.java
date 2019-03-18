package com.vip.saturn.job.console.controller.rest;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.domain.NamespaceDomainInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.NamespaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author Ray Leung
 */
@RequestMapping("/rest/v1")
public class NamespaceAndJobRestApiController extends AbstractRestController {

	@Autowired
	private NamespaceService namespaceService;

	@Audit(type = AuditType.REST)
	@RequestMapping(value = "/namespaces/createNamespaceAndImportJobs", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> create(@RequestBody Map<String, Object> reqParams, HttpServletRequest request)
			throws SaturnJobConsoleException {
		try {
			String namespace = checkAndGetParametersValueAsString(reqParams, "namespace", true);
			String zkClusterName = checkAndGetParametersValueAsString(reqParams, "zkCluster", true);
			String srcNamespace = checkAndGetParametersValueAsString(reqParams, "srcNamespace", true);
			String createBy = checkAndGetParametersValueAsString(reqParams, "createBy", true);
			NamespaceDomainInfo namespaceInfo = new NamespaceDomainInfo();
			namespaceInfo.setNamespace(namespace);
			namespaceInfo.setZkCluster(zkClusterName);
			namespaceInfo.setContent("");
			registryCenterService.createNamespace(namespaceInfo);
			registryCenterService.refreshRegistryCenterForNamespace(zkClusterName, srcNamespace);
			namespaceService.importJobsFromNamespaceToNamespace(srcNamespace, namespace, createBy);
			return new ResponseEntity<>(HttpStatus.CREATED);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}
}
