package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.User;
import com.vip.saturn.job.console.service.AuthorizationService;
import com.vip.saturn.job.console.utils.AuditInfoContext;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * @author hebelala
 */
@RequestMapping("/console/authorization")
public class AuthorizationController extends AbstractGUIController {

	@Resource
	private AuthorizationService authorizationService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping("/isAuthorizationEnabled")
	public SuccessResponseEntity isAuthorizationEnabled() throws SaturnJobConsoleException {
		return new SuccessResponseEntity(authorizationService.isAuthorizationEnabled());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping("/loginUser")
	public SuccessResponseEntity getLoginUser(HttpSession httpSession) throws SaturnJobConsoleException {
		String currentLoginUserName = getCurrentLoginUserName();
		User user = authorizationService.getUser(currentLoginUserName);
		return new SuccessResponseEntity(user);
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@RequestMapping(value = "/refreshCache", method = {RequestMethod.GET, RequestMethod.POST})
	public SuccessResponseEntity refreshCache(HttpSession httpSession) throws SaturnJobConsoleException {
		AuditInfoContext.put("loginUser", getCurrentLoginUserName());
		authorizationService.refreshAuthCache();
		return new SuccessResponseEntity();
	}

}
