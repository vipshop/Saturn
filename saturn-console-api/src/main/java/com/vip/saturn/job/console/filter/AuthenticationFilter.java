/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

package com.vip.saturn.job.console.filter;

import com.alibaba.fastjson.JSON;
import com.vip.saturn.job.console.domain.RequestResultHelper;
import com.vip.saturn.job.console.utils.SessionAttributeKeys;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class AuthenticationFilter implements Filter {

	private boolean isEnabled;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (!isEnabled) {
			chain.doFilter(request, response);
			return;
		}

		HttpServletRequest req = (HttpServletRequest) request;
		if (req.getRequestURL().toString().endsWith("/console/authentication/login")) {
			chain.doFilter(request, response);
			return;
		}

		HttpServletResponse resp = (HttpServletResponse) response;

		if (req.getSession().getAttribute(SessionAttributeKeys.LOGIN_USER_NAME) == null) {
			response.setContentType("application/json;charset=UTF-8");
			PrintWriter writer = resp.getWriter();
			writer.print(JSON.toJSONString(RequestResultHelper.redirect("/#/login")));
			writer.flush();
			return;
		}

		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}

	public void setEnabled(boolean enabled) {
		isEnabled = enabled;
	}
}
