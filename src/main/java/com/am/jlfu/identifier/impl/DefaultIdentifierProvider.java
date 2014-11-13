package com.am.jlfu.identifier.impl;


import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.am.jlfu.identifier.IdentifierProvider;
import com.am.jlfu.notifier.JLFUListenerPropagator;



/**
 * Identifier provider that provides identification based on cookie.
 * 
 * @author antoinem
 * 
 */
@Component
public class DefaultIdentifierProvider
		implements IdentifierProvider {


	public static final String cookieIdentifier = "jlufStaticStateCookieName";

	@Autowired
	JLFUListenerPropagator jlfuListenerPropagator;



	@Override
	public void setIdentifier(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, UUID id) {

		// clear any first
		clearIdentifier(httpServletRequest, httpServletResponse);

		// then set in session
		httpServletRequest.getSession().setAttribute(cookieIdentifier, id);

		// and cookie
		setCookie(httpServletResponse, id);

	}


	public static Cookie getCookie(Cookie[] cookies, String id) {
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(id)) {
					// found it
					if (cookie.getMaxAge() != 0) {
						return cookie;
					}
				}
			}
		}
		return null;
	}


	public static void setCookie(HttpServletResponse response, UUID uuid) {
		Cookie cookie = new Cookie(cookieIdentifier, uuid.toString());
		cookie.setMaxAge((int) TimeUnit.DAYS.toSeconds(31));
		response.addCookie(cookie);
	}


	UUID getUuid() {
		final UUID uuid = UUID.randomUUID();
		jlfuListenerPropagator.getPropagator().onNewClient(uuid);
		return uuid;
	}


	@Override
	public UUID getIdentifier(HttpServletRequest req, HttpServletResponse resp) {

		// get from session
		UUID uuid = (UUID) req.getSession().getAttribute(cookieIdentifier);

		// if nothing in session
		if (uuid == null) {

			// check in cookie
			Cookie cookie = getCookie(req.getCookies(), cookieIdentifier);
			if (cookie != null && cookie.getValue() != null) {
				// set in session
				uuid = UUID.fromString(cookie.getValue());
				req.getSession().setAttribute(cookieIdentifier, uuid);
				jlfuListenerPropagator.getPropagator().onClientBack(uuid);
				return uuid;
			}

			// if not in session nor cookie, create one
			// create uuid
			uuid = getUuid();

			// and set it
			setIdentifier(req, resp, uuid);

		}
		return uuid;
	}


	@Override
	public void clearIdentifier(HttpServletRequest req, HttpServletResponse resp) {
		// clear session
		req.getSession().removeAttribute(cookieIdentifier);

		// remove cookie
		Cookie cookie = getCookie(req.getCookies(), cookieIdentifier);
		if (cookie != null) {
			cookie.setMaxAge(0);
			resp.addCookie(cookie);
		}
	}

}
