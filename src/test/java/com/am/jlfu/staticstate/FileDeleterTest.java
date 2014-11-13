package com.am.jlfu.staticstate;


import static org.hamcrest.CoreMatchers.is;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;



@ContextConfiguration(locations = { "classpath:jlfu.test.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class FileDeleterTest {

	@Autowired
	FileDeleter fileDeleter;

	@Autowired
	StaticStateRootFolderProvider staticStateRootFolderProvider;

	private int number = 100;
	private ExecutorService exec = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(20));
	private File[] files = new File[number];



	@Before
	public void writeFiles()
			throws IOException {
		for (int i = 0; i < number; i++) {
			if (i % 2 == 0) {
				File file = new File(staticStateRootFolderProvider.getRootFolder(), i + "testdir");
				file.mkdirs();
				files[i] = file;
			}
			else {
				files[i] = File.createTempFile("temp", "temp");
			}
		}
	}


	@Test
	public void deleteMultipleFilesSubmittedConcurrently()
			throws Exception {
		List<ListenableFuture<?>> futures = Lists.newArrayList();
		fileDeleter.run();
		for (int i = 0; i < number; i++) {
			final File toDelete = files[i];
			futures.add((ListenableFuture<?>) exec.submit(new Runnable() {

				@Override
				public void run() {
					fileDeleter.deleteFile(toDelete);
				}
			}));
		}
		fileDeleter.run();
		ListenableFuture<List<Object>> allAsList = Futures.allAsList(futures);
		fileDeleter.run();
		Futures.get(allAsList, 2, TimeUnit.SECONDS, Exception.class);
		fileDeleter.run();
		for (File file : files) {
			Assert.assertThat(file.exists(), is(Boolean.FALSE));
		}
	}


	@Test
	public void deleteFileThatIsOpen()
			throws IOException, InterruptedException {
		File file = File.createTempFile("temp", "temp");
		FileInputStream fileInputStream = new FileInputStream(file);
		fileInputStream.read();
		fileDeleter.deleteFile(file);
		fileDeleter.run();
		Assert.assertThat(file.exists(), is(Boolean.TRUE));
		fileInputStream.close();
		fileDeleter.run();
		Assert.assertThat(file.exists(), is(Boolean.FALSE));
	}


	@Test
	public void deleteFileThatIsOpenInADirectory()
			throws IOException, InterruptedException {
		File dir = new File(staticStateRootFolderProvider.getRootFolder(), "zetestdir");
		dir.mkdirs();
		File file = new File(dir, "file");
		file.createNewFile();
		Assert.assertThat(file.exists(), is(Boolean.TRUE));
		Assert.assertThat(dir.exists(), is(Boolean.TRUE));
		FileInputStream fileInputStream = new FileInputStream(file);
		fileInputStream.read();
		fileDeleter.deleteFile(dir);
		fileDeleter.run();
		Assert.assertThat(file.exists(), is(Boolean.TRUE));
		Assert.assertThat(dir.exists(), is(Boolean.TRUE));
		fileInputStream.close();
		fileDeleter.run();
		Assert.assertThat(file.exists(), is(Boolean.FALSE));
		Assert.assertThat(dir.exists(), is(Boolean.FALSE));
	}


	@Test
	public void deleteFileThatIsNotAFile()
			throws IOException {
		File fake = new File("lalala");
		File file = File.createTempFile("temp", "temp");
		Assert.assertThat(fake.exists(), is(Boolean.FALSE));
		Assert.assertThat(file.exists(), is(Boolean.TRUE));
		fileDeleter.deleteFile(fake);
		fileDeleter.deleteFile(file);
		fileDeleter.run();
		Assert.assertThat(fake.exists(), is(Boolean.FALSE));
		Assert.assertThat(file.exists(), is(Boolean.FALSE));
	}

}
