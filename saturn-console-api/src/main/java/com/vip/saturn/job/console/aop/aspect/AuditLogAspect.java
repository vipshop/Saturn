package com.vip.saturn.job.console.aop.aspect;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.utils.AuditInfoContext;
import com.vip.saturn.job.console.utils.SessionAttributeKeys;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Aspect
@Component
public class AuditLogAspect {

	private static final Logger log = LoggerFactory.getLogger("AUDITLOG");

	private static final String UNKNOWN = "Unkown";

	private static final String EMPTY_STR = "";

	private static final String GUI_AUDIT_LOG_TEMPLATE = "GUI API:[%s] is called by User:[%s] with IP:[%s], result is %s.";

	private static final String REST_AUDIT_LOG_TEMPLATE = "REST API:[%s] is called by IP:[%s], result is %s.";

	@Around("@annotation(audit)")
	public Object logAuditInfo(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();

		Boolean isSuccess = false;
		try {
			Object result = joinPoint.proceed();
			isSuccess = true;
			return result;
		} finally {
			logAudit(isSuccess, audit.type(), signature);
			AuditInfoContext.reset();
		}
	}

	private void logAudit(Boolean isSuccess, AuditType auditType, Signature signature) {
		switch (auditType) {
			case REST:
				logRESTRequst(isSuccess, signature);
				break;
			case WEB:
			default:
				logWebRequest(isSuccess, signature);
		}
	}

	protected void logWebRequest(Boolean isSuccess, Signature signature) {
		String uri = getUri();
		String userName = getUserName();
		String ipAddr = getIpAddress();
		String result = isSuccess ? "success" : "failed";

		log.info(buildLogContent(String.format(GUI_AUDIT_LOG_TEMPLATE, uri, userName, ipAddr, result)));
	}

	protected void logRESTRequst(Boolean isSuccess, Signature signature) {
		String uri = getUri();
		String ipAddr = getIpAddress();
		String result = isSuccess ? "success" : "failed";

		log.info(buildLogContent(String.format(REST_AUDIT_LOG_TEMPLATE, uri, ipAddr, result)));
	}

	private String buildLogContent(String initValue) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(initValue);
		// append namespace if set in context

		// append additional context info if possible
		Map<String, String> auditInfoMap = AuditInfoContext.currentAuditInfo();
		if (auditInfoMap != null && auditInfoMap.size() > 0) {
			stringBuilder.append(auditInfoMap);
		}

		return stringBuilder.toString();
	}

	private String getUri() {
		HttpServletRequest request = getRequestFromContext();
		if (request == null) {
			return EMPTY_STR;
		}

		return request.getRequestURI();
	}

	private String getIpAddress() {
		HttpServletRequest request = getRequestFromContext();
		if (request == null) {
			return EMPTY_STR;
		}

		String ip = request.getHeader("X-FORWARDED-FOR");
		return StringUtils.isBlank(ip) ? request.getRemoteAddr() : ip;
	}

	private String getUserName() {
		HttpServletRequest request = getRequestFromContext();
		if (request == null) {
			return UNKNOWN;
		}

		String loginUser = (String) request.getSession().getAttribute(SessionAttributeKeys.LOGIN_USER_NAME);
		return StringUtils.isBlank(loginUser) ? UNKNOWN : loginUser;
	}

	private HttpServletRequest getRequestFromContext() {
		return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.getRequest();
	}
}
