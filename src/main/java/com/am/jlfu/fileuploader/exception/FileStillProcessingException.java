package com.am.jlfu.fileuploader.exception;

import java.util.UUID;


public class FileStillProcessingException extends Exception {

	
	public FileStillProcessingException(UUID fileId) {
		super("The file "+fileId+" is still in a process. AsyncRequest ignored.");
	}
	
}
