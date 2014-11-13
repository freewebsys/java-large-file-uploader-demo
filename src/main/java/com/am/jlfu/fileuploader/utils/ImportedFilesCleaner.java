package com.am.jlfu.fileuploader.utils;


import java.io.File;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.am.jlfu.staticstate.FileDeleter;
import com.am.jlfu.staticstate.StaticStateRootFolderProvider;



/**
 * This cleaner shall be regularly invoked to remove files that are outdated on the system.<br/>
 * That is checked against the last modified time which shall be no more than what is configured.
 * 
 * @author antoinem
 */
@Component
@ManagedResource(objectName = "JavaLargeFileUploader:name=importedFilesCleaner")
public class ImportedFilesCleaner {

	private static final Logger log = LoggerFactory.getLogger(ImportedFilesCleaner.class);

	@Autowired
	StaticStateRootFolderProvider rootFolderProvider;

	@Autowired
	FileDeleter fileDeleter;

	@Value("jlfu{jlfu.filecleaner.maximumInactivityInHoursBeforeDelete:48}")
	volatile Integer maximumInactivityInHoursBeforeDelete;



	public void clean() {
		log.trace("#####################Started file cleaner job.#####################");
		DateTime pastTime = new DateTime().minusHours(maximumInactivityInHoursBeforeDelete);
		File[] listFiles = rootFolderProvider.getRootFolder().listFiles();
		for (File file : listFiles) {
			if (pastTime.isAfter(file.lastModified())) {
				log.debug("Deleting outdated file: " + file.getName());
				fileDeleter.deleteFile(file);
			}
		}
		log.trace("Finished file cleaner job.");
	}


	@ManagedAttribute
	public Integer getMaximumInactivityInHoursBeforeDelete() {
		return maximumInactivityInHoursBeforeDelete;
	}


	@ManagedAttribute
	public void setMaximumInactivityInHoursBeforeDelete(Integer maximumInactivityInHoursBeforeDelete) {
		this.maximumInactivityInHoursBeforeDelete = maximumInactivityInHoursBeforeDelete;
	}


}
