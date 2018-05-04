package com.vip.saturn.job.console.service.impl;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.mybatis.entity.*;
import com.vip.saturn.job.console.mybatis.repository.*;
import com.vip.saturn.job.console.service.AuthorizationService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.console.service.helper.SystemConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author hebelala
 */
public class AuthorizationServiceImpl implements AuthorizationService {

	private static final Logger log = LoggerFactory.getLogger(AuthorizationServiceImpl.class);

	@Autowired
	protected PermissionRepository permissionRepository;

	@Autowired
	protected RoleRepository roleRepository;

	@Autowired
	protected UserRepository userRepository;

	@Autowired
	protected RolePermissionRepository rolePermissionRepository;

	@Autowired
	protected UserRoleRepository userRoleRepository;

	@Autowired
	protected SystemConfigService systemConfigService;

	@Value("${authorization.enabled.default}")
	private boolean authorizationEnabledDefault;

	protected String systemAdminRoleKey = "system_admin";

	protected ConcurrentMap<String, Role> rolesCache = new ConcurrentHashMap<>();
	private Timer refreshAuthToCacheTimer = null;

	@PostConstruct
	public void init() {
		refreshAuthToCacheTimer = new Timer("refresh-auth-to-cache-timer", true);
		refreshAuthToCacheTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					refreshAuthToCache();
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}, 1000L, 1000L * 60 * 15);
	}

	@PreDestroy
	public void destroy() {
		if (refreshAuthToCacheTimer != null) {
			refreshAuthToCacheTimer.cancel();
		}
	}

	@Transactional(readOnly = true)
	private synchronized void refreshAuthToCache() throws InterruptedException {
		try {
			if (isAuthorizationEnabled()) {
				ConcurrentMap<String, Role> rolesMap = new ConcurrentHashMap<>();
				List<Role> roles = roleRepository.selectAll();
				if (roles != null) {
					for (Role role : roles) {
						String roleKey = role.getRoleKey();
						if (roleKey == null) {
							continue;
						}
						List<RolePermission> rolePermissions = rolePermissionRepository.selectByRoleKey(roleKey);
						if (rolePermissions != null) {
							role.setRolePermissions(rolePermissions);
							for (RolePermission rolePermission : rolePermissions) {
								Permission permission = permissionRepository
										.selectByKey(rolePermission.getPermissionKey());
								rolePermission.setPermission(permission);
							}
						}
						rolesMap.put(roleKey, role);
					}
				}
				rolesCache = rolesMap;
			}
		} catch (Throwable t) {
			if (t instanceof InterruptedException) {
				throw (InterruptedException) t;
			}
			log.error(t.getMessage(), t);
		}
	}

	@Override
	public void refreshCache() throws SaturnJobConsoleException {
		try {
			refreshAuthToCache();
		} catch (InterruptedException e) {
			throw new SaturnJobConsoleException(e);
		}
	}

	@Override
	public boolean isAuthorizationEnabled() throws SaturnJobConsoleException {
		return systemConfigService
				.getBooleanValue(SystemConfigProperties.AUTHORIZATION_ENABLED, authorizationEnabledDefault);
	}

	@Transactional(readOnly = true)
	@Override
	public User getUser(String userName) throws SaturnJobConsoleException {
		User user = null;
		if (!isAuthorizationEnabled()) {
			return getAvailableUser(user, userName);
		}
		user = userRepository.select(userName);
		user = getAvailableUser(user, userName);
		List<UserRole> userRoles = userRoleRepository.selectByUserName(userName);
		if (userRoles != null) {
			user.setUserRoles(userRoles);
			for (UserRole userRole : userRoles) {
				String roleKey = userRole.getRoleKey();
				Role role = rolesCache.get(roleKey);
				userRole.setRole(role);
			}
		}
		return user;
	}

	protected User getAvailableUser(User user, String userName) {
		if (user == null) {
			user = new User();
			user.setUserName(userName);
		}
		if (user.getUserRoles() == null) {
			user.setUserRoles(new ArrayList<UserRole>());
		}
		return user;
	}

	@Transactional(readOnly = true)
	@Override
	public boolean hasUserRole(UserRole userRole) throws SaturnJobConsoleException {
		if (!isAuthorizationEnabled()) {
			return true;
		}
		UserRole result = userRoleRepository.select(userRole);
		return result != null;
	}

	@Override
	public void assertIsPermitted(String permissionKey, String userName, String namespace)
			throws SaturnJobConsoleException {
		if (!isAuthorizationEnabled()) {
			return;
		}
		User user = getUser(userName);
		List<UserRole> userRoles = user.getUserRoles();
		if (userRoles != null) {
			for (UserRole userRole : userRoles) {
				Role role = userRole.getRole();
				if (role == null) {
					continue;
				}
				if (namespace.equals(userRole.getNamespace()) || systemAdminRoleKey.equals(userRole.getRoleKey())) {
					List<RolePermission> rolePermissions = role.getRolePermissions();
					if (rolePermissions != null) {
						for (RolePermission rolePermission : rolePermissions) {
							if (permissionKey.equals(rolePermission.getPermissionKey())) {
								return;
							}
						}
					}
				}
			}
		}
		throw new SaturnJobConsoleException(String.format("您没有权限，域:%s，权限:%s", namespace, permissionKey));
	}

	@Override
	public void assertIsSystemAdmin(String userName) throws SaturnJobConsoleException {
		if (!isAuthorizationEnabled()) {
			return;
		}
		User user = getUser(userName);
		List<UserRole> userRoles = user.getUserRoles();
		if (userRoles != null) {
			for (UserRole userRole : userRoles) {
				if (systemAdminRoleKey.equals(userRole.getRoleKey())) {
					return;
				}
			}
		}
		throw new SaturnJobConsoleException("您不是系统管理员，没有权限");
	}
}
