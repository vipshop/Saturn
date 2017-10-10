package com.vip.saturn.job.console.controller;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vip.saturn.job.console.domain.ExportJobConfigPageStatus;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.service.JobConfigInitializationService;
import com.vip.saturn.job.console.utils.SessionAttributeKeys;

@Controller
@RequestMapping("/")
public class JobConfigController extends AbstractController {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobConfigController.class);

	@Resource
	private JobConfigInitializationService jobConfigInitializationService;

	@RequestMapping(value = "export_job_config_page", method = RequestMethod.GET)
	public String exportPage(final ModelMap model, HttpServletRequest request) {
		ExportJobConfigPageStatus exportJobConfigPageStatus = (ExportJobConfigPageStatus) request.getSession()
				.getAttribute(SessionAttributeKeys.EXPORT_JOB_CONFIG_PAGE_STATUS);
		if (exportJobConfigPageStatus != null && exportJobConfigPageStatus.isExported() == false) {
			model.put("exporting", true);
		} else {
			model.put("exporting", false);
		}
		return "jobconfig_export";
	}

	@RequestMapping(value = "jobconfig/getExportRegList", method = RequestMethod.GET)
	@ResponseBody
	public RequestResult load(HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		try {
			List<RegistryCenterConfiguration> regCenterConfList = null;
			ExportJobConfigPageStatus exportJobConfigPageStatus = (ExportJobConfigPageStatus) request.getSession()
					.getAttribute(SessionAttributeKeys.EXPORT_JOB_CONFIG_PAGE_STATUS);
			if (exportJobConfigPageStatus == null || exportJobConfigPageStatus.isExported()) {
				regCenterConfList = jobConfigInitializationService.getRegistryCenterConfigurations();
			} else {
				regCenterConfList = exportJobConfigPageStatus.getRegCenterConfList();
			}
			requestResult.setSuccess(true);
			requestResult.setObj(regCenterConfList);
		} catch (Throwable t) {
			LOGGER.error(t.getMessage(), t);
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "jobconfig/getExportStatus", method = RequestMethod.GET)
	@ResponseBody
	public RequestResult getExportStatus(HttpServletRequest request) {
		RequestResult requestResult = new RequestResult();
		LOGGER.info("getExportStatus");
		try {
			ExportJobConfigPageStatus exportJobConfigPageStatus = (ExportJobConfigPageStatus) request.getSession()
					.getAttribute(SessionAttributeKeys.EXPORT_JOB_CONFIG_PAGE_STATUS);
			requestResult.setSuccess(true);
			requestResult.setObj(exportJobConfigPageStatus);
		} catch (Throwable t) {
			LOGGER.error(t.getMessage(), t);
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "jobconfig/exportAllConfigToDb", method = RequestMethod.POST)
	@ResponseBody
	public RequestResult exportAllConfigToDb(HttpServletRequest request) {
		LOGGER.info("exportAllConfigToDb");
		RequestResult requestResult = new RequestResult();
		try {
			ExportJobConfigPageStatus exportJobConfigPageStatus = (ExportJobConfigPageStatus) request.getSession()
					.getAttribute(SessionAttributeKeys.EXPORT_JOB_CONFIG_PAGE_STATUS);
			if (exportJobConfigPageStatus != null && exportJobConfigPageStatus.isExported() == false) {
				requestResult.setSuccess(false);
				requestResult.setMessage("正在导出配置中，如有必要，请稍后再试！");
				return requestResult;
			}
			exportJobConfigPageStatus = jobConfigInitializationService.exportAllToDb(null);
			request.getSession().setAttribute(SessionAttributeKeys.EXPORT_JOB_CONFIG_PAGE_STATUS,
					exportJobConfigPageStatus);
			requestResult.setSuccess(true);
		} catch (Throwable t) {
			LOGGER.error(t.getMessage(), t);
			requestResult.setSuccess(false);
			requestResult.setMessage(t.toString());
		}
		return requestResult;
	}
}
