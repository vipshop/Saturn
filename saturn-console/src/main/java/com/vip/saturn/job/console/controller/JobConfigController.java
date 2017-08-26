package com.vip.saturn.job.console.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.JobConfigInitializationService;

@Controller
@RequestMapping("/")
public class JobConfigController extends AbstractController {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobConfigController.class);

	@Resource
	private JobConfigInitializationService jobConfigInitializationService;

	private ExecutorService executorService = Executors.newSingleThreadExecutor();

	@RequestMapping(value = "export_job_config_page", method = RequestMethod.GET)
	public String exportPage(final ModelMap model, HttpServletRequest request) {
		boolean isExporting = jobConfigInitializationService.isExporting();
		model.put("isExporting", isExporting);
		return "jobconfig_export";
	}

	@RequestMapping(value = "jobconfig/getExportRegList", method = RequestMethod.GET)
	@ResponseBody
	public Map<?, ?> load() {
		Map<String, Object> model = new HashMap<String, Object>();
		List<RegistryCenterConfiguration> regCenterConfList = jobConfigInitializationService.getRegistryCenterConfigurations();
		model.put("configs", regCenterConfList);
		return model;
	}

	@RequestMapping(value = "jobconfig/getExportStatus", method = RequestMethod.GET)
	@ResponseBody
	public Map<?, ?> getExportStatus() {
		Map<String, Object> model = new HashMap<String, Object>();
		Map<String, String> exportStatus = jobConfigInitializationService.getStatus();
		model.put("exportStatus", exportStatus);
		return model;
	}

	@RequestMapping(value = "jobconfig/exportAllConfigToDb", method = RequestMethod.POST)
	@ResponseBody
	public RequestResult exportAllConfigToDb() {
		RequestResult result = new RequestResult();
		if (jobConfigInitializationService.isExporting()) {
			result.setSuccess(false);
			result.setMessage("正在导出配置中，如有必要，请稍后再试！");
			return result;
		}
		executorService.submit(new Runnable() {
			@Override
			public void run() {
				try {
					jobConfigInitializationService.exportAllToDb(null);
				} catch (SaturnJobConsoleException e) {
					LOGGER.error("在全量导出配置的时候失败", e);
				}
			}
		});
		result.setSuccess(true);
		return result;
	}
}
