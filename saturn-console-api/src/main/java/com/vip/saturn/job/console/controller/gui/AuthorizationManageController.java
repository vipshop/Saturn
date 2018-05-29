package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditParam;
import com.vip.saturn.job.console.controller.SuccessResponseEntity;
import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.Role;
import com.vip.saturn.job.console.mybatis.entity.UserRole;
import com.vip.saturn.job.console.service.AuthorizationManageService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author hebelala
 */
@RequestMapping("/console/authorizationManage")
public class AuthorizationManageController extends AbstractGUIController {

	private static final Logger log = LoggerFactory.getLogger(AuthorizationManageController.class);

	@Resource
	private AuthorizationManageService authorizationManageService;

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping("/addUserRoles")
	public SuccessResponseEntity addUserRoles(@AuditParam("userName") @RequestParam String userName,
			@AuditParam("roleKey") @RequestParam String roleKey,
			@AuditParam("namespace") @RequestParam String namespace,
			@AuditParam("needApproval") @RequestParam Boolean needApproval) throws SaturnJobConsoleException {
		assertIsSystemAdmin();
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
		authorizationManageService.addUserRole(userRole);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@Audit
	@PostMapping("/deleteUserRole")
	public SuccessResponseEntity deleteUserRole(@AuditParam("userName") @RequestParam String userName,
			@AuditParam("roleKey") @RequestParam String roleKey,
			@AuditParam("namespace") @RequestParam String namespace) throws SaturnJobConsoleException {
		assertIsSystemAdmin();
		UserRole userRole = new UserRole();
		userRole.setUserName(userName);
		userRole.setRoleKey(roleKey);
		userRole.setNamespace(namespace);
		String currentLoginUserName = getCurrentLoginUserName();
		userRole.setLastUpdatedBy(currentLoginUserName);
		authorizationManageService.deleteUserRole(userRole);
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
			@AuditParam("needApproval") @RequestParam Boolean needApproval) throws SaturnJobConsoleException {
		assertIsSystemAdmin();
		String currentLoginUserName = getCurrentLoginUserName();
		Date now = new Date();
		UserRole pre = new UserRole();
		pre.setUserName(preUserName);
		pre.setRoleKey(preRoleKey);
		pre.setNamespace(preNamespace);
		pre.setLastUpdateTime(now);
		pre.setLastUpdatedBy(currentLoginUserName);
		UserRole cur = new UserRole();
		cur.setUserName(userName);
		cur.setRoleKey(roleKey);
		cur.setNamespace(namespace);
		cur.setNeedApproval(needApproval);
		cur.setCreatedBy(currentLoginUserName);
		cur.setCreateTime(now);
		cur.setLastUpdatedBy(currentLoginUserName);
		cur.setLastUpdateTime(now);
		cur.setIsDeleted(false);
		authorizationManageService.updateUserRole(pre, cur);
		return new SuccessResponseEntity();
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping("/getRoles")
	public SuccessResponseEntity getRoles() throws SaturnJobConsoleException {
		assertIsSystemAdmin();
		List<Role> roles = authorizationManageService.getRoles();
		return new SuccessResponseEntity(roles);
	}

	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success/Fail", response = RequestResult.class)})
	@GetMapping("/getUserRoles")
	public SuccessResponseEntity getUsersBy(@RequestParam(required = false) String userName,
			@RequestParam(required = false) String roleKey, @RequestParam(required = false) String namespace)
			throws SaturnJobConsoleException {
		assertIsSystemAdmin();
		List<UserRole> userRoles = authorizationManageService.getUserRoles(userName, roleKey, namespace);
		return new SuccessResponseEntity(userRoles);
	}

}
