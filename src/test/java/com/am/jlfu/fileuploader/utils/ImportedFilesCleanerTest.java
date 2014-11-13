package com.am.jlfu.fileuploader.utils;


import java.io.File;
import java.io.IOException;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.am.jlfu.staticstate.FileDeleter;
import com.am.jlfu.staticstate.StaticStateRootFolderProvider;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class ImportedFilesCleanerTest {

	@Autowired
	StaticStateRootFolderProvider staticStateRootFolderProvider;

	@Autowired
	ImportedFilesCleaner importedFilesCleaner;

	@Autowired
	FileDeleter fileDeleter;



	@Test
	public void test()
			throws IOException {
		// put some files
		File rootFolder = staticStateRootFolderProvider.getRootFolder();

		// old one
		File oldDir = new File(rootFolder, "oldDir");
		oldDir.mkdir();
		oldDir.setLastModified(new DateTime().minusMonths(3).getMillis());

		// recent one
		File recentDir = new File(rootFolder, "recentDir");
		recentDir.mkdir();

		// process
		importedFilesCleaner.clean();

		// call file deleter
		fileDeleter.run();

		// assume old is deleted
		Assert.assertFalse(oldDir.exists());

		// assume new os still there
		Assert.assertTrue(recentDir.exists());


	}

}
