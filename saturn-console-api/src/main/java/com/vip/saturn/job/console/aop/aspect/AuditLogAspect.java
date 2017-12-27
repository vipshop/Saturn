package com.vip.saturn.job.console.aop.aspect;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.utils.AuditInfoContext;
import com.vip.saturn.job.console.utils.SessionAttributeKeys;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Aspect to handle the audit log logic.
 *
 * @author kfchu
 */
@Aspect
@Component
public class AuditLogAspect {

	private static final Logger log = LoggerFactory.getLogger("AUDITLOG");

	private static final String UNKNOWN = "Unkown";

	private static final String GUI_AUDIT_LOG_TEMPLATE = "GUI API:[%s] is called by User:[%s] with IP:[%s], result is %s.";

	private static final String REST_AUDIT_LOG_TEMPLATE = "REST API:[%s] is called by IP:[%s], result is %s.";

	@Around("@annotation(audit)")
	public Object logAuditInfo(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
		Boolean isSuccess = false;
		try {
			Object result = joinPoint.proceed();
			isSuccess = true;
			return result;
		} finally {
			logAudit(isSuccess, audit.type());
			AuditInfoContext.reset();
		}
	}

	private void logAudit(Boolean isSuccess, AuditType auditType) {
		String content = null;
		switch (auditType) {
			case REST:
				content = getRESTRequstContent(isSuccess);
				break;
			case WEB:
			default:
				content = getWebRequestContent(isSuccess);
		}
		log.info(content);
	}

	protected String getWebRequestContent(Boolean isSuccess) {
		return buildLogContent(
				String.format(GUI_AUDIT_LOG_TEMPLATE, getUri(), getUserName(), getIpAddress(),
						getResultValue(isSuccess)));
	}

	protected String getRESTRequstContent(Boolean isSuccess) {
		return buildLogContent(
				String.format(REST_AUDIT_LOG_TEMPLATE, getUri(), getIpAddress(), getResultValue(isSuccess)));
	}

	private String buildLogContent(String initValue) {
		StringBuilder stringBuilder = new StringBuilder(initValue);
		// append additional context info if possible
		Map<String, String> auditInfoMap = AuditInfoContext.currentAuditInfo();
		if (auditInfoMap != null && auditInfoMap.size() > 0) {
			stringBuilder.append("context info:" + auditInfoMap).append(".");
		}

		return stringBuilder.toString();
	}

	private String getUri() {
		HttpServletRequest request = getRequestFromContext();
		if (request == null) {
			return StringUtils.EMPTY;
		}

		return request.getRequestURI();
	}

	private String getIpAddress() {
		HttpServletRequest request = getRequestFromContext();
		if (request == null) {
			return StringUtils.EMPTY;
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

	private String getResultValue(boolean isSuccess) {
		return isSuccess ? "success" : "failed";
	}

	private HttpServletRequest getRequestFromContext() {
		return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.getRequest();
	}
}
