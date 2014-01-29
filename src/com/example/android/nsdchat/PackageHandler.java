package com.example.android.nsdchat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class PackageHandler {	
	private Socket mSocket;
	private String sender;
	private PifiFileManager filemanager;
	private Thread mThread = null;
	
	private NsdHelper nsdHelper = null;
	byte[] buffer = new byte[8 * 1024];
	
	 public static final String TAG = "PackageHandler";
	 
	PackageHandler(Socket socket, String self_name) {
		mSocket = socket;
		sender = self_name;
		filemanager = new PifiFileManager();
		mThread = new Thread(new PackageHandlerThread());
	}
	
	PackageHandler(Socket socket, String self_name, NsdHelper nsdhelper) {
		this(socket, self_name);
		this.nsdHelper = nsdhelper;
		
	}
	
	public void handle() {
		mThread.start();
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
	private void sendPackagedFile(File file, OutputStream out, PackageType type) {
		long meta_file_length = file.length();
		String meta_file_name = file.getName();
		
		ProtocolPackage sendpackage = new ProtocolPackage(type, 
				sender, meta_file_name, meta_file_length);
		try {
			//sendpackage.sendPackage(out);
			ObjectOutputStream obj_os = new ObjectOutputStream(out);
			obj_os.writeObject(sendpackage);
			FileInputStream fis = new FileInputStream(file);			
			writeToStream(new DataOutputStream(out), new DataInputStream(fis));
			fis.close();
			
		}catch(Exception e) {
			e.printStackTrace();
		}	
	}	
	
	public void handlePackage(ProtocolPackage p) throws Exception{
		if(p == null) {
			Log.d(TAG, "Pacckage is null");
			return;
		}		
		String sender = p.getSender();
		NsdServiceInfo server = nsdHelper.name2NsdInfo.get(sender);	
		if(server == null || server.getHost() == null) {
			Log.d("SENDER", "Sender is null or host is null.");
			return;
		}
		Socket outputSocket = new Socket(server.getHost(), server.getPort());
		
		switch(p.getType()) {
		case REQUEST_META:
			Log.d(TAG, "handle request meta message");
			try {
				sendPackagedFile(filemanager.getMataFile(), outputSocket.getOutputStream(), PackageType.SEND_META);				
			}catch(Exception e) {
				e.printStackTrace();
			}
			outputSocket.close();
			break;
		case SEND_META:
			Log.d(TAG, "handle send meta message");
			// Receive the meta file and then calculate the difference and then send file.
			try {
				// Read and store other meta file.
        		//DataInputStream socket_in = new DataInputStream(mSocket.getInputStream());         
        		//long byteCount = socket_in.readLong();
        		byte[] buf = new byte[(int)p.getFile_length()];
        		//long file_length = p.getFile_length();
        		
        		BufferedReader inputStream   = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
        		String line = inputStream.readLine();
        		while(line != null) {
        			Log.d("SEND_META", line);
        			line = inputStream.readLine();
        		}
        		
        		
//        		if(true || byteCount == p.getFile_length()){
//					// read metadata
//        			
//					buf = new byte[(int) file_length];
//					int bytesRead = socket_in.read(buf, 0, buf.length);
//					if(bytesRead != buf.length) {
//						throw new Exception("The length readed is not equal to the actual file length.");
//					}
//					Log.v("RECEIVE_META_FILE", buf.toString());	
//        		}        		
//        		else{
//        			throw new Exception ("Corrupted file.");
//        		}
//        		
    			File[] files = filemanager.getDelta(buf);
    			
    			for(File file : files) {
    				sendPackagedFile(file, outputSocket.getOutputStream(), PackageType.SEND_FILE);
    				Log.d("SEND_META", "send file " + file.getName() + " Successfully.");
    				outputSocket.close();
    				outputSocket = new Socket(server.getHost(), server.getPort());
    			}				
			}catch(Exception e) {
				e.printStackTrace();
			}
			outputSocket.close();
			break;
		case SEND_FILE:
			Log.d(TAG, "handle send file message");
			// Just receive the file and store the file in (trasferDirectory + phoneId) directory.
			File local_dir = filemanager.getDeviceTempDirectory(p.getSender());
			assert(local_dir.exists());
			File new_file = new File(local_dir, p.getFile_name());
			try {
				/*BufferedReader inputStream   = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
        		String line = inputStream.readLine();
        		while(line != null) {
        			Log.d("SEND_FILE", line);
        			line = inputStream.readLine();
        		}*/
				DataInputStream in = new DataInputStream(mSocket.getInputStream());
				DataOutputStream out = new DataOutputStream(new FileOutputStream(new_file));
				writeToStream(out, in, (int)p.getFile_length());
				Log.d("SEND_FILE", "Got file " + p.getFile_name() + " successfully.");
				ProtocolPackage x = ProtocolPackage.receivePackage(mSocket.getInputStream());
    			handlePackage(x);
				
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
			readed = in.read(buffer);
			while(readed != -1) {				
				out.write(buffer, 0, readed);
				//out.flush();
				readed = in.read(buffer);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void writeToStream(DataOutputStream out, DataInputStream in, int length) {
		byte[] buffer = new byte[1024*8];
		int bytesLeft = length; // Or whatever
		  try {
		    while (bytesLeft > 0) {
		      int read = in.read(buffer, 0, Math.min(bytesLeft, buffer.length));
		      if (read == -1) {
		        throw new EOFException("Unexpected end of data");
		      }
		      out.write(buffer, 0, read);
		      bytesLeft -= read;
		    }
		  } catch(Exception e) {
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
