package com.am.jlfu.staticstate;


import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.fileuploader.json.FileStateJsonBase;
import com.am.jlfu.fileuploader.web.utils.RequestComponentContainer;
import com.am.jlfu.staticstate.entities.StaticFileState;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class StaticStateManagerTest {

	@Autowired
	FileDeleter fileDeleter;

	@Autowired
	StaticStateManager<StaticStatePersistedOnFileSystemEntity> staticStateManager;

	@Autowired
	StaticStateDirectoryManager staticStatedDirectoryManager;

	@Autowired
	StaticStateIdentifierManager staticStateIdentifierManager;

	@Autowired
	RequestComponentContainer requestComponentContainer;



	@Before
	public void init() {

		// populate request component container
		requestComponentContainer.populate(new MockHttpServletRequest(), new MockHttpServletResponse());

		staticStateManager.init(StaticStatePersistedOnFileSystemEntity.class);
	}


	@Test
	public void testClear()
			throws InterruptedException, ExecutionException, TimeoutException {

		// get entity
		staticStateManager.getEntity();

		// assert directory is there
		File uuidFileParent = staticStatedDirectoryManager.getUUIDFileParent();
		Assert.assertTrue(uuidFileParent.exists());

		// clear
		staticStateManager.clear();

		// force file deleter to delete stuff
		fileDeleter.run();

		// assert directory is deleted
		Assert.assertFalse(uuidFileParent.exists());
	}


	@Test
	public void testClearFile()
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		String randomValue = "a";
		UUID fileId = UUID.randomUUID();

		// get entity
		StaticStatePersistedOnFileSystemEntity entity = staticStateManager.getEntity();
		StaticFileState value = new StaticFileState();
		FileStateJsonBase staticFileStateJson = new FileStateJsonBase();
		value.setStaticFileStateJson(staticFileStateJson);
		entity.getFileStates().put(fileId, value);

		// populate it
		value.setAbsoluteFullPathOfUploadedFile(randomValue);
		staticFileStateJson.setOriginalFileName(randomValue);
		staticFileStateJson.setOriginalFileSizeInBytes(123000l);

		// create a file
		File file = new File(staticStatedDirectoryManager.getUUIDFileParent(), fileId.toString());
		file.createNewFile();
		Assert.assertTrue(file.exists());

		// clear it
		staticStateManager.clearFile(fileId);

		// reget it
		StaticFileState staticFileState = staticStateManager.getEntity().getFileStates().get(fileId);
		Assert.assertNull(staticFileState);

		// force file deleter to run
		fileDeleter.run();

		// assert file deleted
		Assert.assertFalse(file.exists());
	}


	@Test
	public void testGetEntityFromFile() {
		String absoluteFullPathOfUploadedFile = "value";
		UUID fileId = UUID.randomUUID();

		// get entity
		StaticStatePersistedOnFileSystemEntity entity = staticStateManager.getEntity();
		StaticFileState value = new StaticFileState();
		entity.getFileStates().put(fileId, value);

		// put some stuff in the file
		value.setAbsoluteFullPathOfUploadedFile(absoluteFullPathOfUploadedFile);
		staticStateManager.updateEntity(entity);

		// remove from cache
		staticStateManager.cache.invalidate(staticStateIdentifierManager.getIdentifier());

		// get again (it will load from file into cache)
		staticStateManager.getEntity();

		// check everything is good
		Assert.assertEquals(absoluteFullPathOfUploadedFile, value.getAbsoluteFullPathOfUploadedFile());


	}


}
