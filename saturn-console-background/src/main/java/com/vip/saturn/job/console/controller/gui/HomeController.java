package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.controller.AbstractController;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleUIException;
import com.vip.saturn.job.console.vo.Namespace;
import com.vip.saturn.job.console.vo.Namespaces;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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
public class HomeController extends AbstractController {

    @RequestMapping(value = "/namespaces", method = RequestMethod.GET)
    public ResponseEntity<RequestResult> getNamespaces(final HttpServletRequest request) throws SaturnJobConsoleUIException {
        try {
            Namespaces namespaces = new Namespaces();
            List<String> namespacesList = registryCenterService.getNamespaces();
            if (namespacesList != null) {
                namespaces.getNamespaces().addAll(namespacesList);
            }
            return new ResponseEntity<>(new RequestResult(true, namespaces), HttpStatus.OK);
        } catch (Exception e) {
            throw new SaturnJobConsoleUIException(e.getCause());
        }
    }

    @RequestMapping(value = "/namespace", method = RequestMethod.GET)
    public ResponseEntity<RequestResult> getNamespaces(final HttpServletRequest request, final String namespace) throws SaturnJobConsoleUIException {
        try {
            checkMissingParameter("namespace", namespace);
            RegistryCenterConfiguration registryCenterConfiguration = registryCenterService.findConfigByNamespace(namespace);
            Namespace namespaceInfo = new Namespace();
            namespaceInfo.setNamespace(registryCenterConfiguration);
            return new ResponseEntity<>(new RequestResult(true, namespaceInfo), HttpStatus.OK);
        } catch (Exception e) {
            throw new SaturnJobConsoleUIException(e.getCause());
        }
    }

}
