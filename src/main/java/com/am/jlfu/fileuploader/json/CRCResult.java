package com.am.jlfu.fileuploader.json;


import java.io.Serializable;

import com.am.jlfu.fileuploader.utils.CRCHelper;



/**
 * This class is the result of a method of {@link CRCHelper}.
 * It includes the CRC32 value as a string ({@link #value}) and the number of bytes computed (
 * {@link #read})
 * 
 * @author antoinem
 * @see CRCHelper
 * 
 */
public class CRCResult
		implements Serializable {


	/**
	 * generated id
	 */
	private static final long serialVersionUID = 5435020922997235085L;

	private String value;
	private int read;



	public CRCResult() {
	}


	public String getCrcAsString() {
		return value;
	}


	public void setCrcAsString(String crcAsString) {
		this.value = crcAsString;
	}


	public int getTotalRead() {
		return read;
	}


	public void setTotalRead(int streamLength) {
		this.read = streamLength;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + read;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CRCResult other = (CRCResult) obj;
		if (read != other.read)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		}
		else if (!value.equals(other.value))
			return false;
		return true;
	}


}
