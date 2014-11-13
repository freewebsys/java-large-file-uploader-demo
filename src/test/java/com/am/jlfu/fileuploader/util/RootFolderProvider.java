package com.am.jlfu.fileuploader.util;


import java.io.File;
import java.io.IOException;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.am.jlfu.staticstate.StaticStateRootFolderProvider;



@Component
@Primary
public class RootFolderProvider
		extends StaticStateRootFolderProvider {

	private File file;



	@Override
	public File getRootFolder() {
		if (file == null) {
			try {
				file = File.createTempFile("lala", "test");
				file.delete();
				file.mkdir();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return file;
	}
}
