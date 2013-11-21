package com.example.android.nsdchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import android.util.Log;

public class PackageHandler {
	
	private Socket mSocket;
	private String metaPath;
	private String transferDirectory;
	private String sender;
	
	byte[] buffer = new byte[8 * 1024];
	
	void setSocket(Socket socket) {
		mSocket = socket;
	}
	
	
	public void handlePackage(ProtocolPackage p) {
		switch(p.getType()) {
		case GET_META:
			// Send meta package on receiver.
			File metaFile = new File(metaPath);
			long meta_file_length = metaFile.length();
			String meta_file_name = metaFile.getName();
			
			ProtocolPackage send_meta = new ProtocolPackage(PackageType.SEND_META, 
					sender, meta_file_name, meta_file_length);
			try {
				OutputStream out = mSocket.getOutputStream();
				send_meta.sendPackage(out);
				
				FileInputStream fis = new FileInputStream(metaFile.toString());   
        		DataInputStream fin = new DataInputStream(fis);   
				writeToStream(new DataOutputStream(out), fin);
				fin.close();
				
			}catch(Exception e) {
				e.printStackTrace();
			}		
			break;
		case SEND_META:
			// Receive the meta file and then calculate the difference and then send file.
			String name = transferDirectory + "/" + p.getSender() + ".dat";
			File other_meta_file = new File(name);
			try {
				// Read and store other meta file.
				InputStream in = mSocket.getInputStream();
				FileOutputStream file_os = new FileOutputStream(other_meta_file);
		    	DataOutputStream file_data_os = new DataOutputStream(file_os);
		    	
        		DataInputStream socket_in = new DataInputStream(in);         
        		long byteCount = socket_in.readLong();
        		byte[] buf;
        		if(byteCount == p.getFile_length()){
					// read metadata
					buf = new byte[(int) byteCount];
					// while(dis.available() < byteCount){} //
					// wait until full file arrive
					int bytesRead = 0;
					while (bytesRead < byteCount) {
						bytesRead += in.read(buf,
								bytesRead, buf.length - bytesRead);
					}
        		}
        		else{
        			throw new Exception ("corrupted file");
        		}
        		//readFromStream(file_data_os, socket_in, p.getFile_length());
        		//file_data_os.close();
				
        		// Calculate the delta from local meta file and other meta file.
        		String this_meta_path = "xxx";
        		File this_meta_file = new File(this_meta_path);
        		List<String> list = getDelta(this_meta_file, other_meta_file);  		
        			
        		// Find those files and then send those files through socket.
        		//For()
        		
			}catch(Exception e) {
				e.printStackTrace();
			}	
			
		case SEND_FILE:
			// 
			break;
		}
	}
	
	private List<String> getDelta(File this_meta_file, File other_meta_file) {
		//TODO(Minh)
		// look at PifiMobile/edu.isi.usaid.pifi.services/FileUtil.java/getDelta
		return null;
	}
	private void writeToStream(DataOutputStream out, DataInputStream in) {
		int readed = 0;
		try {			
			while(readed != -1) {
				readed = in.read(buffer);
				out.write(buffer, 0, readed);
				out.flush();
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void readFromStream(DataOutputStream out, DataInputStream in, long length) {
		try {
			long bytesReaded = 0;              	
	    	while(bytesReaded < length) {
	    		int readed = in.read(buffer);
	    		bytesReaded += readed;
	    		out.write(buffer, 0, readed);
	    	}
		}catch(Exception e) {
			e.printStackTrace();
		}  
	}
	
	public String getMetaPath() {
		return metaPath;
	}


	public void setMetaPath(String metaPath) {
		this.metaPath = metaPath;
	}


	public String getTransferDirectory() {
		return transferDirectory;
	}


	public void setTransferDirectory(String transferDirectory) {
		this.transferDirectory = transferDirectory;
	}
		
}
