package com.am.jlfu.fileuploader.web;


import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.fileuploader.json.FileStateJson;
import com.am.jlfu.fileuploader.json.InitializationConfiguration;
import com.am.jlfu.fileuploader.json.PrepareUploadJson;
import com.am.jlfu.fileuploader.json.ProgressJson;
import com.am.jlfu.fileuploader.json.SimpleJsonObject;
import com.am.jlfu.fileuploader.web.utils.ExceptionCodeMappingHelper.ExceptionCodeMapping;
import com.am.jlfu.fileuploader.web.utils.RequestComponentContainer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class UploadServletTest {

	@Autowired
	UploadServlet uploadServlet;

	@Autowired
	UploadServletAsync uploadServletAsync;

	@Autowired
	RequestComponentContainer requestComponentContainer;

	MockHttpServletRequest request;
	MockHttpServletResponse response;

	private byte[] content = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
	private MockMultipartFile file = new MockMultipartFile("blob", content);



	@Before
	public void init() {

		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();

		// populate request component container
		requestComponentContainer.populate(request, response);

	}


	@Test
	public void getConfig()
			throws IOException {

		// init an upload to emulate a pending file
		String fileId = prepareUpload();

		// set action parameter
		request.clearAttributes();
		response = new MockHttpServletResponse();
		request.setParameter(UploadServletParameter.action.name(), UploadServletAction.getConfig.name());

		// handle request
		uploadServlet.handleRequest(request, response);

		// extract config from response
		InitializationConfiguration fromJson = new Gson().fromJson(response.getContentAsString(), InitializationConfiguration.class);
		Assert.assertNotNull(fromJson.getInByte());
		Assert.assertThat(response.getStatus(), is(200));
		Map<String, FileStateJson> pendingFiles = fromJson.getPendingFiles();
		Assert.assertThat(pendingFiles.size(), is(1));
		Assert.assertThat(pendingFiles.keySet().iterator().next(), is(fileId));
	}


	@Test
	public void getProgressWithBadId()
			throws IOException {

		// set action parameter
		request.setParameter(UploadServletParameter.action.name(), UploadServletAction.getProgress.name());
		String id = "a bad id";
		request.setParameter(UploadServletParameter.fileId.name(), new Gson().toJson(new String[] { id }));

		// handle request
		uploadServlet.handleRequest(request, response);

		SimpleJsonObject fromJson = new Gson().fromJson(response.getContentAsString(), SimpleJsonObject.class);
		Assert.assertThat(fromJson.getValue(), is("0"));

	}


	@Test
	public void getProgress()
			throws IOException {

		// init an upload to emulate a pending file
		String fileId = prepareUpload();

		// set action parameter
		request.clearAttributes();
		response = new MockHttpServletResponse();
		request.setParameter(UploadServletParameter.action.name(), UploadServletAction.getProgress.name());
		request.setParameter(UploadServletParameter.fileId.name(), new Gson().toJson(new String[] { fileId }));

		// handle request
		uploadServlet.handleRequest(request, response);
		Assert.assertThat(response.getStatus(), is(200));

		HashMap<String, ProgressJson> fromJson = new Gson().fromJson(response.getContentAsString(), new TypeToken<Map<String, ProgressJson>>() {
		}.getType());
		ProgressJson[] array = new ProgressJson[] {};
		array = fromJson.values().toArray(array);
		Assert.assertThat(array[0].getProgress(), is(Float.valueOf(0)));

	}


	@Test
	public void uploadNotMultipartParams()
			throws IOException, ServletException {

		// handle request
		uploadServletAsync.handleRequest(request, response);
		SimpleJsonObject fromJson = new Gson().fromJson(response.getContentAsString(), SimpleJsonObject.class);
		Assert.assertThat(ExceptionCodeMapping.requestIsNotMultipart.getExceptionIdentifier(), is(Integer.valueOf(fromJson.getValue())));

	}


	@Test
	public void prepareUploadTest()
			throws IOException {
		prepareUpload();
	}


	public String prepareUpload()
			throws IOException {

		return (String) prepareUpload(1).values().toArray()[0];

	}


	public Map<String, String> prepareUpload(int size)
			throws IOException {

		// set action parameter
		request.setParameter(UploadServletParameter.action.name(), UploadServletAction.prepareUpload.name());
		PrepareUploadJson[] prepareUploadJsons = new PrepareUploadJson[size];
		for (int i = 0; i < size; i++) {
			PrepareUploadJson j = new PrepareUploadJson();
			j.setTempId(i);
			j.setFileName("file " + i);
			j.setSize(123456l);
			prepareUploadJsons[i] = j;
		}
		request.setParameter(UploadServletParameter.newFiles.name(), new Gson().toJson(prepareUploadJsons));

		// handle request
		uploadServlet.handleRequest(request, response);
		Assert.assertThat(response.getStatus(), is(200));
		HashMap<String, String> fromJson = new Gson().fromJson(response.getContentAsString(), new TypeToken<Map<String, String>>() {
		}.getType());

		return fromJson;
	}


	@Test
	public void prepareUploadMulti()
			throws IOException {
		Map<String, String> prepareUpload = prepareUpload(10);
		Assert.assertThat(prepareUpload.size(), is(10));
	}


	// upload,
	// prepareUpload,
	// clearFile,
	// clearAll;


	@Test
	public void clearFileWithMissingParameter()
			throws IOException {

		// set action parameter
		request.setParameter(UploadServletParameter.action.name(), UploadServletAction.clearFile.name());

		// handle request
		uploadServlet.handleRequest(request, response);

		// assert that we have an error
		SimpleJsonObject fromJson = new Gson().fromJson(response.getContentAsString(), SimpleJsonObject.class);

		Assert.assertThat(ExceptionCodeMapping.MissingParameterException.getExceptionIdentifier(), is(Integer.valueOf(fromJson.getValue())));

	}


	@Test
	public void testGetMultiFileIdsFromString() {
		UUID uuid1 = UUID.randomUUID();
		UUID uuid2 = UUID.randomUUID();
		List<UUID> fileIdsFromString = uploadServlet.getFileIdsFromString(uuid1+","+uuid2);
		Assert.assertThat(fileIdsFromString.size(), CoreMatchers.is(2));
		Assert.assertTrue(fileIdsFromString.contains(uuid1));
		Assert.assertTrue(fileIdsFromString.contains(uuid2));
	}

	@Test
	public void testGetOneFileIdFromString() {
		UUID uuid1 = UUID.randomUUID();
		List<UUID> fileIdsFromString = uploadServlet.getFileIdsFromString(uuid1.toString());
		Assert.assertThat(fileIdsFromString.size(), CoreMatchers.is(1));
		Assert.assertTrue(fileIdsFromString.contains(uuid1));
	}
	
	
}
