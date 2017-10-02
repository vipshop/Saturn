/**
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.JobDiffInfo;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.service.NamespaceZkClusterMappingService;
import com.vip.saturn.job.console.service.ZkDBDiffService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("/")
public class ZKDBDiffController extends AbstractController {

    private static Logger LOGGER = LoggerFactory.getLogger(ServerController.class);

    @Resource
    private ZkDBDiffService zkDBDiffService;

    @Resource
    private NamespaceZkClusterMappingService namespaceZkClusterMappingService;

    @RequestMapping(value = "zk_db_diff", method = RequestMethod.GET)
    public String zkDbDiff(final ModelMap model, HttpServletRequest request) {
        try {
            model.put("zkClusters", namespaceZkClusterMappingService.getZkClusterListWithOnline());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return "zk_db_diff";
    }

    @RequestMapping(value = "zk_db_diff/diffByCluster", method = RequestMethod.GET)
    @ResponseBody
    public RequestResult diffByCluster(HttpServletRequest request, String zkCluster) {
        RequestResult requestResult = new RequestResult();
        try {
            //TODO: 判断是否跟zkcluster同机房，如果否，relay到相应机房的console
            List<JobDiffInfo> resultList = zkDBDiffService.diffByCluster(zkCluster);
            requestResult.setSuccess(true);
            requestResult.setObj(resultList);
        } catch (Exception e) {
            requestResult.setSuccess(false);
            requestResult.setMessage(e.toString());
        }

        return requestResult;
    }

    @RequestMapping(value = "zk_db_diff/diffByJob", method = RequestMethod.GET)
    @ResponseBody
    public RequestResult diffByJob(HttpServletRequest request, String jobName, String namespace) {
        RequestResult requestResult = new RequestResult();
        try {
            JobDiffInfo jobDiffInfo = zkDBDiffService.diffByJob(namespace, jobName);
            requestResult.setSuccess(true);
            requestResult.setObj(jobDiffInfo);
        } catch (Exception e) {
            requestResult.setSuccess(false);
            requestResult.setMessage(e.toString());
        }

        return requestResult;
    }

}
