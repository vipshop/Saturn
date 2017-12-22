package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleGUIException;
import com.vip.saturn.job.console.vo.Namespace;
import com.vip.saturn.job.console.vo.Namespaces;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
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
    public ResponseEntity<RequestResult> getNamespaces(final HttpServletRequest request) throws SaturnJobConsoleException {
        Namespaces namespaces = new Namespaces();
        List<String> namespacesList = registryCenterService.getNamespaces();
        if (namespacesList != null) {
            namespaces.getNamespaces().addAll(namespacesList);
        }
        return new ResponseEntity<>(new RequestResult(true, namespaces), HttpStatus.OK);
    }

    @RequestMapping(value = "/namespaces/{namespace}", method = RequestMethod.GET)
    public ResponseEntity<RequestResult> getNamespace(final HttpServletRequest request, @PathVariable("namespace") String namespace) throws SaturnJobConsoleException {
        checkMissingParameter("namespace", namespace);
        RegistryCenterConfiguration registryCenterConfiguration = registryCenterService.findConfigByNamespace(namespace);
        if (registryCenterConfiguration == null) {
            throw new SaturnJobConsoleGUIException("The namespace is not existing");
        }
        Namespace namespaceInfo = new Namespace();
        namespaceInfo.setNamespace(registryCenterConfiguration);
        return new ResponseEntity<>(new RequestResult(true, namespaceInfo), HttpStatus.OK);
    }

}
