package com.example.android.nsdchat;

import java.io.File;
import java.util.List;

import android.os.Environment;

public class PifiFileManager {
	
	String external_root_path;
	String app_root_path;
	String meta_file_path;
	
	public PifiFileManager() {
		this("Pifitest", "meta.txt");
	}

	public PifiFileManager(String approot, String metafilename) {
		external_root_path = Environment.getExternalStorageDirectory().getPath();
		app_root_path = appendPath(external_root_path, approot);
		meta_file_path = appendPath(app_root_path, metafilename);
	}
	
	public File getMataFile() {
		return new File(meta_file_path);
	}
	
	List<File> getDelta(byte[] otherMetaFile) {
		return null;
	}

	// Get temp data directory pointed by device name.
	public File getDeviceTempDirectory(String device_name) {
		device_name = getRightFormatDeviceName(device_name);
		File device_directory = new File(appendPath(appendPath(app_root_path, "xdir"), device_name));
		if(!device_directory.exists())
			device_directory.mkdirs();
		return device_directory;
	}
	
	private String appendPath(String parent, String child) {
		return parent + "/" + child;
	}
	
	// the original format device name may not be a valid path name. e.g. a MAC address contains ':', which is invalid.
	private String getRightFormatDeviceName(String device_name) {
		device_name.replace("X", "");
		return device_name;
	}
}
