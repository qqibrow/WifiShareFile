package com.example.android.nsdchat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ReceiverThread {
	Socket s;
	String name;
	PackageHandler packagehandler = null;
	public ReceiverThread(Socket s, String name) {
		this.s = s;
		this.name = name;
		packagehandler = new PackageHandler(s, name);
	}
	
	public void start() {
		new Thread(new HandlerServer()).start();
	}
	
	
	class HandlerServer implements Runnable {
		public void run() {
			try {
				InputStream in = s.getInputStream();
				ProtocolPackage p = null;
				while((p = ProtocolPackage.receivePackage(in)) != null) {
					if(p.getType().equals(PackageType.END_SESSION)){
						ProtocolPackage endsession = new ProtocolPackage(PackageType.END_SESSION, 
		    					name);
		    			endsession.sendPackage(s.getOutputStream());
			    		System.out.println(name + " send package whose type is " + endsession.getType());
						Thread.sleep(300);
						System.out.println("HandlerServer exit");
						throw new SocketException();
					}
					System.out.println(name + " received package whose type is " + p.getType());
					packagehandler.handlePackage(p);
					
//					switch(p.getType()) {
//					case REQUEST_META:
//						ProtocolPackage sendmeta = new ProtocolPackage(PackageType.SEND_META, 
//        	    				name);
//						sendmeta.sendPackage(out);
//						System.out.println(name + " send package whose type is " + sendmeta.getType());	
//						break;
//					case SEND_META:
//						ProtocolPackage sendfile = new ProtocolPackage(PackageType.SEND_FILE, 
//        	    				name);
//						sendfile.sendPackage(out);  
//						System.out.println(name + " send package whose type is " + sendfile.getType());	
//						break;
//					case SEND_FILE:
//						
//						break;
//					}
									
				}
			}catch(SocketException e){
				if(s.isConnected())
					try {
						s.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			}		
			catch(Exception e) {
				e.printStackTrace();
			}
			
		}
	}
}
