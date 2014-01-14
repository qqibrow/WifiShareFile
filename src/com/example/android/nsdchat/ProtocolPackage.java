package com.example.android.nsdchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;


// Comparable<ProtocolPackage> interface is not necessary at this time.
public class ProtocolPackage implements java.io.Serializable{	
	private PackageType type;
	private String sender;
	private String file_name;
	private long file_length;
	
	private static final long serialVersionUID = 7526472295622776147L;
	
	public ProtocolPackage() {
		sender = "";
		file_name = "";
	}
	
	public ProtocolPackage(PackageType type, String sender) {
		super();
		this.type = type;
		this.sender = sender;
		file_name = "";
		//file_name = "EMPTYFILE";
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
			case REQUEST_META:
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
	
	public  static ProtocolPackage receivePackage(InputStream in) {
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
	
	@Override
	public boolean equals(Object other){
		if (other == null) return false;
	    if (other == this) return true;
	    if (!(other instanceof ProtocolPackage))return false;
	    ProtocolPackage otherpackage = (ProtocolPackage)other;
		return type == otherpackage.type && sender.equals(otherpackage.sender) && 
				file_name.equals(otherpackage.file_name) && file_length == otherpackage.file_length;
	}
}
