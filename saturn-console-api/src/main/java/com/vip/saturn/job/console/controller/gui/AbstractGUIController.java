package com.vip.saturn.job.console.controller.gui;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.controller.AbstractController;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleGUIException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.utils.SessionAttributeKeys;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class AbstractGUIController extends AbstractController {

	private static final Logger log = LoggerFactory.getLogger(AbstractGUIController.class);

	private static final String UNKNOWN = "Unkown";

	@ExceptionHandler(Throwable.class)
	public ResponseEntity<RequestResult> handleException(Throwable ex) {
		log.error("exception happens inside GUI controller operation:" + ex.getMessage(), ex);
		String message = ex.getMessage();
		if (StringUtils.isBlank(message)) {
			message = ex.toString();
		}
		return new ResponseEntity<>(new RequestResult(false, message), HttpStatus.OK);
	}

	public String getUserNameInSession() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.getRequest();
		if (request == null) {
			return UNKNOWN;
		}

		String loginUser = (String) request.getSession().getAttribute(SessionAttributeKeys.LOGIN_USER_NAME);
		if (StringUtils.isBlank(loginUser)) {
			return UNKNOWN;
		}

		return loginUser;
	}

}
