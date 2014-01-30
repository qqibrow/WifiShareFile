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
	}
	
	public ProtocolPackage(PackageType type, String sender) {
		super();
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
	
	public void sendPackage(OutputStream out) throws Exception {
			ObjectOutputStream obj_os = new ObjectOutputStream(out);
			obj_os.writeObject(this);
			obj_os.flush();
	}
	
	public static ProtocolPackage receivePackage(InputStream in) throws Exception{
		ObjectInputStream obj_in = new ObjectInputStream(in);
		ProtocolPackage other = (ProtocolPackage)obj_in.readObject();
		return other;	
	}
	
	public String getFile_name() {
		return file_name;
	}
	
	public String getFileNameWithoutExtension() {
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
