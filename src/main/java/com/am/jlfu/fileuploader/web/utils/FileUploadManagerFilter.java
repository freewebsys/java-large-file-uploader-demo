package com.am.jlfu.fileuploader.web.utils;


import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;



@Component("jlfuFilter")
public class FileUploadManagerFilter
		implements Filter {

	@Autowired
	RequestComponentContainer requestComponentContainer;



	@Override
	public void destroy() {

	}


	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		requestComponentContainer.populate((HttpServletRequest) req,(HttpServletResponse) resp);
		chain.doFilter(req, resp);
		requestComponentContainer.clear();
	}


	@Override
	public void init(FilterConfig arg0)
			throws ServletException {

	}


}
