package com.am.jlfu.staticstate.entities;


import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import com.am.jlfu.staticstate.StaticStateManager;
import com.google.common.collect.Maps;



/**
 * Abstract class that is persisted on the filesystem and contains information about the files being
 * uploaded.<br>
 * You can of course extend it if you want to persist other stuff on the filesystem. If you do so,
 * you will have to call {@link StaticStateManager#init(Class)} with the type of the class you
 * defined extending this one.
 * 
 * @author antoinem
 * 
 */
public class StaticStatePersistedOnFileSystemEntity
		implements Serializable {

	/** generated id */
	private static final long serialVersionUID = 6033009138577295466L;

	/** The states of the files being uploaded, the UUID being its identifier. */
	private Map<UUID, StaticFileState> fileStates = Maps.newHashMap();



	/**
	 * Default constructor.
	 */
	public StaticStatePersistedOnFileSystemEntity() {
		super();
	}


	public Map<UUID, StaticFileState> getFileStates() {
		return fileStates;
	}


	public void setFileStates(Map<UUID, StaticFileState> fileStates) {
		this.fileStates = fileStates;
	}


}
