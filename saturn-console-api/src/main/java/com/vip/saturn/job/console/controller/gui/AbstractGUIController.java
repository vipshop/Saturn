package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.controller.AbstractController;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.RequestResultHelper;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.AuthorizationService;
import com.vip.saturn.job.console.utils.SessionAttributeKeys;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.vip.saturn.job.console.exception.SaturnJobConsoleException.*;

public class AbstractGUIController extends AbstractController {

	private static final Logger log = LoggerFactory.getLogger(AbstractGUIController.class);

	private static final String UNKNOWN = "Unknown";

	private static final String AUTHENTICATION_FAIL_PREFIX = "认证失败：";

	@Resource
	private AuthorizationService authorizationService;

	@ExceptionHandler
	public ResponseEntity<RequestResult> handleSaturnJobConsoleException(SaturnJobConsoleException e) {
		String message = e.getMessage();
		if (StringUtils.isBlank(message)) {
			message = e.toString();
		}

		switch (e.getErrorCode()) {
			case ERROR_CODE_BAD_REQUEST:
				log.warn("bad request while calling GUI API:{}", message);
				return new ResponseEntity<>(RequestResultHelper.failure(message), HttpStatus.OK);
			case ERROR_CODE_NOT_EXISTED:
				log.warn("resource not existed while calling GUI API:{}", message);
				return new ResponseEntity<>(RequestResultHelper.failure(message), HttpStatus.OK);
			case ERROR_CODE_AUTHN_FAIL:
				log.warn("authentication fail while calling GUI API:{}", message);
				return new ResponseEntity<>(RequestResultHelper.failure(AUTHENTICATION_FAIL_PREFIX + message),
						HttpStatus.OK);
			case ERROR_CODE_INTERNAL_ERROR:
			default:
				log.error("internal server error happens while calling GUI API:{}", message);
				return new ResponseEntity<>(RequestResultHelper.failure(message), HttpStatus.OK);
		}
	}

	@ExceptionHandler(Throwable.class)
	public ResponseEntity<RequestResult> handleException(Throwable ex) {
		log.error("exception happens inside GUI controller operation:" + ex.getMessage(), ex);
		String message = ex.getMessage();
		if (StringUtils.isBlank(message)) {
			message = ex.toString();
		}
		return new ResponseEntity<>(RequestResultHelper.failure(message), HttpStatus.OK);
	}

	private Object getAttributeInSession(String key) {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.getRequest();
		if (request == null) {
			return null;
		}
		return request.getSession().getAttribute(key);
	}

	private String getCurrentUser(String key) {
		String user = (String) getAttributeInSession(key);
		return StringUtils.isBlank(user) ? UNKNOWN : user;
	}

	public String getCurrentLoginUserRealName() {
		return getCurrentUser(SessionAttributeKeys.LOGIN_USER_REAL_NAME);
	}

	public String getCurrentLoginUserName() {
		return getCurrentUser(SessionAttributeKeys.LOGIN_USER_NAME);
	}

	public void assertIsPermitted(String permissionKey, String namespace) throws SaturnJobConsoleException {
		String userName = getCurrentLoginUserName();
		authorizationService.assertIsPermitted(permissionKey, userName, namespace);
	}

	public void assertIsPermitted(String permissionKey) throws SaturnJobConsoleException {
		String userName = getCurrentLoginUserName();
		authorizationService.assertIsPermitted(permissionKey, userName, "");
	}

	public void assertIsSystemAdmin() throws SaturnJobConsoleException {
		String userName = getCurrentLoginUserName();
		authorizationService.assertIsSystemAdmin(userName);
	}

	public void printErrorToJsAlert(String errorMsg, HttpServletResponse response) throws IOException {
		response.setContentType("text/html; charset=utf-8");
		StringBuilder msg = new StringBuilder().append("<script language='javascript'>").append("alert(\"")
				.append(errorMsg.replaceAll("\"", "\\\"")).append("\");").append("history.back();").append("</script>");
		response.getOutputStream().print(new String(msg.toString().getBytes("UTF-8"), "ISO8859-1"));
	}

}
