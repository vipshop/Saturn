package com.vip.saturn.job.console.controller.gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.vip.saturn.job.console.controller.AbstractController;
import com.vip.saturn.job.console.exception.SaturnJobConsoleUIException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ZkCluster;

/**
 * Home page controller.
 */
@Controller
@RequestMapping("/console/home")
public class HomePageController extends AbstractController {

	@RequestMapping(value = "/namespaces", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> getNamespaces(final HttpServletRequest request) throws SaturnJobConsoleUIException {
		try {
			Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
			Map<String, Object> obj = new HashMap<>();
			obj.put("namespaces", zkClusterList);
			return new ResponseEntity<>(new RequestResult(true, obj), HttpStatus.OK);
		} catch (Exception e) {
			throw new SaturnJobConsoleUIException(e.getCause());
		}
	}
	
}
