package com.vip.saturn.job.console.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.vip.saturn.job.console.utils.SessionAttributeKeys;

/**
 * Record the last url visited.
 * 
 * 
 * @author linzhaoming
 *
 */
public class RecordLastVisit implements Filter {
	private static final String GET_METHOD = "get";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		String currentURL = request.getRequestURI();
		String method = request.getMethod();
		if (StringUtils.isNotEmpty(currentURL) && currentURL.indexOf(".js") < 0 && currentURL.indexOf(".css") < 0
				&& GET_METHOD.equalsIgnoreCase(method)) {
			if (currentURL.indexOf("job_detail") >= 0 || currentURL.indexOf("server_detail") >= 0
					|| (currentURL.indexOf("dashboard") >= 0 && currentURL.indexOf("refresh") < 0)
					|| currentURL.indexOf("overview") >= 0) {
				request.setCharacterEncoding("UTF-8");
				String queryString = request.getQueryString();
				if (StringUtils.isNotEmpty(queryString)) {
					currentURL += "?" + queryString;
				}

				request.getSession().setAttribute(SessionAttributeKeys.LAST_VISIT_URL, currentURL);
			}
		}
		if (chain != null) {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
	}
}
