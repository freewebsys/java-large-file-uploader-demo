package com.am.jlfu.staticstate;


import java.io.File;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;



@Component
public class StaticStateDirectoryManager {

	@Autowired
	StaticStateRootFolderProvider staticStateRootFolderProvider;

	@Autowired
	StaticStateIdentifierManager staticStateIdentifierManager;



	/**
	 * Retrieves the file parent of the session.
	 * 
	 * @return
	 */
	public File getUUIDFileParent() {
		return getUUIDFileParent(staticStateIdentifierManager.getIdentifier());
	}


	/**
	 * Retrieves the file parent of the session context less.
	 * 
	 * @param uuid
	 * @return
	 */
	public File getUUIDFileParent(UUID uuid) {
		File uuidFileParent = new File(staticStateRootFolderProvider.getRootFolder(), uuid.toString());
		if (!uuidFileParent.exists()) {
			uuidFileParent.mkdirs();
		}
		return uuidFileParent;
	}


}
