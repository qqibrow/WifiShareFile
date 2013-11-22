package com.example.android.nsdchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;



public class ProtocolPackage {	
	private PackageType type;
	private String sender;
	private String file_name;
	private long file_length;
	
	public ProtocolPackage() {
		
	}
	
	public ProtocolPackage(PackageType type, String sender) {
		this.type = type;
		this.sender = sender;
	}
	
	public ProtocolPackage(PackageType type, String sender, String file_name, long file_length){
		this(type, sender);
		this.setFile_name(file_name);
		this.setFile_length(file_length);
	}
	
	public String getSender() {
		return sender;
	}
	
	public void sendPackage(OutputStream out) {
		try {
			ObjectOutputStream obj_os = new ObjectOutputStream(out);
			obj_os.writeObject(this);
			obj_os.flush();
			
			switch(type) {
			case GET_META:
				break;
			case SEND_META:
			case SEND_FILE:
				byte[] buffer = new byte[8 * 1024];
				File file = new File(file_name);
				FileInputStream fis = new FileInputStream(file.toString());   
        		DataInputStream bis = new DataInputStream(fis);
        		DataOutputStream socket_data_os = new DataOutputStream(out);  
        		int readed = 0;
        		while(readed != -1) {
        			readed = bis.read(buffer);
        			socket_data_os.write(buffer, 0, readed);
        			socket_data_os.flush();
        		}
        		bis.close();
				break;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	
	}
	
	public ProtocolPackage receivePackage(InputStream in) {
		try {
			ObjectInputStream obj_in = new ObjectInputStream(in);
			ProtocolPackage other = (ProtocolPackage)obj_in.readObject();
			return other;
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String getFile_name() {
		return removeExtension(file_name);
	}

	public void setFile_name(String file_name) {
		this.file_name = file_name;
	}

	public long getFile_length() {
		return file_length;
	}

	public void setFile_length(long file_length) {
		this.file_length = file_length;
	}

	public PackageType getType() {
		return type;
	}
	
	public void copyPackage(ProtocolPackage other) {
		setFile_name(other.file_name);
		setFile_length(other.file_length);
		this.type = other.type;
		this.sender = other.sender;
	}
	
	private String removeExtension(String s) {

	    String separator = System.getProperty("file.separator");
	    String filename;

	    // Remove the path upto the filename.
	    int lastSeparatorIndex = s.lastIndexOf(separator);
	    if (lastSeparatorIndex == -1) {
	        filename = s;
	    } else {
	        filename = s.substring(lastSeparatorIndex + 1);
	    }

	    // Remove the extension.
	    int extensionIndex = filename.lastIndexOf(".");
	    if (extensionIndex == -1)
	        return filename;

	    return filename.substring(0, extensionIndex);
	}
}
