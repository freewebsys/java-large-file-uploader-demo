package com.am.jlfu.fileuploader.web;


import java.io.IOException;
import java.util.UUID;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public abstract class UploadServletAsyncListenerAdapter
		implements AsyncListener {

	private static final Logger log = LoggerFactory.getLogger(UploadServletAsyncListenerAdapter.class);

	private UUID id;



	public UploadServletAsyncListenerAdapter(UUID identifier) {
		this.id = identifier;
	}


	abstract void clean();


	@Override
	public void onComplete(AsyncEvent asyncEvent)
			throws IOException {
		log.debug("Done: ({})", id);
		clean();
	}


	@Override
	public void onTimeout(AsyncEvent asyncEvent)
			throws IOException {
		log.warn("Asynchronous request timeout ({})", id);
		clean();
	}


	@Override
	public void onError(AsyncEvent asyncEvent)
			throws IOException {
		log.error("Asynchronous request error (" + id + ")", asyncEvent.getThrowable());
		clean();
	}


	@Override
	public void onStartAsync(AsyncEvent asyncEvent)
			throws IOException {
		log.debug("Started: ({})", id);
	}
}
