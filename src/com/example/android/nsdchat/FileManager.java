package com.example.android.nsdchat;

import java.io.File;
import java.util.List;

import android.os.Environment;

public class FileManager {
	String external_root_path;
	String app_root_path;
	String meta_file_path;	
	private static final FileManager INSTANCE = new FileManager();
	
	public static FileManager getInstance() {
		return INSTANCE;
	}
	
	private FileManager() {
		this("Pifitest", "meta.txt");
	}

	public FileManager(String approot, String metafilename) {
		external_root_path = Environment.getExternalStorageDirectory().getPath();
		app_root_path = appendPath(external_root_path, approot);
		meta_file_path = appendPath(app_root_path, metafilename);
	}
	
	public File getMataFile() {
		File meta = new File(meta_file_path);
		if(meta.exists())
			return meta;
		return null;
	}
	
	File[] getDelta(byte[] otherMetaFile) {
		return getDeltaTest();
	}
	
	// it will return all files under root_dir/test_dir.
	File[] getDeltaTest() {
		File test_dir = new File(appendPath(app_root_path, "testfiles"));
		assert(test_dir.isDirectory() && test_dir.exists());
		return test_dir.listFiles();
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
		StringBuffer res = new StringBuffer();
		for(char c : device_name.toCharArray()) {
			if(c != ':')
				res.append(c);
		}
		return res.toString();
	}
}
