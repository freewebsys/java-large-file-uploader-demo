package com.am.jlfu.fileuploader.exception;


import com.am.jlfu.fileuploader.web.UploadServletParameter;



public class MissingParameterException extends BadRequestException {

	public MissingParameterException(UploadServletParameter parameter) {
		super("The parameter " + parameter.name() + " is missing for this request.");
	}


}
