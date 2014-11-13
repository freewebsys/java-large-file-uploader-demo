package com.am.jlfu.fileuploader.limiter;


public class RequestUploadProcessingConfiguration extends UploadProcessingConfiguration {

	/**
	 * Boolean specifying whether the upload is processing or not.
	 */
	private volatile boolean isProcessing;

	/** 
	 * Boolean specifying whether the client uploading the file is telling the server that it should not process the stream read.
	 */
	private volatile boolean paused;

	
	public boolean isProcessing() {
		return isProcessing;
	}


	public void setProcessing(boolean isProcessing) {
		this.isProcessing = isProcessing;
	}


	public void pause() {
		this.paused = true;
	}

	public void resume() {
		this.paused = false;
	}
	
	public boolean isPaused() {
		return this.paused;
	}


}
