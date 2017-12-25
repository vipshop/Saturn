package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.controller.AbstractController;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.JobService;
import com.vip.saturn.job.console.vo.JobInfo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Job page controller.
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/console/{namespace:.+}/jobs")
public class JobsController extends AbstractController {

    @Resource
    private JobService jobService;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<RequestResult> list(final HttpServletRequest request, @PathVariable("namespace") String namespace) throws SaturnJobConsoleException {
        checkMissingParameter("namespace", namespace);
        List<JobInfo> jobInfoList = new ArrayList<>();
        List<JobInfo> list = jobService.list(namespace);
        if (list != null) {
            jobInfoList.addAll(list);
        }
        return new ResponseEntity<>(new RequestResult(true, jobInfoList), HttpStatus.OK);
    }

}
