package com.am.jlfu.authorizer.impl;


import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.am.jlfu.authorizer.Authorizer;
import com.am.jlfu.fileuploader.exception.AuthorizationException;
import com.am.jlfu.fileuploader.web.UploadServletAction;



/**
 * Default {@link Authorizer} that never throws an {@link AuthorizationException}.
 * 
 * @author antoinem
 * 
 */
@Component
public class DefaultAuthorizer
		implements Authorizer {

    private static final Logger log = LoggerFactory.getLogger(DefaultAuthorizer.class);

    @Override
	public void getAuthorization(HttpServletRequest request, UploadServletAction action, UUID clientId, UUID... optionalFileId) {
		// by default, all calls are authorized

	}

}
