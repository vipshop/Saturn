package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.controller.AbstractController;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.RequestResultHelper;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.Permission;
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

public class AbstractGUIController extends AbstractController {

	private static final Logger log = LoggerFactory.getLogger(AbstractGUIController.class);

	private static final String UNKNOWN = "Unkown";

	@Resource
	private AuthorizationService authorizationService;

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

	public void assertIsPermitted(Permission permission, String namespace) throws SaturnJobConsoleException {
		String userName = getCurrentLoginUserName();
		authorizationService.assertIsPermitted(permission, userName, namespace);
	}

	public void assertIsPermitted(Permission permission) throws SaturnJobConsoleException {
		String userName = getCurrentLoginUserName();
		authorizationService.assertIsPermitted(permission, userName, "");
	}

	public void assertIsSystemAdmin() throws SaturnJobConsoleException {
		String userName = getCurrentLoginUserName();
		authorizationService.assertIsSystemAdmin(userName);
	}

}
