/**
 * Copyright 2016 vip.com. <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. </p>
 */

package com.vip.saturn.job.console.interceptor;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.controller.AbstractController;
import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.SessionAttributeKeys;
import com.vip.saturn.job.console.utils.ThreadLocalCuratorClient;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public final class CuratorSessionClientInterceptor extends HandlerInterceptorAdapter {

	@Resource
	private RegistryCenterService registryCenterService;

	@Override
	public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
			throws Exception {
		String nns = request.getParameter(AbstractController.REQUEST_NAMESPACE_PARAM);
		if (!Strings.isNullOrEmpty(nns)) {
			RegistryCenterClient client = registryCenterService.connect(nns);
			ThreadLocalCuratorClient.setCuratorClient(client.getCuratorClient());
			RegistryCenterConfiguration conf = registryCenterService.findConfig(client.getNameAndNamespace());
			if (conf == null) {
				response.sendRedirect(request.getContextPath() + "/registry_center_page");
				return false;
			}
			request.getSession().setAttribute(SessionAttributeKeys.ACTIVATED_CONFIG_SESSION_KEY, conf);
			request.getSession().setAttribute(SessionAttributeKeys.CURRENT_ZK_CLUSTER_KEY, conf.getZkClusterKey());
			return true;
		}
		RegistryCenterConfiguration reg = (RegistryCenterConfiguration) request.getSession()
				.getAttribute(SessionAttributeKeys.ACTIVATED_CONFIG_SESSION_KEY);
		if (reg == null) {
			response.sendRedirect(request.getContextPath() + "/registry_center_page");
			return false;
		}
		RegistryCenterClient client = registryCenterService.getCuratorByNameAndNamespace(reg.getNameAndNamespace());
		if (null == client || !client.isConnected()) {
			response.sendRedirect(request.getContextPath() + "/registry_center_page");
			return false;
		}
		ThreadLocalCuratorClient.setCuratorClient(client.getCuratorClient());
		return true;
	}

	@Override
	public void postHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler,
			final ModelAndView modelAndView) throws Exception {
		ThreadLocalCuratorClient.clear();
	}
}
