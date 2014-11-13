package com.am.jlfu.fileuploader.json;


import java.io.Serializable;



public class ProgressJson
		implements Serializable {

	/**
	 * generated id
	 */
	private static final long serialVersionUID = -8710522591230352636L;

	protected Float progress;
	protected Long uploadRate;
	protected Long estimatedRemainingTimeInSeconds;


	public ProgressJson() {
	}

	
	/**
	 * @return the percentage completed.
	 */
	public Float getProgress() {
		return progress;
	}


	public void setProgress(Float progress) {
		this.progress = progress;
	}

	/**
	 * @return current file upload rate in byte per second.
	 */
	public Long getUploadRate() {
		return uploadRate;
	}


	public void setUploadRate(Long uploadRate) {
		this.uploadRate = uploadRate;
	}


	/**
	 * @return the estimated remaining time in seconds.
	 */
	public Long getEstimatedRemainingTimeInSeconds() {
		return estimatedRemainingTimeInSeconds;
	}


	
	public void setEstimatedRemainingTimeInSeconds(Long estimatedRemainingTimeInSeconds) {
		this.estimatedRemainingTimeInSeconds = estimatedRemainingTimeInSeconds;
	}

}
