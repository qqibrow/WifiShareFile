package com.example.android.nsdchat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

class ThreadHandler {
	String name;
	Socket s = null;
	Boolean send_request_finished = false;
	
	public ThreadHandler(Socket s, String name) {
		System.out.println("Thread handler built");
		this.name = name;
		this.s = s;
	}
	
	public void start() {
		Thread handler = new Thread(new HandlerServerThread());
		Thread request = new Thread(new SendRequestThread());
		handler.start();
		request.start();
		
	}
	
	class SendRequestThread implements Runnable {
		public void run() {
				try {
						OutputStream out = s.getOutputStream();											
						ProtocolPackage requst_meta_package = new ProtocolPackage(PackageType.REQUEST_META, 
        	    				name);
        	    		requst_meta_package.sendPackage(out);
        	    		System.out.println(name + " send package whose type is " + requst_meta_package.getType());
					}catch(SocketException e){
						synchronized(s) {
							if(s.isConnected())
								try {
									s.close();
								} catch (IOException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
						}			
					}	
					catch(Exception e) {
					e.printStackTrace();
				}
		}
	}
	
	class HandlerServerThread implements Runnable {
		public void run() {
			try {
				InputStream in = s.getInputStream();
				OutputStream out = s.getOutputStream();
				ProtocolPackage p = null;
				while((p = ProtocolPackage.receivePackage(in)) != null) {
					System.out.println(name + " received package whose type is " + p.getType());	
					switch(p.getType()) {
					case REQUEST_META:
						ProtocolPackage sendmeta = new ProtocolPackage(PackageType.SEND_META, 
        	    				name);
						sendmeta.sendPackage(out);
						System.out.println(name + " send package whose type is " + sendmeta.getType());	
						break;
					case SEND_META:
						ProtocolPackage sendfile = new ProtocolPackage(PackageType.SEND_FILE, 
        	    				name);
						sendfile.sendPackage(out);  
						System.out.println(name + " send package whose type is " + sendfile.getType());	
						break;
					case SEND_FILE:
						
						break;
					}
									
				}
			}catch(SocketException e){
				synchronized(s) {
					if(s.isConnected())
						try {
							s.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
				}			
			}		
			catch(Exception e) {
				e.printStackTrace();
			}
			
		}
	}
}