package com.am.jlfu.fileuploader.limiter;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.utils.ClientToFilesMap;
import com.google.common.collect.Maps;



@Component
public class UploadProcessingOperationManager {

	private static final Logger log = LoggerFactory.getLogger(UploadProcessingOperationManager.class);

	@Autowired
	ClientToFilesMap clientToFilesMap;
	
	// ////////////
	// operation//
	// ////////////

	/** Operation for clients and requests. */
	final ConcurrentMap<UUID, UploadProcessingOperation> clientsAndRequestsProcessingOperation = Maps.newConcurrentMap();

	/** Operation for master. */
	final UploadProcessingOperation masterProcessingOperation = new UploadProcessingOperation();

	
	public Map<UUID, UploadProcessingOperation> getClientsAndRequestsProcessingOperation() {
		return clientsAndRequestsProcessingOperation;
	}


	public void startOperation(UUID clientId, UUID fileId) {
		log.debug("starting operation for client "+clientId + " and file "+fileId);
		
		// create the request one
		// XXX are we sure that there is only one there?
		clientsAndRequestsProcessingOperation.put(fileId, new UploadProcessingOperation());

		// get or create the client one
		clientsAndRequestsProcessingOperation.putIfAbsent(clientId, new UploadProcessingOperation());

		// mapping
		Set<UUID> set = clientToFilesMap.get(clientId);
		if (set == null) {
			set = new HashSet<UUID>();
			clientToFilesMap.put(clientId, set);
		}
		synchronized (set) {
			set.add(fileId);
		}

	}


	public void stopOperation(UUID clientId, UUID fileId) {
		log.debug("stopping operation for client "+clientId + " and file "+fileId);
		
		// remove from map
		clientsAndRequestsProcessingOperation.remove(fileId);

		// remove mapping
		Set<UUID> set = clientToFilesMap.get(clientId);
		if (set != null) {
			synchronized (set) {
				set.remove(fileId);
				
				// if client is empty, remove client
				final boolean noreMoreUploadsForThisClient = set.isEmpty();
				if (noreMoreUploadsForThisClient) {
					clientToFilesMap.remove(clientId);
					clientsAndRequestsProcessingOperation.remove(clientId);
				}
			}
		}

	}


	public UploadProcessingOperation getClientProcessingOperation(UUID clientId) {
		return clientsAndRequestsProcessingOperation.get(clientId);
	}


	public UploadProcessingOperation getFileProcessingOperation(UUID fileId) {
		return clientsAndRequestsProcessingOperation.get(fileId);
	}


	public UploadProcessingOperation getMasterProcessingOperation() {
		return masterProcessingOperation;
	}
}
