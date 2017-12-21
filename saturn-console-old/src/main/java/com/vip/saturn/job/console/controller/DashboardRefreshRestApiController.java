package com.vip.saturn.job.console.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.DashboardService;

/**
 * @author timmy.hu
 */
@Controller
@RequestMapping("/rest/v1")
public class DashboardRefreshRestApiController extends AbstractController {

	@Autowired
	private DashboardService dashboardService;

	/**
	 * 根据ZK集群key，刷新该集群的dashboard信息
	 * 
	 * @param zkClusterKey
	 * @param request
	 * @return
	 * @throws SaturnJobConsoleException
	 */
	@RequestMapping(value = "/dashboard/refresh", method = { RequestMethod.POST,
			RequestMethod.GET }, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> dashboardRefresh(String zkClusterKey, HttpServletRequest request)
			throws SaturnJobConsoleException {
		try {
			checkMissingParameter("zkClusterKey", zkClusterKey);
			long beforeRefresh = System.currentTimeMillis();
			dashboardService.refreshStatistics2DB(zkClusterKey);
			long afterRefresh = System.currentTimeMillis();
			long takeTime = afterRefresh - beforeRefresh;
			return new ResponseEntity<Object>(takeTime, HttpStatus.OK);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

}
