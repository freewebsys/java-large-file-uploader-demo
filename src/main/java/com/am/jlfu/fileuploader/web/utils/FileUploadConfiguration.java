package com.am.jlfu.fileuploader.web.utils;


import java.io.InputStream;
import java.util.UUID;



public class FileUploadConfiguration {

	private UUID fileId;
	private String crc;
	private InputStream inputStream;



	public FileUploadConfiguration() {
	}


	public UUID getFileId() {
		return fileId;
	}


	public void setFileId(UUID fileId) {
		this.fileId = fileId;
	}


	public String getCrc() {
		return crc;
	}


	public void setCrc(String crc) {
		this.crc = crc;
	}


	public InputStream getInputStream() {
		return inputStream;
	}


	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}


}
