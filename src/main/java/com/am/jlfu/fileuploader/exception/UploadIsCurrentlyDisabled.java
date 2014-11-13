package com.am.jlfu.fileuploader.exception;

import com.am.jlfu.staticstate.JavaLargeFileUploaderService;


/**
 * Exception thrown if the uploads are not enabled at the moment.
 * @see JavaLargeFileUploaderService#enableFileUploader()
 * @see JavaLargeFileUploaderService#disableFileUploader()
 * @author antoinem
 */
public class UploadIsCurrentlyDisabled extends Exception {

	public UploadIsCurrentlyDisabled() {
		super("All uploads are currently suspended.");
	}
	
}
