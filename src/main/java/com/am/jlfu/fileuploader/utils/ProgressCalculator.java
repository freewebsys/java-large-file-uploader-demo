package com.am.jlfu.fileuploader.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.limiter.RateLimiterConfigurationManager;
import com.am.jlfu.staticstate.JavaLargeFileUploaderService;
import com.am.jlfu.staticstate.entities.FileProgressStatus;
import com.am.jlfu.staticstate.entities.StaticFileState;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;

/**
 * Component to calculate the information related to a file currently uploading.
 * @author antoinem
 *
 */
@Component
public class ProgressCalculator {

	private static final Logger log = LoggerFactory.getLogger(ProgressCalculator.class);

	@Autowired
	JavaLargeFileUploaderService<StaticStatePersistedOnFileSystemEntity> javaLargeFileUploaderService;

	@Autowired
	RateLimiterConfigurationManager rateLimiterConfigurationManager;

	@Autowired
	RemainingTimeEstimator remainingTimeEstimator;

	
	/**
	 * Retrieves the progress of the specified file for the specified client.<br>
	 * 
	 * 
	 * @param clientId
	 * @param fileId
	 * @return
	 * @throws FileNotFoundException
	 */
	public FileProgressStatus getProgress(UUID clientId, UUID fileId) throws FileNotFoundException {
		// get the file
		StaticStatePersistedOnFileSystemEntity model = javaLargeFileUploaderService.getEntityIfPresent(clientId);
		//if cannot find the model, return null
		if (model == null) {
			return null;
		}
		return processProgress(fileId, model);

	}


	private FileProgressStatus processProgress(UUID fileId, StaticStatePersistedOnFileSystemEntity model)
			throws FileNotFoundException {
		StaticFileState fileState = model.getFileStates().get(fileId);
		if (fileState == null) {
			throw new FileNotFoundException("File with id " + fileId + " not found");
		}
		File file = new File(fileState.getAbsoluteFullPathOfUploadedFile());

		//init returned entity
		FileProgressStatus fileProgressStatus = new FileProgressStatus();

		// compare size of the file to the expected size
		Long originalFileSizeInBytes = fileState.getStaticFileStateJson().getOriginalFileSizeInBytes();
		long currentFileSize = file.length();
		Float progress = calculateProgress(currentFileSize, originalFileSizeInBytes).floatValue();

		//set it
		fileProgressStatus.setProgress(progress);
		fileProgressStatus.setTotalFileSize(originalFileSizeInBytes);
		fileProgressStatus.setBytesUploaded(currentFileSize);

		//set upload rate
		fileProgressStatus.setUploadRate(rateLimiterConfigurationManager.getUploadState(fileId));
		
		//calculate estimated remaining time
		fileProgressStatus.setEstimatedRemainingTimeInSeconds(remainingTimeEstimator.getRemainingTime(fileId, fileProgressStatus, fileProgressStatus.getUploadRate()));
		
		//log file progress status
		log.debug("Calculated progress for file "+fileId+": "+fileProgressStatus);
		
		return fileProgressStatus;
	}
	


	Double calculateProgress(Long currentSize, Long expectedSize) {
		double percent = currentSize.doubleValue() / expectedSize.doubleValue() * 100d;
		if (percent == 100 && expectedSize - currentSize != 0) {
			percent = 99.99d;
		}
		return percent;
	}

	
}
