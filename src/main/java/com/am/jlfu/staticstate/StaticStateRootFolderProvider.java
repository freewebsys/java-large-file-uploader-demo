package com.am.jlfu.staticstate;


import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;



/**
 * Provides the root folder in which files will be uploaded.<br>
 * 
 * @author antoinem
 * 
 */
@Component
public class StaticStateRootFolderProvider {
	
	@Value("jlfu{jlfu.defaultUploadFolder:/JavaLargeFileUploader}")
	private String defaultUploadFolder;
	
	@Value("jlfu{jlfu.uploadFolderRelativePath:true}")
	private Boolean uploadFolderRelativePath;
	
	@Autowired(required = false)
	WebApplicationContext webApplicationContext;
	
	public File getRootFolder() {
		String realPath = defaultUploadFolder;
		if (uploadFolderRelativePath) {
			 realPath = webApplicationContext.getServletContext().getRealPath(defaultUploadFolder);
		} 
		File file = new File(realPath);
		// create if non existent
		if (!file.exists()) {
			file.mkdirs();
		}
		// if existent but a file, runtime exception
		else {
			if (file.isFile()) {
				throw new RuntimeException(file.getAbsolutePath() +
						" is a file. The default root folder provider uses this path to store the files. Consider using a specific root folder provider or delete this file.");
			}
		}
		return file;
	}
	
}
