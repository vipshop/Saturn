package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.SystemConfig;
import com.vip.saturn.job.console.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hebelala
 */
@Controller
@RequestMapping("/")
public class SystemConfigController extends AbstractController {

	private static Logger LOGGER = LoggerFactory.getLogger(ServerController.class);

	@Resource
	private SystemConfigService systemConfigService;

	@RequestMapping(value = "system_config", method = RequestMethod.GET)
	public String systemConfig(HttpServletRequest request, HttpSession session, ModelMap model) {
		try {
			model.put("propertiesSupported", systemConfigService.getPropertiesCached());
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return "system_config";
	}

	@RequestMapping(value = "system_config/queryValue", method = RequestMethod.POST)
	@ResponseBody
	public RequestResult queryValue(HttpServletRequest request, String property1) {
		RequestResult requestResult = new RequestResult();
		try {
			List<String> properties = new ArrayList<>();
			if (property1 != null) {
				String[] split = property1.split(",");
				if (split != null) {
					for (String s : split) {
						if (s != null && s.trim().length() > 0) {
							String tmp = s.trim();
							if (!properties.contains(tmp)) {
								properties.add(tmp);
							}
						}
					}
				}
			}
			List<SystemConfig> systemConfigs = systemConfigService.getSystemConfigsDirectly(properties);
			requestResult.setSuccess(true);
			requestResult.setObj(systemConfigs);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
			return requestResult;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			requestResult.setSuccess(false);
			requestResult.setMessage(e.toString());
		}
		return requestResult;
	}

	@RequestMapping(value = "system_config/insertOrUpdate", method = RequestMethod.POST)
	@ResponseBody
	public RequestResult insertOrUpdate(HttpServletRequest request, String property2, String value2) {
		RequestResult requestResult = new RequestResult();
		try {
			if (property2 == null) {
				throw new SaturnJobConsoleException("property cannot be null");
			}
			SystemConfig systemConfig = new SystemConfig();
			systemConfig.setProperty(property2);
			systemConfig.setValue(value2);
			systemConfigService.insertOrUpdate(systemConfig);
			requestResult.setSuccess(true);
		} catch (SaturnJobConsoleException e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.getMessage());
			return requestResult;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			requestResult.setSuccess(false);
			requestResult.setMessage(e.toString());
		}
		return requestResult;
	}

}
