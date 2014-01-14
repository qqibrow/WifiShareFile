package com.example.android.nsdchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import android.util.Log;

public class PackageHandler {	
	private Socket mSocket;
	private String sender;
	private PifiFileManager filemanager;
	private Thread mThread = null;
	
	byte[] buffer = new byte[8 * 1024];
	
	PackageHandler(Socket socket, String self_name) {
		mSocket = socket;
		sender = self_name;
		mThread = new Thread(new PackageHandlerThread());
	}
	
	public void handle() {
		mThread.run();
	}

	class PackageHandlerThread implements Runnable {   	
    	public void run() {
    		try {
    			ProtocolPackage p = ProtocolPackage.receivePackage(mSocket.getInputStream());
    			handlePackage(p);  			
    		}catch(Exception e) {
    			e.printStackTrace();
    		}   		    			
    	}
    }

	// Warp the file as the send_meta package and output to OutputStream.
	// the format of the package will be || packageheader | file ||.
	private void sendPackagedFile(File file, OutputStream out) {
		long meta_file_length = file.length();
		String meta_file_name = file.getName();
		
		ProtocolPackage send_meta = new ProtocolPackage(PackageType.SEND_META, 
				sender, meta_file_name, meta_file_length);
		try {
			send_meta.sendPackage(out);		
			FileInputStream fis = new FileInputStream(file.toString());			
			writeToStream(new DataOutputStream(out), new DataInputStream(fis));
			fis.close();
			
		}catch(Exception e) {
			e.printStackTrace();
		}	
	}	
	
	public void handlePackage(ProtocolPackage p) {
		switch(p.getType()) {
		case REQUEST_META:
			try {
				sendPackagedFile(filemanager.getMataFile(), mSocket.getOutputStream());				
			}catch(Exception e) {
				e.printStackTrace();
			}		
			break;
		case SEND_META:
			// Receive the meta file and then calculate the difference and then send file.
			try {
				// Read and store other meta file.
        		DataInputStream socket_in = new DataInputStream(mSocket.getInputStream());         
        		long byteCount = socket_in.readLong();
        		byte[] buf;
        		if(byteCount == p.getFile_length()){
					// read metadata
					buf = new byte[(int) byteCount];
					int bytesRead = socket_in.read(buf, 0, buf.length);
					if(bytesRead != buf.length) {
						throw new Exception("The length readed is not equal to the actual file length.");
					}
					Log.v("RECEIVE_META_FILE", buf.toString());	
        		}        		
        		else{
        			throw new Exception ("Corrupted file.");
        		}
        		
    			List<File> files = filemanager.getDelta(buf);
    			
    			for(File file : files) {
    				sendPackagedFile(file, mSocket.getOutputStream());
    			}				
			}catch(Exception e) {
				e.printStackTrace();
			}	
			
		case SEND_FILE:
			// Just receive the file and store the file in (trasferDirectory + phoneId) directory.
			File local_dir = filemanager.getDeviceTempDirectory(p.getSender());
			assert(local_dir.exists());
			File new_file = new File(local_dir, p.getFile_name());
			try {
				DataInputStream in = new DataInputStream(mSocket.getInputStream());
				DataOutputStream out = new DataOutputStream(new FileOutputStream(new_file));
				writeToStream(out, in);
				out.close();
				
			}catch(Exception e) {
				e.printStackTrace();
			}		
			break;
		}
	}
	
//	private List<String> getDelta(byte[] bytes, File metaFile){
//		// TODO This code runs a risk of running into exception if the meta data
//		// file size is too big. Needs to fix this issue later on
//		List<Video> sendTo = new List<Video>();
//		FileInputStream fin = new FileInputStream(metaFile);
//		
//		//create copies of actual list
//		for(Video vid : Videos.parseFrom(fin).getVideoList()){
//			sendTo.add(vid);
//		}
//		fin.close();
//		
//		List<Video> recv = new ArrayList<Video>();
//		for(Video vid : Videos.parseFrom(bytes).getVideoList()){
//			recv.add(vid);
//		}
//		
//		fin = new FileInputStream(metaFile);
//		Iterator<Video> local = Videos.parseFrom(fin).getVideoList().iterator();
//		fin.close();
//
//		// for each local entry
//		while (local.hasNext()) {
//			Iterator<Video> remote = Videos.parseFrom(bytes).getVideoList().iterator();
//			Video v = local.next();
//			// for each remote entry
//			while (remote.hasNext()) {
//				Video rem = remote.next();
//				// if local entry matches remote entry
//				if (v.getFilename().equals(rem.getFilename())) {
//					sendTo.remove(v);
//					recv.remove(rem);
//				}
//			}
//		}
//		return sendTo;
//	}
	
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

	 public synchronized void setSocket(Socket socket) {
         Log.d("PackageHandler", "setSocket being called.");
         if (socket == null) {
             Log.d("PackageHandler", "Setting a null socket.");
         }
         if (mSocket != null) {
             if (mSocket.isConnected()) {
                 try {
                     mSocket.close();
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
         }
         mSocket = socket;
     }
}
