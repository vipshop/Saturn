package com.vip.saturn.job.console.controller.gui;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.vip.saturn.job.console.controller.AbstractController;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.Permission;
import com.vip.saturn.job.console.mybatis.service.AuthorizationService;
import com.vip.saturn.job.console.utils.SessionAttributeKeys;

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

	public String getUserOaNameInSession() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.getRequest();
		if (request == null) {
			return UNKNOWN;
		}

		String loginUser = (String) request.getSession().getAttribute(SessionAttributeKeys.LOGIN_USER_OA_NAME);
		if (StringUtils.isBlank(loginUser)) {
			return UNKNOWN;
		}

		return loginUser;
	}

	public void assertIsPermitted(Permission permission, String namespace) throws SaturnJobConsoleException {
		String userOaName = getUserOaNameInSession();
		if (!authorizationService.isPermitted(permission, userOaName, namespace)) {
			throw new SaturnJobConsoleException(String.format("您没有权限，域:%s，权限:%s", namespace, permission.getKey()));
		}
	}

	public void assertIsPermitted(Permission permission) throws SaturnJobConsoleException {
		String userOaName = getUserOaNameInSession();
		if (!authorizationService.isPermitted(permission, userOaName, "")) {
			throw new SaturnJobConsoleException(String.format("您没有权限，权限:%s", permission.getKey()));
		}
	}

	public void assertIsSuper() throws SaturnJobConsoleException {
		String userOaName = getUserOaNameInSession();
		if (!authorizationService.isSuperRole(userOaName)) {
			throw new SaturnJobConsoleException(String.format("您不是管理员，没有权限"));
		}
	}

}
