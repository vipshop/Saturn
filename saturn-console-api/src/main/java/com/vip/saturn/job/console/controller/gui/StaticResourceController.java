package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.utils.SaturnConsoleUtils;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Static resource controller.
 *
 * @author kfchu
 */
@RequestMapping("/console/static")
public class StaticResourceController extends AbstractGUIController {

	private static final String EXPORT_FILE_NAME = "jobs-template.xls";

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/jobTemplate/download")
	public void exportJobs(final HttpServletRequest request, final HttpServletResponse response)
			throws SaturnJobConsoleException {
		InputStream is = getClass().getResourceAsStream("/download/" + EXPORT_FILE_NAME);
		if (is == null) {
			throw new SaturnJobConsoleException("The jobs-template is not existing");
		}
		SaturnConsoleUtils.exportFile(response, is, EXPORT_FILE_NAME);
	}

}
