package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleGUIException;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Static resource controller.
 *
 * @author kfchu
 */
@Controller
@RequestMapping("/console/static")
public class StaticResourceController extends AbstractGUIController {

	private static final Logger log = LoggerFactory.getLogger(StaticResourceController.class);

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping(value = "/jobTemplate/download")
	public void exportJobs(final HttpServletRequest request, final HttpServletResponse response)
			throws SaturnJobConsoleException {
		try {
			InputStream is = getClass().getResourceAsStream("/download/jobs-template.xls");
			if (is == null) {
				throw new SaturnJobConsoleGUIException("The jobs-template is not existing");
			}

			response.setContentType("application/octet-stream");
			response.setHeader("Content-disposition",
					"attachment; filename=" + new String("jobs-template.xls".getBytes("UTF-8"), "ISO8859-1"));

			BufferedInputStream bis = null;
			BufferedOutputStream bos = null;
			try {
				bis = new BufferedInputStream(is);
				bos = new BufferedOutputStream(response.getOutputStream());
				byte[] buff = new byte[2048];
				int bytesRead;
				while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
					bos.write(buff, 0, bytesRead);
				}
			} finally {
				if (bis != null) {
					try {
						bis.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
				if (bos != null) {
					try {
						bos.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleGUIException(e);
		}
	}

}
