package com.am.jlfu.fileuploader.exception;


import java.util.Arrays;
import java.util.UUID;

import com.am.jlfu.fileuploader.web.UploadServletAction;



public class AuthorizationException extends Exception {

	public AuthorizationException(UploadServletAction actionByParameterName, UUID clientId, UUID... optionalFileIds) {
		super("User " + clientId + " is not authorized to perform " + actionByParameterName + (optionalFileIds != null ? " on " + Arrays.toString(optionalFileIds) : ""));
	}


}
