package com.am.jlfu.fileuploader.json;

import java.io.Serializable;


public class SimpleJsonObject
		implements Serializable {

	/**
	 * generated id
	 */
	private static final long serialVersionUID = 1815625862539981019L;

	/**
	 * Value.
	 */
	private String value;



	public SimpleJsonObject(String value) {
		super();
		this.value = value;
	}


	public SimpleJsonObject() {
		super();
	}


	public String getValue() {
		return value;
	}


	public void setValue(String value) {
		this.value = value;
	}


}
