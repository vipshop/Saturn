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
