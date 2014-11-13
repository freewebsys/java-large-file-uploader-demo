package com.am.jlfu.fileuploader.utils;


import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.am.jlfu.notifier.JLFUListener;
import com.am.jlfu.notifier.JLFUListenerPropagator;
import com.am.jlfu.staticstate.entities.FileProgressStatus;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;



/**
 * Component responsible for advertising the write progress of a specific file to
 * {@link JLFUListener}s.<br>
 * Every second, it calculates the progress of all the files being uploaded.
 * 
 * @author antoinem
 * 
 */
@Component
public class ProgressManager {
	private static final Logger log = LoggerFactory.getLogger(ProgressManager.class);

	@Autowired
	private JLFUListenerPropagator jlfuListenerPropagator;

	@Autowired
	private ClientToFilesMap clientToFilesMap;
	
	@Autowired
	private ProgressCalculator progressCalculator;

	/** Internal map. */
	Map<UUID, FileProgressStatus> fileToProgressInfo = Maps.newHashMap();
	
	/** Simple advertiser. */
	ProgressManagerAdvertiser progressManagerAdvertiser = new ProgressManagerAdvertiser();
	
	@Scheduled(fixedRate = 1000)
	public void calculateProgress() {

		synchronized (fileToProgressInfo) {
		
			//for all clients
			for (Entry<UUID, Set<UUID>> entry : clientToFilesMap.entrySet()) {
				
				//for all pending upload
				Set<UUID> originSet = entry.getValue();
				Set<UUID> copySet;
				synchronized (originSet) {
					 copySet = Sets.newHashSet(originSet);
				}
				for (UUID fileId : copySet) {
				
					try {
						
						//calculate its progress
						FileProgressStatus newProgress = progressCalculator.getProgress(entry.getKey(), fileId);

						//if progress has successfully been computed
						if (newProgress != null) {
							
							//get from map
							FileProgressStatus progressInMap = fileToProgressInfo.get(fileId);
							
							//if not present in map
							//or if present in map but different from previous one
							if (progressInMap == null || !progressInMap.getProgress().equals(newProgress.getProgress())) {
								
								//add to map
								fileToProgressInfo.put(fileId, newProgress);
								
								// and avertise
								progressManagerAdvertiser.advertise(entry.getKey(), fileId, newProgress);
								
							}
						}
						
					}
					catch (FileNotFoundException e) {
						log.debug("cannot retrieve progress for "+fileId);
					}
					
				}
			}

		}
	}
	
	class ProgressManagerAdvertiser {
		
		void advertise(UUID clientId, UUID fileId, FileProgressStatus newProgress) {
			jlfuListenerPropagator.getPropagator().onFileUploadProgress(clientId, fileId, newProgress);
		}
	}

	/**
	 * Returns a calculated progress of a pending file upload.<br>
	 * @param fileId
	 * @return
	 */
	public FileProgressStatus getProgress(UUID fileId) {
		synchronized (fileToProgressInfo) {
			return fileToProgressInfo.get(fileId);
		}
	}

	
	
}
