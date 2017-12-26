package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * Job overview page controller.
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/console/job-overview")
public class JobOverviewController extends AbstractGUIController {

    @Resource
    private JobService jobService;

    @RequestMapping(value = "/jobs", method = RequestMethod.GET)
    public ResponseEntity<RequestResult> list(final HttpServletRequest request,
                                              @RequestParam(name = "namespace", required = true) String namespace) throws SaturnJobConsoleException {
        return new ResponseEntity<>(new RequestResult(true, jobService.jobs(namespace)), HttpStatus.OK);
    }

    @RequestMapping(value = "/groups", method = RequestMethod.GET)
    public ResponseEntity<RequestResult> groups(final HttpServletRequest request,
                                                @RequestParam(name = "namespace", required = true) String namespace) throws SaturnJobConsoleException {
        return new ResponseEntity<>(new RequestResult(true, jobService.groups(namespace)), HttpStatus.OK);
    }

}
