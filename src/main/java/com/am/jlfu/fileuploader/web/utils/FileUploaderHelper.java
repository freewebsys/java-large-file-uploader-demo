package com.am.jlfu.fileuploader.web.utils;


import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.exception.JavaFileUploaderException;
import com.am.jlfu.fileuploader.exception.MissingParameterException;
import com.am.jlfu.fileuploader.json.SimpleJsonObject;
import com.am.jlfu.fileuploader.web.UploadServletParameter;
import com.am.jlfu.fileuploader.web.utils.ExceptionCodeMappingHelper.ExceptionCodeMapping;
import com.google.gson.Gson;



/**
 * Provides some common methods to deal with file upload requests.
 * 
 * @author antoinem
 * 
 */
@Component
public class FileUploaderHelper {


	public FileUploadConfiguration extractFileUploadConfiguration(HttpServletRequest request)
			throws MissingParameterException, FileUploadException, IOException, JavaFileUploaderException {
		final FileUploadConfiguration fileUploadConfiguration = new FileUploadConfiguration();

		// check if the request is multipart:
		if (!ServletFileUpload.isMultipartContent(request)) {
			throw new JavaFileUploaderException(ExceptionCodeMapping.requestIsNotMultipart);
		}

		// extract the fields
		fileUploadConfiguration.setFileId(UUID.fromString(getParameterValue(request, UploadServletParameter.fileId)));
		fileUploadConfiguration.setCrc(getParameterValue(request, UploadServletParameter.crc, false));

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload();

		// parse the requestuest
		FileItemIterator iter = upload.getItemIterator(request);
		FileItemStream item = iter.next();

		// throw exception if item is null
		if (item == null) {
			throw new JavaFileUploaderException(ExceptionCodeMapping.NoFileToUploadInTheRequest);
		}

		// extract input stream
		fileUploadConfiguration.setInputStream(item.openStream());

		// return conf
		return fileUploadConfiguration;

	}


	public String getParameterValue(HttpServletRequest request, UploadServletParameter parameter)
			throws MissingParameterException {
		return getParameterValue(request, parameter, true);
	}


	public String getParameterValue(HttpServletRequest request, UploadServletParameter parameter, boolean mandatory)
			throws MissingParameterException {
		String parameterValue = request.getParameter(parameter.name());
		if (parameterValue == null && mandatory) {
			throw new MissingParameterException(parameter);
		}
		return parameterValue;
	}


	public void writeExceptionToResponse(final JavaFileUploaderException e, ServletResponse servletResponse)
			throws IOException {
		writeToResponse(new SimpleJsonObject(Integer.valueOf(e.getExceptionCodeMapping().getExceptionIdentifier()).toString()), servletResponse);
	}


	public void writeToResponse(Serializable jsonObject, ServletResponse servletResponse)
			throws IOException {
		servletResponse.setContentType("application/json");
		servletResponse.getWriter().print(new Gson().toJson(jsonObject));
		servletResponse.getWriter().close();
	}
}
