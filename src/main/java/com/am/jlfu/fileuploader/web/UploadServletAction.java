package com.am.jlfu.fileuploader.web;


/**
 * One of the possible action that the servlet handles.
 * 
 * @author antoinem
 * 
 */
public enum UploadServletAction {

	getConfig,
	getProgress,
	prepareUpload,
	clearFile,
	setRate,
	resumeFile,
	pauseFile,
	verifyCrcOfUncheckedPart,
	clearAll,
	upload;


}
