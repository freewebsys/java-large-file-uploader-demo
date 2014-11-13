package com.am.jlfu.fileuploader.exception;


import com.am.jlfu.fileuploader.web.utils.ExceptionCodeMappingHelper.ExceptionCodeMapping;



/**
 * This exception contains a simple identifier ({@link #exceptionIdentifier}) so that the javascript
 * can identify it and i18n it.
 * 
 * @author antoinem
 * 
 */
public class JavaFileUploaderException extends Exception {

	private ExceptionCodeMapping exceptionCodeMapping;



	public JavaFileUploaderException() {
	}


	public JavaFileUploaderException(ExceptionCodeMapping exceptionCodeMapping) {
		super();
		this.exceptionCodeMapping = exceptionCodeMapping;
	}


	public ExceptionCodeMapping getExceptionCodeMapping() {
		return exceptionCodeMapping;
	}


	public void setExceptionCodeMapping(ExceptionCodeMapping exceptionCodeMapping) {
		this.exceptionCodeMapping = exceptionCodeMapping;
	}


}
