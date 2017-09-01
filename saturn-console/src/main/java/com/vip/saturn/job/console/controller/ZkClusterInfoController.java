package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.mybatis.entity.ZkClusterInfo;
import com.vip.saturn.job.console.mybatis.service.ZkClusterInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author hebelala
 */
@RestController
@RequestMapping("registry_center")
public class ZkClusterInfoController extends AbstractController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZkClusterInfoController.class);

	@Resource
	private ZkClusterInfoService zkClusterInfoService;

	@RequestMapping(value = "getAllZkClusterInfo", method = RequestMethod.GET)
	public RequestResult getAllZkClusterInfo(HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			List<ZkClusterInfo> allZkClusterInfo = zkClusterInfoService.getAllZkClusterInfo();
			requestResult.setObj(allZkClusterInfo);
			requestResult.setSuccess(true);
		} catch (Throwable t) {
			LOGGER.error(t.getMessage(), t);
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

}
