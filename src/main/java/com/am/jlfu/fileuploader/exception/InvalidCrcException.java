package com.am.jlfu.fileuploader.exception;

public class InvalidCrcException extends Exception {

	public InvalidCrcException(String crc32, String crc) {
		super("The file chunk is invalid. Expected "+crc32+" but received "+crc);
	}


	
	
}
