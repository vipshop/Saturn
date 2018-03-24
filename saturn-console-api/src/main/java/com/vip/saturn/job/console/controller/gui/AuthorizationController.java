package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.User;
import com.vip.saturn.job.console.mybatis.entity.UserRole;
import com.vip.saturn.job.console.mybatis.service.AuthorizationService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

/**
 * @author xiaopeng.he
 */
@RequestMapping("/console/authorization")
public class AuthorizationController extends AbstractGUIController {

	@Resource
	private AuthorizationService authorizationService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping("/useAuthorization")
	public SuccessResponseEntity isAuthorizationEnabled() {
		return new SuccessResponseEntity(authorizationService.isAuthorizationEnabled());
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping("/loginUser")
	public SuccessResponseEntity getLoginUser(HttpSession httpSession) throws SaturnJobConsoleException {
		String currentLoginUserName = getCurrentLoginUserName();
		User user = authorizationService.getUser(currentLoginUserName);
		if (user == null) {
			user = new User();
			user.setName(currentLoginUserName);
		}
		return new SuccessResponseEntity(user);
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping("/addUserRoles")
	public SuccessResponseEntity addUserRoles(@AuditParam("userName") @RequestParam String userName,
			@AuditParam("roleKey") @RequestParam String roleKey,
			@AuditParam("namespace") @RequestParam String namespace,
			@AuditParam("needApproval") @RequestParam Boolean needApproval, HttpSession httpSession)
			throws SaturnJobConsoleException {
		assertIsSuper();
		String currentLoginUserName = getCurrentLoginUserName();
		Date now = new Date();
		UserRole userRole = new UserRole();
		userRole.setUserName(userName);
		userRole.setRoleKey(roleKey);
		userRole.setNamespace(namespace);
		userRole.setNeedApproval(needApproval);
		userRole.setIsDeleted(false);
		userRole.setCreatedBy(currentLoginUserName);
		userRole.setCreateTime(now);
		userRole.setLastUpdatedBy(currentLoginUserName);
		userRole.setLastUpdateTime(now);
		User user = new User();
		user.setName(userName);
		user.setPassword("");
		user.setRealName("");
		user.setEmployeeId("");
		user.setEmail("");
		user.setCreatedBy(currentLoginUserName);
		user.setCreateTime(now);
		user.setLastUpdatedBy(currentLoginUserName);
		user.setLastUpdateTime(now);
		user.setIsDeleted(false);
		userRole.setUser(user);
		authorizationService.addUserRole(userRole);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping("/deleteUserRole")
	public SuccessResponseEntity deleteUserRole(@AuditParam("userName") @RequestParam String userName,
			@AuditParam("roleKey") @RequestParam String roleKey,
			@AuditParam("namespace") @RequestParam String namespace, HttpSession httpSession)
			throws SaturnJobConsoleException {
		assertIsSuper();
		UserRole userRole = new UserRole();
		userRole.setUserName(userName);
		userRole.setRoleKey(roleKey);
		userRole.setNamespace(namespace);
		String currentLoginUserName = getCurrentLoginUserName();
		userRole.setLastUpdatedBy(currentLoginUserName);
		authorizationService.deleteUserRole(userRole);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping("/updateUserRole")
	public SuccessResponseEntity updateUserRole(@AuditParam("preUserName") @RequestParam String preUserName,
			@AuditParam("preRoleKey") @RequestParam String preRoleKey,
			@AuditParam("preNamespace") @RequestParam String preNamespace,
			@AuditParam("userName") @RequestParam String userName, @AuditParam("roleKey") @RequestParam String roleKey,
			@AuditParam("namespace") @RequestParam String namespace,
			@AuditParam("needApproval") @RequestParam Boolean needApproval, HttpSession httpSession)
			throws SaturnJobConsoleException {
		assertIsSuper();
		UserRole pre = new UserRole();
		pre.setUserName(preUserName);
		pre.setRoleKey(preRoleKey);
		pre.setNamespace(preNamespace);
		UserRole cur = new UserRole();
		cur.setUserName(userName);
		cur.setRoleKey(roleKey);
		cur.setNamespace(namespace);
		cur.setNeedApproval(needApproval);
		String currentLoginUserName = getCurrentLoginUserName();
		Date now = new Date();
		cur.setCreatedBy(currentLoginUserName);
		cur.setCreateTime(now);
		cur.setLastUpdatedBy(currentLoginUserName);
		cur.setLastUpdateTime(now);
		cur.setIsDeleted(false);
		authorizationService.updateUserRole(pre, cur);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping("/getAllUser")
	public SuccessResponseEntity getAllUsers() throws SaturnJobConsoleException {
		assertIsSuper();
		List<User> allUser = authorizationService.getAllUsers();
		return new SuccessResponseEntity(allUser);
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping("/getSupers")
	public SuccessResponseEntity getSupers() throws SaturnJobConsoleException {
		List<User> supers = authorizationService.getSupers();
		return new SuccessResponseEntity(supers);
	}


}
