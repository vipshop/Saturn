package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.NamespaceDomainInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.RestApiService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Namespace management service.
 */
@Controller
@RequestMapping("/rest/v1")
public class NamespaceManagementRestApiController extends AbstractController {

	@Resource
	private RestApiService restApiService;

	@RequestMapping(value = "/namespaces", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> create(@RequestBody Map<String, Object> reqParams, HttpServletRequest request) throws SaturnJobConsoleException {
		try {
			NamespaceDomainInfo namespaceInfo = constructNamespaceDomainInfo(reqParams);
			restApiService.createNamespace(namespaceInfo);
			//TODO: 改成异步
			registryCenterService.refreshRegCenter();

			return new ResponseEntity<>(HttpStatus.CREATED);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/namespaces/{namespace:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> query(@PathVariable("namespace") String namespace) throws SaturnJobConsoleException {
		HttpHeaders httpHeaders = new HttpHeaders();
		try {
			checkMissingParameter("namespace", namespace);
			NamespaceDomainInfo namespaceDomainInfo = restApiService.getNamespace(namespace);
			return new ResponseEntity<Object>(namespaceDomainInfo, httpHeaders, HttpStatus.OK);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	private NamespaceDomainInfo constructNamespaceDomainInfo(Map<String, Object> reqParams) throws SaturnJobConsoleException {
		NamespaceDomainInfo namespaceInfo = new NamespaceDomainInfo();

		namespaceInfo.setNamespace(checkAndGetParametersValueAsString(reqParams, "namespace", true));
		namespaceInfo.setZkCluster(checkAndGetParametersValueAsString(reqParams, "zkCluster", true));

		return namespaceInfo;
	}

}
