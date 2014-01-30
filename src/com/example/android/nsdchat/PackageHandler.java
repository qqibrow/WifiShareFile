package com.example.android.nsdchat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import android.util.Log;

public class PackageHandler {	
	private static final int CACHE_SIZE = 8 * 1024;
	public static final String TAG = "PackageHandler";
	
	private Socket mSocket;
	private String sender;
	private Thread mThread = null;	
	byte[] buffer = new byte[CACHE_SIZE];

	PackageHandler(Socket socket, String self_name) {
		mSocket = socket;
		sender = self_name;
	}
	
	PackageHandler(Socket socket, String self_name, NsdHelper nsdhelper) {
		this(socket, self_name);
		
	}
	
	public void handle() {
		mThread.start();
	}

	// Warp the file as the send_meta package and output to OutputStream.
	// the format of the package will be || packageheader | file ||.
	private void sendPackagedFile(File file, OutputStream out, PackageType type) {
		long meta_file_length = file.length();
		String file_name = file.getName();
		
		ProtocolPackage sendpackage = new ProtocolPackage(type, 
				sender, file_name, meta_file_length);
		try {
			ObjectOutputStream obj_os = new ObjectOutputStream(out);
			obj_os.writeObject(sendpackage);
			FileInputStream fis = new FileInputStream(file);			
			writeToStream(new DataOutputStream(out), new DataInputStream(fis));
			fis.close();
			System.out.println(sender + " send message" + type + " with file " + file_name);
		}catch(Exception e) {
			e.printStackTrace();
		}	
	}	
	
	public void handlePackage(ProtocolPackage p) throws Exception{
		assert(p != null);
		OutputStream out = mSocket.getOutputStream();
		InputStream in = mSocket.getInputStream();
		
		switch(p.getType()) {
		case REQUEST_META:
				File meta_file = FileManager.getInstance().getMataFile();
				sendPackagedFile(meta_file, out, PackageType.SEND_META);				
			break;
		case SEND_META:
			// Receive the meta file and then calculate the difference and then send file.
			// Read and store other meta file.
    		
			// For testing in two phones, just output its meta file.
			long length = p.getFile_length();
			System.out.println("file length is" + length);
			{
				File local_dir = FileManager.getInstance().getDeviceTempDirectory(p.getSender());
				assert(local_dir.exists());
				File new_file = new File(local_dir, p.getFile_name());
				DataInputStream fin = new DataInputStream(in);
				DataOutputStream fout = new DataOutputStream(new FileOutputStream(new_file));
				writeToStream(fout, fin, (int)p.getFile_length());
	
				BufferedReader inputStream  = new BufferedReader(new InputStreamReader(new FileInputStream(new_file)));
        		String line = inputStream.readLine();
        		while(line != null) {
        			System.out.println(line);
        			line = inputStream.readLine();
        		}
        		inputStream.close();
			}
			///////////////End of testing //////////////////////////////////
				
    		byte[] buf = new byte[(int)p.getFile_length()];
			File[] files = FileManager.getInstance().getDelta(buf);
			
			for(File file : files) {
				sendPackagedFile(file, out, PackageType.SEND_FILE);
			}
			
			// Send end of session package.
			new ProtocolPackage(PackageType.END_SESSION, sender).sendPackage(out);
			break;
		case SEND_FILE:
			Log.d(TAG, "handle send file message");
			// Just receive the file and store the file in (trasferDirectory + phoneId) directory.
			File local_dir = FileManager.getInstance().getDeviceTempDirectory(p.getSender());
			assert(local_dir.exists());
			File new_file = new File(local_dir, p.getFile_name());
			
			DataInputStream fin = new DataInputStream(in);
			DataOutputStream fout = new DataOutputStream(new FileOutputStream(new_file));
			writeToStream(fout, fin, (int)p.getFile_length());				
			break;
		default:
			throw new Exception("Error go to default. Every case should be handled");
		}
	}
	
	private void writeToStream(DataOutputStream out, DataInputStream in) {
		int readed = 0;
		try {			
			readed = in.read(buffer);
			while(readed != -1) {				
				out.write(buffer, 0, readed);
				out.flush();
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
}
