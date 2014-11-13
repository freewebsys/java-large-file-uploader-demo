package com.am.jlfu.fileuploader.web.utils;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Component;



/**
 * {@link HttpServletResponse} and {@link HttpServletRequest} are stored in a thread local and are
 * populated by the filter.
 * 
 * @author antoinem
 * 
 */
@Component
public class RequestComponentContainer {


	private ThreadLocal<HttpServletResponse> responseThreadLocal = new ThreadLocal<HttpServletResponse>();
	private ThreadLocal<HttpServletRequest> requestThreadLocal = new ThreadLocal<HttpServletRequest>();



	public void populate(HttpServletRequest request, HttpServletResponse response) {
		responseThreadLocal.set(response);
		requestThreadLocal.set(request);
	}


	public void clear() {
		responseThreadLocal.remove();
	}


	public HttpServletResponse getResponse() {
		return responseThreadLocal.get();
	}


	public HttpServletRequest getRequest() {
		return requestThreadLocal.get();

	}


	public HttpSession getSession() {
		return requestThreadLocal.get().getSession();

	}


}
