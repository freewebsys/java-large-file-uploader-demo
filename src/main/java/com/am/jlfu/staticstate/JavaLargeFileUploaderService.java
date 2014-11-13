package com.am.jlfu.staticstate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.am.jlfu.fileuploader.logic.UploadServletAsyncProcessor;
import com.am.jlfu.fileuploader.utils.ProgressManager;
import com.am.jlfu.notifier.JLFUListenerPropagator;
import com.am.jlfu.staticstate.entities.FileProgressStatus;
import com.am.jlfu.staticstate.entities.StaticStatePersistedOnFileSystemEntity;
import com.thoughtworks.xstream.XStream;

/**
 * Provides methods related to the management of information of the files for services outside the scope of a request.<br>
 * 
 * @author antoinem
 * @see StaticStateManager
 */
@Component
@ManagedResource(objectName = "JavaLargeFileUploader:name=operationManager")
public class JavaLargeFileUploaderService<T extends StaticStatePersistedOnFileSystemEntity> {

	@Autowired
	FileDeleter fileDeleter;

	@Autowired
	StaticStateManager<T> staticStateManager;

	@Autowired
	ProgressManager progressManager;
	
	@Autowired
	StaticStateDirectoryManager staticStateDirectoryManager;

	@Autowired
	UploadServletAsyncProcessor uploadServletAsyncProcessor;
	
	@Autowired
	JLFUListenerPropagator jlfuListenerPropagator;
	
	private static final Logger log = LoggerFactory.getLogger(JavaLargeFileUploaderService.class);

	
	/**
	 * Retrieves the progress of the specified file for the specified client.<br>
	 * 
	 * 
	 * @param clientId
	 * @param fileId
	 * @return
	 * @throws FileNotFoundException
	 */
	public FileProgressStatus getProgress(UUID clientId, UUID fileId)
			throws FileNotFoundException {
		return progressManager.getProgress(fileId);
	}



	/**
	 * Updates an entity inside the cache and onto the filesystem.
	 * 
	 * @param uuid the uuid of the client, identifying the file
	 * @param entity the entity to write into that file
	 */
	public void updateEntity(UUID uuid, T entity) {
		log.debug("writing state for " + uuid);
		staticStateManager.cache.put(uuid, entity);
		writeEntity(new File(staticStateDirectoryManager.getUUIDFileParent(uuid), StaticStateManager.FILENAME), entity);
	}

	/**
	 * Persists modifications onto filesystem only.
	 * 
	 * @param uuid the uuid of the client, identifying the file
	 * @param entity the entity to write into that file
	 */
	public void writeEntity(UUID uuid, T entity) {
		writeEntity(new File(staticStateDirectoryManager.getUUIDFileParent(uuid), StaticStateManager.FILENAME), entity);
	}

	/**
	 * Persists modifications onto filesystem only.
	 * 
	 * @param staticStateFile the file in which to write the entity
	 * @param entity the entity to write into that file
	 */
	public void writeEntity(File staticStateFile, T entity) {
		write(entity, staticStateFile);
	}
	
	private void write(T modelFromContext, File modelFile) {
		XStream xStream = new XStream();
		FileOutputStream fs = null;
		try {
			fs = new FileOutputStream(modelFile);
			xStream.toXML(modelFromContext, fs);
		}
		catch (FileNotFoundException e) {
			log.error("cannot write to model file for " + modelFromContext.getClass().getSimpleName() + ": " + e.getMessage(), e);
		}
		finally {
			IOUtils.closeQuietly(fs);
		}
	}

	

	/**
	 * Retrieves the entity from cache using a client identifier.
	 * 
	 * @param clientIdentifier
	 * @return
	 */
	public T getEntityIfPresent(UUID clientIdentifier) {
		return staticStateManager.cache.getIfPresent(clientIdentifier);
	}

	/**
	 * Remove the pending uploaded file identifier by this id for this client.
	 * @param clientId
	 * @param fileId
	 */
	public void clearFile(final UUID clientId, final UUID fileId)
	{
		log.debug("Clearing pending uploaded file and all attributes linked to it.");
		
		final File uuidFileParent = staticStateDirectoryManager.getUUIDFileParent(clientId);
		
		// remove the uploaded file for this particular id
		fileDeleter.deleteFile(uuidFileParent.listFiles(new FilenameFilter() {
			
			public boolean accept(File dir, String name) {
				return name.startsWith(fileId.toString());
			}
		}));
		
		// remove the file information in entity
		T entity = getEntityIfPresent(clientId);
		entity.getFileStates().remove(fileId);
		
		// and save
		updateEntity(clientId, entity);
	}
	
	/**
	 * Clear everything including cache, session, files for this client.
	 * 
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public void clearClient(UUID clientId) {
		log.debug("Clearing everything including cache, session, files.");

		final File uuidFileParent = staticStateDirectoryManager.getUUIDFileParent(clientId);

		// schedule file for deletion
		fileDeleter.deleteFile(uuidFileParent);

		// remove entity from cache
		staticStateManager.cache.invalidate(clientId);

	}
	
	
	/**
	 * Enables the processing of file uploads. Clients will automatically resume their upload.
	 * @see #disableFileUploader()
	 */
	@ManagedOperation
	public void enableFileUploader() {
		uploadServletAsyncProcessor.setEnabled(true);
		jlfuListenerPropagator.getPropagator().onFileUploaderEnabled();
	}
	
	/**
	 * Disables the processing of file uploads. Clients currently uploading Files will wait and automatically resume the uploads when {@link #enableFileUploader()} is called.
	 * @see #enableFileUploader()
	 */
	@ManagedOperation
	public void disableFileUploader() {
		uploadServletAsyncProcessor.setEnabled(false);
		jlfuListenerPropagator.getPropagator().onFileUploaderDisabled();
	}
	
}

