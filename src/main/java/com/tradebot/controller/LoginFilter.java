package com.tradebot.controller;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		   throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		
		String requestURI = httpRequest.getRequestURI();

		if (requestURI.endsWith("/login.xhtml")) {
			chain.doFilter(request, response);
			return;
		}

		if (httpRequest.getUserPrincipal() == null) {
			httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.xhtml");
		} else {
			chain.doFilter(request, response);
		}
	}
}
