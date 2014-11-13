package com.am.jlfu.staticstate;


import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;



/**
 * Takes care of deleting the files.
 * 
 * @author antoinem
 * 
 */
@Component
public class FileDeleter
		implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(FileDeleter.class);

	/** The executor */
	private ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);



	@PostConstruct
	private void start() {
		executor.schedule(this, 10, TimeUnit.SECONDS);
	}



	/**
	 * List of the files to delete.
	 */
	private List<File> files = Lists.newArrayList();



	@Override
	public void run() {

		// extract all the files to an immutable list
		ImmutableList<File> copyOf;
		synchronized (this.files) {
			copyOf = ImmutableList.copyOf(this.files);
		}

		// log
		boolean weHaveFilesToDelete = !copyOf.isEmpty();
		if (weHaveFilesToDelete) {
			log.debug(copyOf.size() + " files to delete");
		}

		// and create a new list
		List<File> successfullyDeletedFiles = Lists.newArrayList();

		// delete them
		for (File file : copyOf) {
			if (delete(file)) {
				successfullyDeletedFiles.add(file);
				log.debug(file + " successfully deleted.");
			}
			else {
				log.debug(file + " not deleted, rescheduled for deletion.");
			}
		}

		// all the files have been processed
		// remove the deleted files from queue
		synchronized (this.files) {
			Iterables.removeAll(this.files, successfullyDeletedFiles);
		}

		// log
		if (weHaveFilesToDelete) {
			log.debug(successfullyDeletedFiles.size() + " deleted files");
		}

		// and reschedule
		start();
	}


	/**
	 * @param file
	 * @return true if the file has been deleted, false otherwise.
	 */
	private boolean delete(File file) {

		try {
			// if file exists
			if (file.exists()) {

				// if it is a file
				if (file.isFile()) {
					// delete it
					return file.delete();
				}
				// otherwise, if it is a directoy
				else if (file.isDirectory()) {
					FileUtils.deleteDirectory(file);
					return true;
				}
				// if its none of them, we cannot delete them so we assume its deleted.
				else {
					return true;
				}

			}
			// if does not exist, we can remove it from list
			else {
				return true;
			}
		}
		// if we have an exception
		catch (Exception e) {
			log.error(file + " deletion exception: " + e.getMessage());
			// the file has not been deleted
			return false;
		}

	}


	public void deleteFile(File... file) {
		deleteFiles(Arrays.asList(file));
	}


	public void deleteFiles(Collection<File> files) {
		synchronized (this.files) {
			this.files.addAll(files);
		}
	}


	/**
	 * Returns true if the specified file is scheduled for deletion
	 * 
	 * @param file
	 * @return
	 */
	public boolean deletionQueueContains(File file) {
		synchronized (this.files) {
			return files.contains(file);
		}
	}
	
	
	@PreDestroy
	private void destroy() throws InterruptedException {
		log.debug("destroying executor");
		executor.shutdown();
		if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
			log.error("executor timed out");
			List<Runnable> shutdownNow = executor.shutdownNow();
			for (Runnable runnable : shutdownNow) {
				log.error(runnable + "has not been terminated");
			}
		}
	}

}
