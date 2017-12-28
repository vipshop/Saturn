package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleGUIException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Home page controller.
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/console/home")
public class HomeController extends AbstractGUIController {

	@RequestMapping(value = "/namespaces", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> getNamespaces(final HttpServletRequest request)
			throws SaturnJobConsoleException {
		List<String> namespaceList = new ArrayList<>();
		List<String> temp = registryCenterService.getNamespaces();
		if (temp != null) {
			namespaceList.addAll(temp);
		}
		return new ResponseEntity<>(new RequestResult(true, namespaceList), HttpStatus.OK);
	}

	@RequestMapping(value = "/namespace", method = RequestMethod.GET)
	public ResponseEntity<RequestResult> getNamespace(final HttpServletRequest request, @RequestParam String namespace)
			throws SaturnJobConsoleException {
		RegistryCenterConfiguration registryCenterConfiguration = registryCenterService
				.findConfigByNamespace(namespace);
		if (registryCenterConfiguration == null) {
			throw new SaturnJobConsoleGUIException("The namespace is not existing");
		}
		return new ResponseEntity<>(new RequestResult(true, registryCenterConfiguration), HttpStatus.OK);
	}

}
