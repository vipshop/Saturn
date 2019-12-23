package com.vip.saturn.job.console.controller.rest;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.NamespaceAndJobService;
import org.apache.commons.lang3.StringUtils;
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
	private NamespaceAndJobService namespaceAndJobService;

	@Audit(type = AuditType.REST)
	@RequestMapping(value = "/namespaces/createNamespaceAndImportJobs", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> create(@RequestBody Map<String, Object> reqParams, HttpServletRequest request)
			throws SaturnJobConsoleException {
		try {
			String namespace = checkAndGetParametersValueAsString(reqParams, "namespace", true);
			if (StringUtils.isBlank(namespace)) {
				throw new SaturnJobConsoleException("namespace is empty");
			}
			String zkClusterName = checkAndGetParametersValueAsString(reqParams, "zkCluster", true);
			if (StringUtils.isBlank(zkClusterName)) {
				throw new SaturnJobConsoleException("zkCluster is empty");
			}
			String srcNamespace = checkAndGetParametersValueAsString(reqParams, "srcNamespace", true);
			if (StringUtils.isBlank(srcNamespace)) {
				throw new SaturnJobConsoleException("srcNamespace is empty");
			}
			String createBy = checkAndGetParametersValueAsString(reqParams, "createBy", true);
			namespaceAndJobService.createNamespaceAndCloneJobs(srcNamespace, namespace, zkClusterName, createBy);
			return new ResponseEntity<>(HttpStatus.CREATED);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	@Audit(type = AuditType.REST)
	@RequestMapping(value = "/namespaces/asyncCreateNamespaceAndImportJobs", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> asyncCreate(@RequestBody Map<String, Object> reqParams, HttpServletRequest request)
			throws SaturnJobConsoleException {
		String namespace = checkAndGetParametersValueAsString(reqParams, "namespace", true);
		if (StringUtils.isBlank(namespace)) {
			throw new SaturnJobConsoleException("namespace is empty");
		}
		String zkClusterName = checkAndGetParametersValueAsString(reqParams, "zkCluster", true);
		if (StringUtils.isBlank(zkClusterName)) {
			throw new SaturnJobConsoleException("zkCluster is empty");
		}
		String srcNamespace = checkAndGetParametersValueAsString(reqParams, "srcNamespace", true);
		if (StringUtils.isBlank(srcNamespace)) {
			throw new SaturnJobConsoleException("srcNamespace is empty");
		}
		String createBy = checkAndGetParametersValueAsString(reqParams, "createBy", true);
		namespaceAndJobService.asyncCreateNamespaceAndCloneJobs(srcNamespace, namespace, zkClusterName, createBy);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}
}
