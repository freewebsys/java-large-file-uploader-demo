package com.am.jlfu.fileuploader.utils;


public abstract class ConditionProvider {

	public abstract boolean condition();


	public void onFail() {
	}


	public void onSuccess() {

	}
}
