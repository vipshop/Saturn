package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.controller.AbstractController;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * Job page controller.
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/console/job_overview/{namespace:.+}/")
public class JobOverviewController extends AbstractController {

    @Resource
    private JobService jobService;

    @RequestMapping(value = "/jobs", method = RequestMethod.GET)
    public ResponseEntity<RequestResult> list(final HttpServletRequest request, @PathVariable("namespace") String namespace) throws SaturnJobConsoleException {
        checkMissingParameter("namespace", namespace);
        return new ResponseEntity<>(new RequestResult(true, jobService.jobs(namespace)), HttpStatus.OK);
    }

    @RequestMapping(value = "/groups", method = RequestMethod.GET)
    public ResponseEntity<RequestResult> groups(final HttpServletRequest request, @PathVariable("namespace") String namespace) throws SaturnJobConsoleException {
        checkMissingParameter("namespace", namespace);
        return new ResponseEntity<>(new RequestResult(true, jobService.groups(namespace)), HttpStatus.OK);
    }

}
